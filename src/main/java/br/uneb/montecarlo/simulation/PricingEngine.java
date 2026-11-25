package br.uneb.montecarlo.simulation;

import br.uneb.montecarlo.model.OptionParameters;
import br.uneb.montecarlo.model.PriceEstimate;

/** Common contract for sequential, threaded, MPJ and OpenCL implementations. */
public interface PricingEngine {
    PriceEstimate price(OptionParameters parameters);
}
