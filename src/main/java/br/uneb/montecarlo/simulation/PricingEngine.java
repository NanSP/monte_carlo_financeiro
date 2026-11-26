package br.uneb.montecarlo.simulation;

import java.util.Objects;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;

/** Common contract for sequential, threaded, MPJ and OpenCL implementations. */
public interface PricingEngine {
    PriceEstimate price(OptionParameters parameters);

    /** Engines without incremental reporting may publish only the final result. */
    default PriceEstimate price(OptionParameters parameters, ProgressListener listener) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(listener, "listener");
        PriceEstimate result = price(parameters);
        listener.onProgress(parameters.trajectories(), result);
        return result;
    }
}
