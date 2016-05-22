package org.battelle.clodhopper.seeding;

import gnu.trove.map.TIntDoubleMap;
import org.battelle.clodhopper.tuple.Sparse2DAbstractTupleList;
import org.battelle.clodhopper.tuple.SparseTupleList;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

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
 * RandomSeeder.java
 *
 *===================================================================*/

public class SparseRandomSeeder implements SparseRandomClusterSeeder {

    protected long seed;
    protected Random random;

    public SparseRandomSeeder(long seed, Random random) {
        this.seed = seed;
        if (random != null) {
            this.random = random;
        } else {
            this.random = new Random();
        }
    }

    public SparseRandomSeeder() {
        this(System.nanoTime(), new Random());
    }

    public long getRandomGeneratorSeed() {
        return seed;
    }

    public void setRandomGeneratorSeed(long seed) {
        this.seed = seed;
    }

    @Override
    public SparseTupleList generateSeeds(SparseTupleList tuples, int seedCount) {

        if (seedCount <= 0) {
            throw new IllegalArgumentException();
        }

        int tupleCount = tuples.getTupleCount();

        if (tupleCount == 0 && seedCount > 0) {
            throw new IllegalArgumentException("cannot generate seeds from an empty TupleList");
        }

        int tupleLength = tuples.getTupleLength();

        random.setSeed(seed);

        int[] indices = getShuffledTupleIndexes(tupleCount, random);

        int centersFound = 0;
        Map<SparseSeedCandidate, SparseSeedCandidate> seedMap = new LinkedHashMap<>(seedCount * 2);

        for (int i = 0; i < tupleCount && centersFound < seedCount; i++) {
            int ndx = indices[i];
            TIntDoubleMap center = tuples.getTuple(ndx);
            SparseSeedCandidate seed = new SparseSeedCandidate(center);
            if (!seedMap.containsKey(seed)) {
                seedMap.put(seed, seed);
                centersFound++;
            }
        }

        SparseTupleList seeds = new Sparse2DAbstractTupleList(tupleLength, centersFound);
        int n = 0;
        for (SparseSeedCandidate seed : seedMap.keySet()) {
            seeds.setTuple(n++, seed.getCenter());
        }

        return seeds;
    }

    protected static int[] getShuffledTupleIndexes(final int tupleCount, final Random random) {
        int[] shuffledIndexes = new int[tupleCount];
        // Place the indexes in an array.
        for (int i = 0; i < tupleCount; i++) {
            shuffledIndexes[i] = i;
        }
        // Now shuffle them.
        for (int i = tupleCount - 1; i > 0; i--) {
            int j = random.nextInt(i);
            if (i != j) {
                int tmp = shuffledIndexes[i];
                shuffledIndexes[i] = shuffledIndexes[j];
                shuffledIndexes[j] = tmp;
            }
        }
        return shuffledIndexes;
    }

    static class SparseSeedCandidate {

        private TIntDoubleMap center;

        SparseSeedCandidate(TIntDoubleMap center) {
            this.center = center;
        }

        TIntDoubleMap getCenter() {
            return center;
        }

        public int hashCode() {
            int hc = 0;
            int len = center.size();
            if (len > 0) {
                long l = Double.doubleToLongBits(center.get(center.keys()[0]));
                hc = (int) (l ^ (l >>> 32));
                for (int i = 1; i < center.size(); i++) {
                    int key = center.keys()[i];
                    l = Double.doubleToLongBits(center.get(key));
                    hc = 37 * hc + (int) (l ^ (l >>> 32));
                }
            }
            return hc;
        }

        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof SparseSeedCandidate) {
                TIntDoubleMap otherCenter = ((SparseSeedCandidate) o).center;
                int n = this.center.size();
                if (n == otherCenter.size()) {
                    for (int key : this.center.keys()) {
                        if (Double.doubleToLongBits(this.center.get(key)) != Double.doubleToLongBits(otherCenter.get(key))) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }
    }
}
