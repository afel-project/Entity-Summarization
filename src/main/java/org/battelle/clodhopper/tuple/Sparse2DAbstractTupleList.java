package org.battelle.clodhopper.tuple;

import gnu.trove.map.TIntDoubleMap;

/**
 * This class demonstrates how to package a 2-dimensional array of data as
 * a TupleList, so that ClodHopper's clustering algorithm can process it.
 *
 * @author R. Scarberry
 * @since 1.0.1
 */
public class Sparse2DAbstractTupleList extends SparseAbstractTupleList {
    //private final boolean[][] feature_flags;
    private final TIntDoubleMap[] data;

    public Sparse2DAbstractTupleList(int tuple_length, int tuple_count) {
        super(tuple_length, tuple_count);

        //initialize the data matrix
        data = new TIntDoubleMap[tuple_count];
    }

    /**
     * Constructor
     *
     * @param data a 2-D array, where data.length is the number of tuples, and the
     *             length of any of the elements data[i] is the tuple length.  This array must be
     *             non-null, of length greater than 0, and each element data[i] must be the same length.
     */
    public Sparse2DAbstractTupleList(TIntDoubleMap[] data, int feature_length) {
        // Call parent constructor with the tupleCount and the tupleLength.
        // This code assume data is non-null and data.length > 0.  It also assumes
        // all data[i].length == data[0].length for all i != 0.
        super(feature_length, data.length);
        this.data = data;
    }

    /**
     * Set the values for tuple number n.  The values are copied in from the supplied
     * buffer.
     */
    @Override
    public void setTuple(int n, TIntDoubleMap values) {
        data[n] = values;
    }

    /**
     * Get an individual tuple element.
     */
    @Override
    public double getTupleValue(int n, int col) {
        return data[n].containsKey(col) ? data[n].get(col) : 0.0;
    }

    @Override
    public int getTupleFeatureLength(int n) {
        return data[n].size();
    }

    @Override
    public TIntDoubleMap getTuple(int n) {
        return data[n];
    }
}