package br.uneb.montecarlo.simulation;

import br.uneb.montecarlo.model.PriceEstimate;

/** Mergeable mean and variance accumulator using Welford's method. */
public final class RunningMoments {
    private static final double Z_95 = 1.959963984540054;

    private long count;
    private double mean;
    private double m2;

    public void add(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("payoff must be finite");
        }
        count++;
        double delta = value - mean;
        mean += delta / count;
        m2 += delta * (value - mean);
    }

    public void merge(RunningMoments other) {
        if (other.count == 0) return;
        if (count == 0) {
            count = other.count;
            mean = other.mean;
            m2 = other.m2;
            return;
        }
        long total = Math.addExact(count, other.count);
        double delta = other.mean - mean;
        m2 += other.m2 + delta * delta * ((double) count * other.count / total);
        mean += delta * ((double) other.count / total);
        count = total;
    }

    public PriceEstimate estimate() {
        if (count < 2) throw new IllegalStateException("at least two samples are required");
        double sampleVariance = Math.max(0.0, m2 / (count - 1));
        double standardError = StrictMath.sqrt(sampleVariance / count);
        double margin = Z_95 * standardError;
        return new PriceEstimate(mean, Math.max(0.0, mean - margin),
                mean + margin, standardError, count);
    }

    public long count() {
        return count;
    }
}
