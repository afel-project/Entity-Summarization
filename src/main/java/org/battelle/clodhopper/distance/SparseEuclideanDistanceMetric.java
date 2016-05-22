package org.battelle.clodhopper.distance;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Created by besnik on 1/11/15.
 */
public class SparseEuclideanDistanceMetric implements SparseDistanceMetric {
    /**
     * {@inheritDoc}
     */
    @Override
    public double distance(final TIntDoubleMap a, final TIntDoubleMap  b) {
        return getEucledianDistance(a, b);
    }

    @Override
    public double distance(TIntDoubleMap  a, double[] center) {
        return getEucledianDistance(a, center);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SparseDistanceMetric clone() {
        try {
            return (SparseDistanceMetric) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Computes the Euclidean distance between two feature vectors.
     * http://en.wikipedia.org/wiki/Euclidean_distance
     *
     * @param a
     * @param b
     * @return
     */
    private double getEucledianDistance(TIntDoubleMap a, TIntDoubleMap b) {
        double rst = 0.0;

        for (int key : a.keys()) {
            double a_val = a.get(key);
            double b_val = b.get(key);
            rst += Math.pow(a_val - b_val, 2);
        }

        for (int key : b.keys()) {
            double a_val = a.get(key);
            double b_val = b.get(key);
            rst += Math.pow(a_val - b_val, 2);
        }

        return Math.sqrt(rst);
    }

    /**
     * Computes the Euclidean distance between two feature vectors.
     * http://en.wikipedia.org/wiki/Euclidean_distance
     *
     * @param a
     * @param b
     * @return
     */
    private double getEucledianDistance(TIntDoubleMap a, double[] b) {
        double rst = 0.0;

        for (int i = 0; i < b.length; i++) {
            double b_val = b[i];
            double a_val = a.get(i);
            rst += Math.pow(a_val - b_val, 2);
        }

        return Math.sqrt(rst);
    }
}
