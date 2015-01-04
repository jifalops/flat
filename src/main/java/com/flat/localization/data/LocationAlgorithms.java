package com.flat.localization.data;

import com.flat.localization.algorithm.Criteria;
import com.flat.localization.algorithm.LocationAlgorithmManager;
import com.flat.localization.algorithm.MinMax;
import com.flat.localization.algorithm.Trilateration;

/**
 * @author Jacob Phillips (01/2015, jphilli85 at gmail)
 */
public class LocationAlgorithms {
    public static void initialize(LocationAlgorithmManager manager) {
/*
         * ===================
         * Location Algorithms
         * ===================
         */

        Criteria.AlgorithmMatchCriteria criteria;
        Criteria.NodeMatchCriteria nmc;


        /*
         * MinMax
         */
        final MinMax minmax = new MinMax();
        criteria = new Criteria.AlgorithmMatchCriteria();
        nmc = new Criteria.NodeMatchCriteria();
        nmc.rangePendingCountMin = 1;
        nmc.rangePendingCountMax = Integer.MAX_VALUE;
        criteria.nodeRequirements.add(nmc);
        manager.addAlgorithm(minmax, criteria);



        /*
         * Trilateration
         */
        final Trilateration trilat = new Trilateration();
        criteria = new Criteria.AlgorithmMatchCriteria();

        // Anchor 1 = (0, 0)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {0, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {0, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 2 = (x, 0)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, 0, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, 0, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        // Anchor 3 = (x, y)
        nmc = new Criteria.NodeMatchCriteria();
        nmc.posMin = new float[] {Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE};
        nmc.posMax = new float[] {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
        criteria.nodeListRequirements.add(nmc);

        manager.addAlgorithm(trilat, criteria);
    }
}
