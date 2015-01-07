package com.flat.localization.algorithm;

/**
 * @author Jacob Phillips
 * This class assumes a specific set of coordinates for reference nodes:
 * Anchor1=(0,0); Anchor2=(x,0); Anchor3=(x,y)
 * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
 */
public final class Trilateration extends PositionAlgorithm {
    @Override
    public String getName() {
        return "Trilat.";
    }

    @Override
    public float[] findCoords(float[][] positions, float[] ranges) {
        float[] pos = new float[3];
        pos[0] = calcX(ranges[0], ranges[1], positions[1][0]);
        pos[1] = calcY(ranges[0], ranges[2], positions[2][0], positions[2][1], pos[0]);
        pos[2] = calcZ(ranges[0], pos[0], pos[1]);
        return pos;
    }

    /** p1=[0,0,0], p2=[x,0,0]. p1 is not actually used in the formula. */
    public static float[] trilaterate(float[] p1, float r1, float[] p2, float r2, float[] p3, float r3) {
        float[] pos = new float[3];
        pos[0] = calcX(r1, r2, p2[0]);
        pos[1] = calcY(r1, r3, p3[0], p3[1], pos[0]);
        pos[2] = calcZ(r1, pos[0], pos[1]);
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
    public static float calcX(float r1, float r2, float d) {
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
    public static float calcY(float r1, float r3, float i, float j, float x) {
        return ((r1*r1 - r3*r3 + i*i + j*j) / (2*j)) - x*i/j;
    }

    /**
     * Calculate the (plus or minus) Z coordinate for a node's position using information
     * about anchor nodes.
     * @see <a href='http://en.wikipedia.org/wiki/Trilateration'>wikipedia.org/wiki/Trilateration</a>
     * @param r1 Anchor 1's range in meters
     * @param x The nodes X coordinate. See calcX(double,double,double).
     * @param y The nodes Y coordinate. See calcY(double,double,double).
     */
    public static float calcZ(float r1, float x, float y) {
        return (float)Math.sqrt(r1*r1 - x*x - y*y);
    }
}
