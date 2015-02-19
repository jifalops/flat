package com.flat.localization.algorithm;

import com.flat.localization.NodeMatchCriteria;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public final class AlgorithmManagerStaticData {
    public static void initialize(AlgorithmManager manager) {
/*
         * ===================
         * Location Algorithms
         * ===================
         */

        AlgorithmMatchCriteria criteria;
        NodeMatchCriteria nmc;


        /*
         * MinMax
         */
        final MinMax minmax = new MinMax();
        criteria = new AlgorithmMatchCriteria();
        nmc = new NodeMatchCriteria();
        nmc.rangePendingCountMin = 1;
        nmc.rangePendingCountMax = Integer.MAX_VALUE;
        criteria.nodeRequirements.add(nmc);
        manager.addAlgorithm(minmax, criteria);



        /*
         * Trilateration
         */
        final Trilateration trilat = new Trilateration();
        criteria = new AlgorithmMatchCriteria();

        // Anchor 1 = (0, 0)
        nmc = new NodeMatchCriteria();
        nmc.posMin = new float[] {0, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {0, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 2 = (x, 0)
        nmc = new NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 3 = (x, y)
        nmc = new NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        manager.addAlgorithm(trilat, criteria);
    }
}
