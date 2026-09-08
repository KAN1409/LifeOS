package com.kareem.lifeos.context;

/** Atomic reservation boundary used to prevent duplicate side effects. */
public interface ActionLedger {
    /** Returns true only for the first reservation of a non-empty key. */
    boolean reserve(String idempotencyKey);
}
