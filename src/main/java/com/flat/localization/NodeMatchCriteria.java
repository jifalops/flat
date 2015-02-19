package com.flat.localization;

import com.flat.localization.util.Calc;

import java.util.regex.Pattern;

/**
 * A single criterion to check if a node should be considered in a given localization algorithm.
 */
public final class NodeMatchCriteria {
    public boolean matchAll;
    public Pattern idMatches;

    public float[] posMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
    public float[] posMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
    public float[] angleMin = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
    public float[] angleMax = {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
    public Pattern stateAlgMatches;
    public long stateAgeMin = Long.MAX_VALUE;
    public long stateAgeMax = Long.MIN_VALUE;
    public int statePendingCountMin = Integer.MAX_VALUE;
    public int statePendingCountMax = Integer.MIN_VALUE;

    public float rangeMin = Float.MAX_VALUE;
    public float rangeMax = Float.MIN_VALUE;
    public Pattern rangeSigMatches;
    public Pattern rangeAlgMatches;
    public long rangeAgeMin = Long.MAX_VALUE;
    public long rangeAgeMax = Long.MIN_VALUE;
    public int rangePendingCountMin = Integer.MAX_VALUE;
    public int rangePendingCountMax = Integer.MIN_VALUE;

    /**
     * True if any of the above match. For min/max values, a match is when the value is in [min, max].
     */
    public boolean matches(Node n) {
        int pr = n.getRangePendingSize();
        int ps = n.getStatePendingSize();
        return matchAll ||
                (pr >= rangePendingCountMin && pr <= rangePendingCountMax) ||
                (ps >= statePendingCountMin && ps <= statePendingCountMax) ||
                (n.getRange().range >= rangeMin && n.getRange().range <= rangeMax) ||
                (n.getRange().rangeOverride >= rangeMin && n.getRange().rangeOverride <= rangeMax) ||
                (idMatches != null && idMatches.matcher(n.getId()).matches()) ||
                (stateAlgMatches != null && stateAlgMatches.matcher(n.getState().algorithm).matches()) ||
                (rangeSigMatches != null && rangeSigMatches.matcher(n.getRange().signal).matches()) ||
                (rangeAlgMatches != null && rangeAlgMatches.matcher(n.getRange().interpreter).matches()) ||
                (Calc.isLessThanOrEqual(n.getState().pos, posMax) && Calc.isLessThanOrEqual(posMin, n.getState().pos)) ||
                (Calc.isLessThanOrEqual(n.getState().angle, angleMax) && Calc.isLessThanOrEqual(angleMin, n.getState().angle)) ||
                (System.currentTimeMillis() - n.getState().time >= stateAgeMin && System.currentTimeMillis() - n.getState().time <= stateAgeMax) ||
                (System.currentTimeMillis() - n.getRange().time >= rangeAgeMin && System.currentTimeMillis() - n.getRange().time <= rangeAgeMax);
    }
}
