package com.kareem.lifeos.context;

import java.util.List;

/** Pure interpretation boundary: raw evidence in, zero or more derived events out. */
public interface ContextInterpreter {
    String version();
    List<ContextEvent> interpret(RawObservation observation);
}
