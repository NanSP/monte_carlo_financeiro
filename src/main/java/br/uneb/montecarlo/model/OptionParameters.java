package br.uneb.montecarlo.model;

/** Parameters for a European call option under geometric Brownian motion. */
public record OptionParameters(
        double initialPrice,
        double strikePrice,
        double annualRiskFreeRate,
        double annualVolatility,
        double maturityYears,
        long trajectories,
        long seed) {

    public OptionParameters {
        if (!Double.isFinite(initialPrice) || initialPrice <= 0) {
            throw new IllegalArgumentException("initialPrice must be positive and finite");
        }
        if (!Double.isFinite(strikePrice) || strikePrice <= 0) {
            throw new IllegalArgumentException("strikePrice must be positive and finite");
        }
        if (!Double.isFinite(annualRiskFreeRate)) {
            throw new IllegalArgumentException("annualRiskFreeRate must be finite");
        }
        if (!Double.isFinite(annualVolatility) || annualVolatility < 0) {
            throw new IllegalArgumentException("annualVolatility must be nonnegative and finite");
        }
        if (!Double.isFinite(maturityYears) || maturityYears <= 0) {
            throw new IllegalArgumentException("maturityYears must be positive and finite");
        }
        if (trajectories < 2) {
            throw new IllegalArgumentException("at least two trajectories are needed for a confidence interval");
        }
    }
}
