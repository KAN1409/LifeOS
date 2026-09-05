package com.kareem.lifeos.context;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class SafeActionEngineTest {
    @Test public void writeRequiresExplicitApproval() {
        AtomicInteger executions = new AtomicInteger();
        SafeActionEngine engine = engine(executions);
        ActionExecutionResult result = engine.execute(proposal(ActionProposal.Risk.REVERSIBLE_WRITE, "raw-1"), null, model());
        assertEquals(ActionExecutionResult.Status.REJECTED, result.status);
        assertEquals(0, executions.get());
    }

    @Test public void approvedGroundedWriteExecutesOnce() {
        AtomicInteger executions = new AtomicInteger();
        SafeActionEngine engine = engine(executions);
        ActionProposal p = proposal(ActionProposal.Risk.REVERSIBLE_WRITE, "raw-1");
        ActionApproval approval = new ActionApproval(p.proposalId, true, 2000L);
        assertEquals(ActionExecutionResult.Status.SUCCESS, engine.execute(p, approval, model()).status);
        assertEquals(ActionExecutionResult.Status.DUPLICATE, engine.execute(p, approval, model()).status);
        assertEquals(1, executions.get());
    }

    @Test public void inventedEvidenceIsRejectedBeforeExecutor() {
        AtomicInteger executions = new AtomicInteger();
        SafeActionEngine engine = engine(executions);
        ActionProposal p = proposal(ActionProposal.Risk.REVERSIBLE_WRITE, "made-up");
        ActionExecutionResult result = engine.execute(p, new ActionApproval(p.proposalId, true, 2000L), model());
        assertEquals(ActionExecutionResult.Status.REJECTED, result.status);
        assertEquals(0, executions.get());
    }

    @Test public void readOnlyCanRunWithoutApprovalButStillNeedsGroundingAndIdempotency() {
        AtomicInteger executions = new AtomicInteger();
        SafeActionEngine engine = engine(executions);
        ActionProposal p = proposal(ActionProposal.Risk.READ_ONLY, "raw-1");
        assertEquals(ActionExecutionResult.Status.SUCCESS, engine.execute(p, null, model()).status);
        assertEquals(ActionExecutionResult.Status.DUPLICATE, engine.execute(p, null, model()).status);
        assertEquals(1, executions.get());
    }

    private static SafeActionEngine engine(final AtomicInteger executions) {
        return new SafeActionEngine(new ActionExecutor() {
            @Override public ActionExecutionResult execute(ActionProposal proposal) {
                executions.incrementAndGet();
                return ActionExecutionResult.success("done");
            }
        }, new InMemoryActionLedger());
    }

    private static ActionProposal proposal(ActionProposal.Risk risk, String evidenceId) {
        return new ActionProposal("p1", "s1", "reply", "Ahmed", "Reply hello", risk,
                "idem-1", Collections.singletonList(evidenceId));
    }

    private static LifeModelSnapshot model() {
        ContextEvent event = new ContextEvent("event:raw-1", "MESSAGE", "", "chat|ahmed",
                "hello", 1000L, 1.0, Collections.singletonList("raw-1"));
        Episode episode = new Episode("ep1", "chat|ahmed", 1000L, 1000L,
                Collections.singletonList(event), Collections.<EntityRef>emptyList());
        Situation situation = new Situation("s1", 1000L, 1000L, "Ahmed",
                Collections.singletonList(episode), Collections.<EntityRef>emptyList(), 0.9);
        return new LifeModelSnapshot("test", 2000L, Collections.<LifeFact>emptyList(),
                new LinkedHashMap<String,LifeFact>(), Collections.singletonList(situation));
    }
}
