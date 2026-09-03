package com.kareem.lifeos.context;

/** Execution boundary. Concrete Android automation is implemented later behind this interface. */
public interface ActionExecutor {
    ActionExecutionResult execute(ActionProposal proposal);
}
