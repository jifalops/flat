package com.flat.localization.node;

/**
 * Ranging provides the primary input to the localization system. A range is created by
 * interpreting an external signal such as a wifi beacon.
 *
 * {@link com.flat.localization.signals.Signal}
 * {@link com.flat.localization.signals.interpreters.SignalInterpreter}
 */
public final class NodeRange {
    public float range = 0;
    public float rangeOverride = 0; // when given
    public String signal = "none";
    public String interpreter = "none";
    public long time = System.currentTimeMillis();
    @Override public String toString() { return String.format("%.2fm (overridden to %.2fm)", range, rangeOverride); }
}
