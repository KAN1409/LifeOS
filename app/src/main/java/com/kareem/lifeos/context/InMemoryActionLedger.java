package com.kareem.lifeos.context;

import java.util.HashSet;
import java.util.Set;

/** Simple synchronized ledger; persistent Android implementation will replace this for production execution. */
public final class InMemoryActionLedger implements ActionLedger {
    private final Set<String> keys = new HashSet<String>();

    @Override public synchronized boolean reserve(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) return false;
        return keys.add(idempotencyKey);
    }
}
