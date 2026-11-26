package br.uneb.montecarlo.coordinator;

import java.util.UUID;

import br.uneb.montecarlo.model.PriceEstimate;

/** Immutable view of a job at one instant. */
public record JobSnapshot(
        UUID id,
        JobStatus status,
        long completedTrajectories,
        long totalTrajectories,
        PriceEstimate partialEstimate,
        PriceEstimate result,
        String error) {

    public double progress() {
        return (double) completedTrajectories / totalTrajectories;
    }
}
