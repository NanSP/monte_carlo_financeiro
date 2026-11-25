package br.uneb.montecarlo.simulation.sequential;

import java.util.Objects;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;
import br.uneb.montecarlo.simulation.PricingEngine;
import br.uneb.montecarlo.simulation.RunningMoments;
import br.uneb.montecarlo.simulation.SimulationKernel;

/** Sequential reference with the same canonical blocks used by threaded engines. */
public final class SequentialPricingEngine implements PricingEngine {
    @Override
    public PriceEstimate price(OptionParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        SimulationKernel kernel = new SimulationKernel(parameters);
        RunningMoments total = new RunningMoments();
        for (long block = 0; block < SimulationKernel.blockCount(parameters.trajectories()); block++) {
            total.merge(kernel.block(SimulationKernel.blockStart(block),
                    SimulationKernel.blockEnd(block, parameters.trajectories())));
        }
        return total.estimate();
    }
}
