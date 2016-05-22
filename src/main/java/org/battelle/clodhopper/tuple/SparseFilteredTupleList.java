package org.battelle.clodhopper.tuple;

/*=====================================================================
 * 
 *                       CLODHOPPER CLUSTERING API
 * 
 * -------------------------------------------------------------------- 
 * 
 * Copyright (C) 2013 Battelle Memorial Institute 
 * http://www.battelle.org
 * 
 * -------------------------------------------------------------------- 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 * 
 * -------------------------------------------------------------------- 
 * *
 * FilteredTupleList.java
 *
 *===================================================================*/

import gnu.trove.map.TIntDoubleMap;

public class SparseFilteredTupleList extends SparseAbstractTupleList {

    private int[] indexes;
    private SparseTupleList filteredTuples;

    public SparseFilteredTupleList(int[] indexes, SparseTupleList tuples) {
        super(tuples.getTupleLength(), indexes.length);
        final int wrappedTupleCount = tuples.getTupleCount();
        for (int i = 0; i < tupleCount; i++) {
            int n = indexes[i];
            if (n < 0 || n >= wrappedTupleCount) {
                throw new IndexOutOfBoundsException(
                        String.format("filtered tuple index not in [0 - %d]: %d", wrappedTupleCount, n));
            }
        }
        this.indexes = indexes;
        this.filteredTuples = tuples;
    }

    @Override
    public void setTuple(int n, TIntDoubleMap values) {
        filteredTuples.setTuple(indexes[n], values);
    }

    @Override
    public double getTupleValue(int n, int col) {
        return filteredTuples.getTupleValue(indexes[n], col);
    }

    @Override
    public int getTupleFeatureLength(int n) {
        return filteredTuples.getTuple(n).size();
    }

    @Override
    public TIntDoubleMap getTuple(int n) {
        return filteredTuples.getTuple(n);
    }

    public int getFilteredIndex(int n) {
        return indexes[n];
    }
}
