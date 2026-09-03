package com.kareem.lifeos.engine;

/**
 * Temporary in-memory bridge for M1 shadow-mode evaluation.
 * It never writes to LifeDb and never changes the legacy capture result.
 */
public final class ParallelUnderstandingProbe {
    private static volatile RawScreenSnapshot lastSnapshot;
    private static volatile ScreenState lastState=new ScreenState(ScreenState.Type.UNKNOWN,0.0);

    private ParallelUnderstandingProbe(){}

    public static void observe(RawScreenSnapshot snapshot){
        lastSnapshot=snapshot;
        lastState=StructuralScreenClassifier.classify(snapshot);
    }

    public static RawScreenSnapshot lastSnapshot(){return lastSnapshot;}
    public static ScreenState lastState(){return lastState;}
}
