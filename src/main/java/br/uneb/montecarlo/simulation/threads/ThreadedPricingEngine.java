package br.uneb.montecarlo.simulation.threads;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.PricingEngine;
import br.uneb.montecarlo.simulation.RunningMoments;
import br.uneb.montecarlo.simulation.SimulationKernel;

/** Prices canonical blocks in parallel, either with fixed ranges or dynamic work sharing. */
public final class ThreadedPricingEngine implements PricingEngine {
    public enum Distribution { STATIC, DYNAMIC }

    private final int requestedWorkers;
    private final Distribution distribution;

    public ThreadedPricingEngine(int workers, Distribution distribution) {
        if (workers <= 0) throw new IllegalArgumentException("workers must be positive");
        this.requestedWorkers = workers;
        this.distribution = Objects.requireNonNull(distribution, "distribution");
    }

    @Override
    public PriceEstimate price(OptionParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        long blocksLong = SimulationKernel.blockCount(parameters.trajectories());
        if (blocksLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("too many blocks for the threaded implementation");
        }
        int blockCount = (int) blocksLong;
        int workers = Math.min(requestedWorkers, blockCount);
        RunningMoments[] results = new RunningMoments[blockCount];
        AtomicInteger nextBlock = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(workers);
        List<Future<?>> futures = new ArrayList<>(workers);
        try {
            for (int worker = 0; worker < workers; worker++) {
                int workerIndex = worker;
                futures.add(executor.submit(() -> {
                    SimulationKernel kernel = new SimulationKernel(parameters);
                    if (distribution == Distribution.STATIC) {
                        int each = blockCount / workers;
                        int remainder = blockCount % workers;
                        int first = workerIndex * each + Math.min(workerIndex, remainder);
                        int end = first + each + (workerIndex < remainder ? 1 : 0);
                        for (int block = first; block < end; block++) {
                            computeBlock(kernel, parameters.trajectories(), results, block);
                        }
                    } else {
                        while (true) {
                            int block = nextBlock.getAndUpdate(
                                    current -> current < blockCount ? current + 1 : current);
                            if (block >= blockCount) break;
                            computeBlock(kernel, parameters.trajectories(), results, block);
                        }
                    }
                }));
            }
            for (Future<?> future : futures) future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("pricing interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("worker failed", cause);
        } finally {
            for (Future<?> future : futures) {
                if (!future.isDone()) future.cancel(true);
            }
            executor.shutdownNow();
        }

        RunningMoments total = new RunningMoments();
        for (RunningMoments block : results) total.merge(block);
        return total.estimate();
    }

    private static void computeBlock(SimulationKernel kernel, long trajectories,
            RunningMoments[] results, int block) {
        results[block] = kernel.block(SimulationKernel.blockStart(block),
                SimulationKernel.blockEnd(block, trajectories));
    }
}
