package br.uneb.montecarlo.coordinator;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.PricingEngine;

/** Bounded coordinator for jobs submitted by multiple clients. */
public final class JobCoordinator implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final ConcurrentHashMap<UUID, Job> jobs = new ConcurrentHashMap<>();
    private final Object lifecycleLock = new Object();
    private boolean closed;

    public JobCoordinator(int maxConcurrent, int queueCapacity) {
        if (maxConcurrent <= 0 || queueCapacity <= 0) {
            throw new IllegalArgumentException("concurrency and queue capacity must be positive");
        }
        executor = new ThreadPoolExecutor(maxConcurrent, maxConcurrent, 0L,
                TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.AbortPolicy());
    }

    /** Submits a validated task or rejects it immediately when the queue is full. */
    public UUID submit(OptionParameters parameters, PricingEngine engine) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(engine, "engine");
        synchronized (lifecycleLock) {
            if (closed) throw new IllegalStateException("coordinator is closed");
            Job job = new Job(UUID.randomUUID(), parameters);
            job.future = new FutureTask<>(() -> {
                runJob(job, engine);
                return null;
            });
            if (jobs.putIfAbsent(job.id, job) != null) {
                throw new IllegalStateException("duplicate job identifier");
            }
            try {
                executor.execute(job.future);
            } catch (RejectedExecutionException e) {
                jobs.remove(job.id, job);
                throw e;
            }
            return job.id;
        }
    }

    public JobSnapshot snapshot(UUID id) {
        return snapshotOf(find(id));
    }

    private JobSnapshot snapshotOf(Job job) {
        synchronized (job) {
            return new JobSnapshot(job.id, job.status, job.completed,
                    job.parameters.trajectories(), job.partial, job.result, job.error);
        }
    }

    /** Waits for completion without polling; cancellation returns a CANCELLED snapshot. */
    public JobSnapshot await(UUID id, long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException {
        Objects.requireNonNull(unit, "unit");
        Job job = find(id);
        try {
            job.future.get(timeout, unit);
        } catch (CancellationException ignored) {
            // The state was published before FutureTask.cancel was called.
        } catch (ExecutionException e) {
            throw new IllegalStateException("job runner failed unexpectedly", e.getCause());
        }
        return snapshotOf(job);
    }

    /** Returns false when the job has already reached a terminal state. */
    public boolean cancel(UUID id) {
        synchronized (lifecycleLock) {
            return cancelLocked(find(id));
        }
    }

    /** Drops a completed job after its result has been collected by the client. */
    public boolean forget(UUID id) {
        Job job = find(id);
        synchronized (job) {
            return job.status.isTerminal() && jobs.remove(id, job);
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (closed) return;
            closed = true;
            for (Job job : jobs.values()) cancelLocked(job);
            executor.shutdownNow();
        }
    }

    private boolean cancelLocked(Job job) {
        synchronized (job) {
            if (job.status.isTerminal()) return false;
            job.status = JobStatus.CANCELLED;
        }
        job.future.cancel(true);
        executor.remove(job.future);
        return true;
    }

    private Job find(UUID id) {
        Objects.requireNonNull(id, "id");
        Job job = jobs.get(id);
        if (job == null) throw new NoSuchElementException("unknown job: " + id);
        return job;
    }

    private void runJob(Job job, PricingEngine engine) {
        synchronized (job) {
            if (job.status != JobStatus.QUEUED) return;
            job.status = JobStatus.RUNNING;
        }
        try {
            PriceEstimate result = Objects.requireNonNull(
                    engine.price(job.parameters, (completed, partial) -> publish(job, completed, partial)),
                    "engine result");
            if (result.trajectories() != job.parameters.trajectories()) {
                throw new IllegalStateException("result trajectory count differs from request");
            }
            synchronized (job) {
                if (job.status == JobStatus.RUNNING) {
                    job.completed = job.parameters.trajectories();
                    job.partial = result;
                    job.result = result;
                    job.status = JobStatus.SUCCEEDED;
                }
            }
        } catch (Throwable failure) {
            synchronized (job) {
                if (job.status == JobStatus.RUNNING) {
                    job.error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
                    job.status = JobStatus.FAILED;
                }
            }
        }
    }

    private void publish(Job job, long completed, PriceEstimate partial) {
        synchronized (job) {
            if (job.status != JobStatus.RUNNING) return;
            if (completed < job.completed || completed > job.parameters.trajectories()
                    || completed < 1 || partial == null || partial.trajectories() != completed) {
                throw new IllegalArgumentException("invalid progress update");
            }
            job.completed = completed;
            job.partial = partial;
        }
    }

    private static final class Job {
        final UUID id;
        final OptionParameters parameters;
        FutureTask<Void> future;
        JobStatus status = JobStatus.QUEUED;
        long completed;
        PriceEstimate partial;
        PriceEstimate result;
        String error;

        Job(UUID id, OptionParameters parameters) {
            this.id = id;
            this.parameters = parameters;
        }
    }
}
