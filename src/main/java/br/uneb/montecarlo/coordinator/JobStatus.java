package br.uneb.montecarlo.coordinator;

/** Lifecycle states visible to clients. */
public enum JobStatus {
    QUEUED, RUNNING, SUCCEEDED, FAILED, CANCELLED;

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == CANCELLED;
    }
}
