package com.kareem.lifeos.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DeepBrainResult {
    public final String requestId;
    public final String answer;
    public final List<PriorityDecision> priorities;

    public DeepBrainResult(String requestId, String answer, List<PriorityDecision> priorities) {
        this.requestId = requestId == null ? "" : requestId;
        this.answer = answer == null ? "" : answer;
        this.priorities = Collections.unmodifiableList(priorities == null ?
                new ArrayList<PriorityDecision>() : new ArrayList<PriorityDecision>(priorities));
    }
}
