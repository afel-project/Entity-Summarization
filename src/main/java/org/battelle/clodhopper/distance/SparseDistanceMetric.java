package org.battelle.clodhopper.distance;

import gnu.trove.map.TIntDoubleMap;

/**
 * Created by besnik on 1/11/15.
 */
public interface SparseDistanceMetric extends Cloneable {

    /**
     * Computes the distance between tuple data contained in two arrays of the
     * same length.
     *
     * @param tuple1 array containing data for the first tuple.
     * @param tuple2 array containing data for the second tuple.
     * @return the distance between the tuples.
     */
    double distance(TIntDoubleMap tuple1, TIntDoubleMap tuple2);

    /**
     * Computes the distance between tuple data contained in two arrays of the
     * same length.
     *
     * @param tuple1 array containing data for the first tuple.
     * @param center array containing data for the second tuple.
     * @return the distance between the tuples.
     */
    double distance(TIntDoubleMap tuple1, double[] center);

    /**
     * @return a deep copy of the instance.
     */
    SparseDistanceMetric clone();
}
