package com.kareem.lifeos.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Policy gate before any Android/external side effect.
 * - proposal must reference a real situation and only its evidence;
 * - every write/sensitive action requires explicit approval;
 * - idempotency is reserved before execution to prevent duplicate side effects after retries/crashes.
 */
public final class SafeActionEngine {
    private final ActionExecutor executor;
    private final ActionLedger ledger;

    public SafeActionEngine(ActionExecutor executor, ActionLedger ledger) {
        if (executor == null) throw new IllegalArgumentException("executor required");
        if (ledger == null) throw new IllegalArgumentException("ledger required");
        this.executor = executor;
        this.ledger = ledger;
    }

    public ActionExecutionResult execute(ActionProposal proposal, ActionApproval approval,
                                         LifeModelSnapshot lifeModel) {
        if (proposal == null || lifeModel == null) return ActionExecutionResult.rejected("missing proposal or life model");
        Map<String,Set<String>> evidence = evidenceBySituation(lifeModel.situations);
        Set<String> allowed = evidence.get(proposal.situationId);
        if (allowed == null) return ActionExecutionResult.rejected("unknown situation");
        if (!allowed.containsAll(proposal.evidenceIds)) return ActionExecutionResult.rejected("ungrounded evidence");
        if (proposal.actionType.trim().isEmpty()) return ActionExecutionResult.rejected("missing action type");
        if (proposal.idempotencyKey.trim().isEmpty()) return ActionExecutionResult.rejected("missing idempotency key");

        if (proposal.risk != ActionProposal.Risk.READ_ONLY) {
            if (approval == null || !approval.approved || !proposal.proposalId.equals(approval.proposalId)) {
                return ActionExecutionResult.rejected("explicit approval required");
            }
        }

        if (!ledger.reserve(proposal.idempotencyKey)) {
            return ActionExecutionResult.duplicate("action already reserved/executed");
        }
        ActionExecutionResult result = executor.execute(proposal);
        return result == null ? ActionExecutionResult.failed("executor returned no result") : result;
    }

    private static Map<String,Set<String>> evidenceBySituation(List<Situation> situations) {
        Map<String,Set<String>> out = new HashMap<String,Set<String>>();
        if (situations == null) return out;
        for (Situation s : situations) {
            if (s == null || s.situationId.isEmpty()) continue;
            Set<String> evidence = new HashSet<String>();
            for (Episode episode : s.episodes) {
                if (episode == null) continue;
                for (ContextEvent event : episode.events) if (event != null) evidence.addAll(event.evidenceIds);
            }
            out.put(s.situationId, evidence);
        }
        return out;
    }
}
