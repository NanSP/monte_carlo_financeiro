package br.uneb.montecarlo.simulation.sequential;

import java.util.Objects;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.DeterministicGaussian;
import br.uneb.montecarlo.simulation.PricingEngine;

/** Sequential baseline for a European call under geometric Brownian motion. */
public final class SequentialPricingEngine implements PricingEngine {
    private static final double Z_95 = 1.959963984540054;

    @Override
    public PriceEstimate price(OptionParameters p) {
        Objects.requireNonNull(p, "parameters");
        double drift = (p.annualRiskFreeRate()
                - 0.5 * p.annualVolatility() * p.annualVolatility()) * p.maturityYears();
        double diffusion = p.annualVolatility() * StrictMath.sqrt(p.maturityYears());
        double discount = StrictMath.exp(-p.annualRiskFreeRate() * p.maturityYears());

        // Welford's recurrence avoids subtracting two large, nearly equal moments.
        double mean = 0.0;
        double m2 = 0.0;
        for (long i = 0; i < p.trajectories(); i++) {
            double z = DeterministicGaussian.sample(p.seed(), i);
            double terminalPrice = p.initialPrice() * StrictMath.exp(drift + diffusion * z);
            double discountedPayoff = discount * Math.max(terminalPrice - p.strikePrice(), 0.0);
            if (!Double.isFinite(discountedPayoff)) {
                throw new IllegalArgumentException("parameters produced a non-finite payoff");
            }
            double delta = discountedPayoff - mean;
            mean += delta / (i + 1);
            m2 += delta * (discountedPayoff - mean);
        }

        double sampleVariance = Math.max(0.0, m2 / (p.trajectories() - 1));
        double standardError = StrictMath.sqrt(sampleVariance / p.trajectories());
        double margin = Z_95 * standardError;
        return new PriceEstimate(mean, Math.max(0.0, mean - margin),
                mean + margin, standardError, p.trajectories());
    }
}
