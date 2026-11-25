package br.uneb.montecarlo;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.BlackScholes;
import br.uneb.montecarlo.simulation.DeterministicGaussian;
import br.uneb.montecarlo.simulation.sequential.SequentialPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine;
import br.uneb.montecarlo.simulation.threads.ThreadedPricingEngine.Distribution;

/** Standalone checks that run with the JDK, without a testing dependency. */
public final class FoundationCheck {
    private FoundationCheck() {
    }

    public static void main(String[] args) {
        OptionParameters p = new OptionParameters(100, 100, 0.05, 0.20, 1, 250_000, 42);
        double analytic = BlackScholes.callPrice(p);
        check(Math.abs(analytic - 10.4505836) < 0.0001, "Black-Scholes benchmark");

        double first = DeterministicGaussian.sample(p.seed(), 123);
        check(Double.doubleToLongBits(first)
                == Double.doubleToLongBits(DeterministicGaussian.sample(p.seed(), 123)),
                "same trajectory must have the same draw");

        SequentialPricingEngine sequential = new SequentialPricingEngine();
        PriceEstimate estimate = sequential.price(p);
        check(estimate.equals(sequential.price(p)), "fixed seed must give the same result");
        check(Math.abs(estimate.price() - analytic) < 5 * estimate.standardError(),
                "Monte Carlo estimate is inconsistent with Black-Scholes");
        check(estimate.lower95() <= estimate.price() && estimate.price() <= estimate.upper95(),
                "confidence interval must contain the estimate");

        assertThreadedEquals(sequential, p, 1);
        assertThreadedEquals(sequential, p, 4);
        assertThreadedEquals(sequential, p, 16);
        for (long trajectories : new long[] { 2, 8_192, 8_193, 16_384, 16_385 }) {
            OptionParameters boundary = new OptionParameters(100, 105, 0.01, 0.30, 2,
                    trajectories, 123);
            assertThreadedEquals(sequential, boundary, 3);
        }

        OptionParameters noVolatility = new OptionParameters(100, 100, 0.05, 0, 1, 2, 7);
        check(Math.abs(sequential.price(noVolatility).price()
                - BlackScholes.callPrice(noVolatility)) < 1e-12,
                "zero-volatility case");
        try {
            new OptionParameters(0, 100, 0.05, 0.2, 1, 100, 1);
            throw new AssertionError("invalid initial price was accepted");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }
        System.out.println("Foundation checks passed. Price=" + estimate.price()
                + ", Black-Scholes=" + analytic);
    }

    private static void assertThreadedEquals(SequentialPricingEngine sequential,
            OptionParameters parameters, int workers) {
        PriceEstimate expected = sequential.price(parameters);
        for (Distribution distribution : Distribution.values()) {
            ThreadedPricingEngine threaded = new ThreadedPricingEngine(workers, distribution);
            for (int repetition = 0; repetition < 3; repetition++) {
                check(expected.equals(threaded.price(parameters)),
                        distribution + " with " + workers + " workers differs from sequential");
            }
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
