package br.uneb.montecarlo.simulation;

import java.util.Objects;
import java.util.concurrent.CancellationException;

import br.uneb.montecarlo.model.OptionParameters;

/** Deterministic payoff calculation for a contiguous range of trajectories. */
public final class SimulationKernel {
    public static final int BLOCK_SIZE = 8_192;

    private final OptionParameters parameters;
    private final DeterministicGaussian generator;
    private final double drift;
    private final double diffusion;
    private final double discount;

    public SimulationKernel(OptionParameters parameters) {
        this.parameters = Objects.requireNonNull(parameters, "parameters");
        generator = new DeterministicGaussian(parameters.seed());
        drift = (parameters.annualRiskFreeRate()
                - 0.5 * parameters.annualVolatility() * parameters.annualVolatility())
                * parameters.maturityYears();
        diffusion = parameters.annualVolatility() * StrictMath.sqrt(parameters.maturityYears());
        discount = StrictMath.exp(-parameters.annualRiskFreeRate() * parameters.maturityYears());
    }

    public RunningMoments block(long start, long end) {
        if (start < 0 || end <= start || end > parameters.trajectories()) {
            throw new IllegalArgumentException("invalid trajectory range");
        }
        RunningMoments moments = new RunningMoments();
        for (long index = start; index < end; index++) {
            if ((index & 255L) == 0 && Thread.currentThread().isInterrupted()) {
                throw new CancellationException("pricing interrupted");
            }
            double z = generator.sample(index);
            double terminalPrice = parameters.initialPrice() * StrictMath.exp(drift + diffusion * z);
            double payoff = discount * Math.max(terminalPrice - parameters.strikePrice(), 0.0);
            moments.add(payoff);
        }
        return moments;
    }

    public static long blockCount(long trajectories) {
        return (trajectories - 1) / BLOCK_SIZE + 1;
    }

    public static long blockStart(long blockIndex) {
        return blockIndex * BLOCK_SIZE;
    }

    public static long blockEnd(long blockIndex, long trajectories) {
        long start = blockStart(blockIndex);
        return start + Math.min(BLOCK_SIZE, trajectories - start);
    }
}
