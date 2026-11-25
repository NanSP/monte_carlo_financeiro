package br.uneb.montecarlo;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.BlackScholes;
import br.uneb.montecarlo.simulation.DeterministicGaussian;
import br.uneb.montecarlo.simulation.sequential.SequentialPricingEngine;

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

        SequentialPricingEngine engine = new SequentialPricingEngine();
        PriceEstimate estimate = engine.price(p);
        check(estimate.equals(engine.price(p)), "fixed seed must give the same result");
        check(Math.abs(estimate.price() - analytic) < 5 * estimate.standardError(),
                "Monte Carlo estimate is inconsistent with Black-Scholes");
        check(estimate.lower95() <= estimate.price() && estimate.price() <= estimate.upper95(),
                "confidence interval must contain the estimate");

        OptionParameters noVolatility = new OptionParameters(100, 100, 0.05, 0, 1, 2, 7);
        check(Math.abs(engine.price(noVolatility).price()
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
