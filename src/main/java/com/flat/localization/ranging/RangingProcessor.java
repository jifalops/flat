package com.flat.localization.ranging;

/**
 * These process signals into one-dimensional ranges (no direction).
 * Internal signals however, can translate directly into a state change of the 'me' node.
 *
 * Created by jacob on 10/25/14.
 */
public interface RangingProcessor {
    String getName();
}
