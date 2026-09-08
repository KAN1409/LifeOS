package com.kareem.lifeos.context;

import java.util.List;

/** Pure replaceable semantic boundary: immutable raw evidence in, assertions out. */
public interface SemanticInterpreter {
    String version();
    List<SemanticAssertion> interpret(RawObservation observation);
}
