package org.battelle.clodhopper.tuple;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;

/**
 * This class demonstrates how to package a 2-dimensional array of data as
 * a TupleList, so that ClodHopper's clustering algorithm can process it.
 *
 * @author R. Scarberry
 * @since 1.0.1
 */
public class SparseMap2DTupleList extends AbstractTupleList {
    private final TIntDoubleMap[] data;

    /**
     * Constructor
     *
     * @param data a 2-D array, where data.length is the number of tuples, and the
     *             length of any of the elements data[i] is the tuple length.  This array must be
     *             non-null, of length greater than 0, and each element data[i] must be the same length.
     */
    public SparseMap2DTupleList(TIntDoubleMap[] data, int feature_length) {
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
    public void setTuple(int n, double[] values) {
        TIntDoubleMap tmp = new TIntDoubleHashMap();
        tmp.values(null);
        for (int i = 0; i < values.length; i++) {
            if (values[i] != 0) {
                tmp.put(i, values[i]);
            }
        }
    }

    /**
     * Fetch values into a buffer.  If you pass in a null buffer, or a buffer of unsufficient length,
     * a new buffer is instantiated and returned with the values.
     */
    @Override
    public double[] getTuple(int n, double[] reuseBuffer) {
        double[] buffer = (reuseBuffer != null && reuseBuffer.length >= tupleLength) ? reuseBuffer : new double[tupleLength];
        TIntDoubleMap tmp = data[n];
        for (int key : tmp.keys()) {
            buffer[key] = tmp.get(key);
        }
        return buffer;
    }

    /**
     * Get an individual tuple element.
     */
    @Override
    public double getTupleValue(int n, int col) {
        return data[n].get(col);
    }

}