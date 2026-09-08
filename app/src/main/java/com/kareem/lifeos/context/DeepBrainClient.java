package com.kareem.lifeos.context;

/** Transport/model boundary for high-level prioritization and decision support. */
public interface DeepBrainClient {
    DeepBrainResult analyze(DeepBrainRequest request);
}
