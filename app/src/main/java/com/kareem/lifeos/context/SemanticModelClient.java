package com.kareem.lifeos.context;

import java.util.List;

/**
 * Transport/model boundary for semantic understanding.
 * Implementations may use a local model, a remote service, or a ChatGPT bridge.
 */
public interface SemanticModelClient {
    List<SemanticAssertion> analyze(RawObservation observation, List<EntityRef> resolvedEntities);
}
