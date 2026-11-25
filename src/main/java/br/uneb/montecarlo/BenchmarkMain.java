package br.uneb.montecarlo;

import java.util.Arrays;
import java.util.Locale;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.BlackScholes;
import br.uneb.montecarlo.simulation.PricingEngine;
import br.uneb.montecarlo.simulation.SimulationKernel;
import br.uneb.montecarlo.simulation.sequential.SequentialPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine.Distribution;

/** Runs comparable sequential, static and dynamic measurements and prints CSV. */
public final class BenchmarkMain {
    private BenchmarkMain() {
    }

    public static void main(String[] args) {
        if (args.length > 4) {
            throw new IllegalArgumentException(
                    "Usage: BenchmarkMain [trajectories] [workers] [repetitions] [seed]");
        }
        long trajectories = args.length > 0 ? Long.parseLong(args[0]) : 1_000_000L;
        int requestedWorkers = args.length > 1 ? Integer.parseInt(args[1])
                : Runtime.getRuntime().availableProcessors();
        int repetitions = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        long seed = args.length > 3 ? Long.parseLong(args[3]) : 42L;
        if (repetitions <= 0) throw new IllegalArgumentException("repetitions must be positive");

        OptionParameters parameters = parameters(trajectories, seed);
        PricingEngine[] engines = {
                new SequentialPricingEngine(),
                new ThreadedPricingEngine(requestedWorkers, Distribution.STATIC),
                new ThreadedPricingEngine(requestedWorkers, Distribution.DYNAMIC)
        };
        String[] names = { "SEQUENTIAL", "STATIC", "DYNAMIC" };
        long[][] durations = new long[engines.length][repetitions];
        int activeWorkers = (int) Math.min((long) requestedWorkers,
                SimulationKernel.blockCount(trajectories));

        OptionParameters warmup = parameters(Math.min(trajectories, 100_000L), seed);
        for (PricingEngine engine : engines) engine.price(warmup);

        PriceEstimate reference = null;
        for (int repetition = 0; repetition < repetitions; repetition++) {
            // Rotate the order so one engine does not always benefit from running last.
            for (int offset = 0; offset < engines.length; offset++) {
                int engineIndex = (repetition + offset) % engines.length;
                long start = System.nanoTime();
                PriceEstimate result = engines[engineIndex].price(parameters);
                durations[engineIndex][repetition] = System.nanoTime() - start;
                if (engineIndex == 0 && reference == null) reference = result;
                if (!reference.equals(result)) {
                    throw new IllegalStateException(names[engineIndex]
                            + " differs from sequential at repetition " + (repetition + 1));
                }
            }
        }

        double sequentialNs = median(durations[0]);
        double analytic = BlackScholes.callPrice(parameters);
        System.out.println("mode,trajectories,seed,requested_workers,active_workers,repetitions,"
                + "median_ms,speedup,efficiency,price,lower95,upper95,black_scholes");
        for (int i = 0; i < engines.length; i++) {
            int workers = i == 0 ? 1 : activeWorkers;
            double speedup = sequentialNs / median(durations[i]);
            System.out.printf(Locale.ROOT,
                    "%s,%d,%d,%d,%d,%d,%.3f,%.4f,%.4f,%.10f,%.10f,%.10f,%.10f%n",
                    names[i], trajectories, seed, requestedWorkers, workers, repetitions,
                    median(durations[i]) / 1_000_000.0, speedup, speedup / workers,
                    reference.price(), reference.lower95(), reference.upper95(), analytic);
        }
    }

    private static OptionParameters parameters(long trajectories, long seed) {
        return new OptionParameters(100, 100, 0.05, 0.20, 1, trajectories, seed);
    }

    private static double median(long[] values) {
        long[] sorted = values.clone();
        Arrays.sort(sorted);
        int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle]
                : sorted[middle - 1] / 2.0 + sorted[middle] / 2.0;
    }
}
