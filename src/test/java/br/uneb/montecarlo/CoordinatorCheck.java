package br.uneb.montecarlo;

import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import br.uneb.montecarlo.coordinator.JobCoordinator;
import br.uneb.montecarlo.coordinator.JobSnapshot;
import br.uneb.montecarlo.coordinator.JobStatus;
import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.PricingEngine;
import br.uneb.montecarlo.simulation.ProgressListener;
import br.uneb.montecarlo.simulation.sequential.SequentialPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine.Distribution;

/** Exercises bounded execution, progress, cancellation and failure without sleeps. */
public final class CoordinatorCheck {
    private CoordinatorCheck() {
    }

    public static void main(String[] args) throws Exception {
        OptionParameters small = new OptionParameters(100, 100, 0.05, 0.2, 1, 4, 42);
        BlockingEngine blocking = new BlockingEngine();
        try (JobCoordinator coordinator = new JobCoordinator(1, 1)) {
            UUID first = coordinator.submit(small, blocking);
            check(blocking.entered.await(2, TimeUnit.SECONDS), "first job did not start");
            JobSnapshot running = coordinator.snapshot(first);
            check(running.status() == JobStatus.RUNNING && running.completedTrajectories() == 2
                    && running.progress() == 0.5 && running.partialEstimate() != null,
                    "partial progress was not published");

            UUID queued = coordinator.submit(small, blocking);
            check(coordinator.snapshot(queued).status() == JobStatus.QUEUED, "second job not queued");
            try {
                coordinator.submit(small, blocking);
                throw new AssertionError("full queue accepted a third job");
            } catch (RejectedExecutionException expected) {
                // The running slot and the single queue slot are occupied.
            }
            check(coordinator.cancel(queued), "queued job was not cancelled");
            check(coordinator.await(queued, 2, TimeUnit.SECONDS).status() == JobStatus.CANCELLED,
                    "queued job did not become cancelled");
            UUID replacement = coordinator.submit(small, blocking);
            blocking.release.countDown();
            check(coordinator.await(first, 2, TimeUnit.SECONDS).status() == JobStatus.SUCCEEDED,
                    "first job did not finish");
            check(coordinator.await(replacement, 2, TimeUnit.SECONDS).status() == JobStatus.SUCCEEDED,
                    "replacement job did not finish");
            check(blocking.maximum.get() == 1, "concurrency limit was exceeded");
            check(coordinator.forget(queued), "completed job was not removed");
            try {
                coordinator.snapshot(queued);
                throw new AssertionError("forgotten job remained visible");
            } catch (NoSuchElementException expected) {
                // Expected.
            }
        }

        BlockingEngine underLoad = new BlockingEngine();
        ExecutorService callers = Executors.newFixedThreadPool(20);
        try (JobCoordinator coordinator = new JobCoordinator(4, 8)) {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<UUID>> submissions = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                submissions.add(callers.submit(() -> {
                    start.await();
                    return coordinator.submit(small, underLoad);
                }));
            }
            start.countDown();
            List<UUID> accepted = new ArrayList<>();
            int rejected = 0;
            for (Future<UUID> submission : submissions) {
                try {
                    accepted.add(submission.get(3, TimeUnit.SECONDS));
                } catch (ExecutionException e) {
                    if (!(e.getCause() instanceof RejectedExecutionException)) throw e;
                    rejected++;
                }
            }
            check(accepted.size() == 12 && rejected == 8,
                    "concurrent submissions did not honor the execution and queue limits");
            underLoad.release.countDown();
            for (UUID id : accepted) {
                check(coordinator.await(id, 3, TimeUnit.SECONDS).status() == JobStatus.SUCCEEDED,
                        "accepted job did not finish");
            }
            check(underLoad.maximum.get() <= 4, "concurrent workers exceeded limit");
        } finally {
            underLoad.release.countDown();
            callers.shutdownNow();
        }

        try (JobCoordinator coordinator = new JobCoordinator(1, 1)) {
            UUID failed = coordinator.submit(small, parameters -> {
                throw new IllegalStateException("injected failure");
            });
            JobSnapshot snapshot = coordinator.await(failed, 2, TimeUnit.SECONDS);
            check(snapshot.status() == JobStatus.FAILED
                    && snapshot.error().contains("injected failure") && snapshot.result() == null,
                    "failure was not recorded");
        }

        BlockingEngine interrupted = new BlockingEngine();
        try (JobCoordinator coordinator = new JobCoordinator(1, 1)) {
            UUID id = coordinator.submit(small, interrupted);
            check(interrupted.entered.await(2, TimeUnit.SECONDS), "cancellable job did not start");
            check(coordinator.cancel(id), "running job was not cancelled");
            check(coordinator.await(id, 2, TimeUnit.SECONDS).status() == JobStatus.CANCELLED,
                    "running job published a result after cancellation");
        } finally {
            interrupted.release.countDown();
        }

        OptionParameters real = new OptionParameters(100, 100, 0.05, 0.2, 1, 8_193, 7);
        PriceEstimate expected = new SequentialPricingEngine().price(real);
        try (JobCoordinator coordinator = new JobCoordinator(2, 2)) {
            for (PricingEngine engine : new PricingEngine[] {
                    new SequentialPricingEngine(),
                    new ThreadedPricingEngine(2, Distribution.STATIC),
                    new ThreadedPricingEngine(2, Distribution.DYNAMIC) }) {
                UUID id = coordinator.submit(real, engine);
                JobSnapshot snapshot = coordinator.await(id, 3, TimeUnit.SECONDS);
                check(snapshot.status() == JobStatus.SUCCEEDED
                        && snapshot.completedTrajectories() == real.trajectories()
                        && expected.equals(snapshot.result())
                        && expected.equals(snapshot.partialEstimate()),
                        "real engine produced an invalid final snapshot: " + snapshot);
            }
        }
        System.out.println("Coordinator checks passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class BlockingEngine implements PricingEngine {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final AtomicInteger active = new AtomicInteger();
        final AtomicInteger maximum = new AtomicInteger();

        @Override
        public PriceEstimate price(OptionParameters parameters) {
            throw new AssertionError("coordinator should use the progress overload");
        }

        @Override
        public PriceEstimate price(OptionParameters parameters, ProgressListener listener) {
            int now = active.incrementAndGet();
            maximum.accumulateAndGet(now, Math::max);
            try {
                listener.onProgress(2, new PriceEstimate(1, 1, 1, 0, 2));
                entered.countDown();
                try {
                    release.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CancellationException("interrupted");
                }
                return new PriceEstimate(1, 1, 1, 0, parameters.trajectories());
            } finally {
                active.decrementAndGet();
            }
        }
    }
}
