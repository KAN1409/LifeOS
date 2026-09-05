package com.kareem.lifeos.context;

/** Boundary between Android/source-specific capture and the generic context engine. */
public interface ObservationAdapter<T> {
    RawObservation adapt(T sourceEvent);
}
