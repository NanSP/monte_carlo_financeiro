package br.uneb.montecarlo.model;

/** Estimated call price and a normal-approximation 95% confidence interval. */
public record PriceEstimate(
        double price,
        double lower95,
        double upper95,
        double standardError,
        long trajectories) {
}
