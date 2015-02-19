package com.flat.localization.node;

import com.flat.localization.CoordinateSystem;
import com.flat.util.Format;

/**
 * A state is the position [x,y,z] and angle (see below) of a node at a specific time. It also
 * contains the reference frame through which this state was defined ({@link com.flat.localization.CoordinateSystem}).
 * <pre>
 * Angle:
 * 0:       angle with respect to left-right axis on a vertically held phone.
 * 1:       angle with respect to the up-down axis on a vertically held phone.
 * 2:       angle with respect to the forward-back axis on a vertically held phone.
 * </pre>
 * For orientation of angle coordinates, see
 * <a href='http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords'>http://developer.android.com/guide/topics/sensors/sensors_overview.html#sensors-coords</a>
 */
public final class NodeState {
    public CoordinateSystem referenceFrame;
    public float pos[] = {0,0,0};           // TODO put the position of the local node in the coordinate system?
    public float angle[] = {0,0,0};
    public String algorithm = "none";
    public long time = System.currentTimeMillis();

    @Override
    public String toString() {
        String x,y,z;
        x = Format.SCIENTIFIC_3SIG.format(pos[0]);
        y = Format.SCIENTIFIC_3SIG.format(pos[1]);
        z = Format.SCIENTIFIC_3SIG.format(pos[2]);
        return String.format("(%s, %s, %s), (%.0f, %.0f, %.0f)",
                x,y,z, angle[0], angle[1], angle[2]);
    }
}
