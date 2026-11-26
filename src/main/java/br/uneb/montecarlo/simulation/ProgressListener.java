package br.uneb.montecarlo.simulation;

import br.uneb.montecarlo.model.PriceEstimate;

/** Receives a partial estimate after one or more complete trajectory blocks. */
@FunctionalInterface
public interface ProgressListener {
    void onProgress(long completedTrajectories, PriceEstimate partialEstimate);
}
