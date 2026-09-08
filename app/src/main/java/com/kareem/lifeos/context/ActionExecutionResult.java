package com.kareem.lifeos.context;

public final class ActionExecutionResult {
    public enum Status { SUCCESS, FAILED, REJECTED, DUPLICATE }

    public final Status status;
    public final String message;

    public ActionExecutionResult(Status status, String message) {
        this.status = status == null ? Status.FAILED : status;
        this.message = message == null ? "" : message;
    }

    public static ActionExecutionResult success(String message) { return new ActionExecutionResult(Status.SUCCESS, message); }
    public static ActionExecutionResult failed(String message) { return new ActionExecutionResult(Status.FAILED, message); }
    public static ActionExecutionResult rejected(String message) { return new ActionExecutionResult(Status.REJECTED, message); }
    public static ActionExecutionResult duplicate(String message) { return new ActionExecutionResult(Status.DUPLICATE, message); }
}
