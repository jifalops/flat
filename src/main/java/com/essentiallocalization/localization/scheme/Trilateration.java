package com.essentiallocalization.localization.scheme;

import android.os.Bundle;

import com.essentiallocalization.localization.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Jacob Phillips
 * This class assumes a specific set of coordinates for reference nodes.
 * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
 */
public final class Trilateration implements Scheme {

    public static final String ARG_REFERENCE_NODES = "ref_nodes";
    public static final String ARG_RANGES = "ranges";

    @Override
    public Node.State calcNewState(Node node, Bundle args) {
        Node[] refnodes = (Node[]) args.get(ARG_REFERENCE_NODES);
        double[] ranges = args.getDoubleArray(ARG_RANGES);

        // represent refnodes' coordinates in a primative array
        double[][] positions = new double[refnodes.length][3];
        for (int i=0; i<refnodes.length; ++i) {
            for (int j=0; j<3; ++j) {
                positions[i][j] = refnodes[i].getState().pos[j];
            }
        }

        return new Node.State(doTrilateration(positions, ranges));
    }

    public double[] doTrilateration(double[][] positions, double[] distances) {
        double[] pos = new double[3];
        pos[0] = calcX(distances[0], distances[1], positions[1][0]);
        pos[1] = calcY(distances[0], distances[2], positions[2][0], positions[2][1], pos[0]);
        pos[2] = calcZ(distances[0], pos[0], pos[1]);
        return pos;
    }

    /**
     * Calculate the X coordinate for a node's position using information
     * about anchor nodes.
     * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
     * @param r1 Anchor 1's range in meters
     * @param r2 Anchor 2's range in meters
     * @param d Anchor 2's X coordinate (distance between anchor 1 and 2).
     */
    public double calcX(double r1, double r2, double d) {
        return (r1*r1 - r2*r2 - d*d) / (2*d);
    }

    /**
     * Calculate the Y coordinate for a node's position using information
     * about anchor nodes.
     * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
     * @param r1 Anchor 1's range in meters
     * @param r3 Anchor 3's range in meters
     * @param i Anchor 3's X coordinate
     * @param j Anchor 3's Y coordinate
     * @param x The nodes X coordinate. See calcX(double,double,double).
     */
    public double calcY(double r1, double r3, double i, double j, double x) {
        return ((r1*r1 - r3*r3 + i*i + j*j) / (2*j)) - x*i/j;
    }

    /**
     * Calculate the Z coordinate for a node's position using information
     * about anchor nodes.
     * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
     * @param r1 Anchor 1's range in meters
     * @param x The nodes X coordinate. See calcX(double,double,double).
     * @param y The nodes Y coordinate. See calcY(double,double,double).
     */
    public double calcZ(double r1, double x, double y) {
        return Math.sqrt(r1 * r1 - x * x - y * y);
    }
}
