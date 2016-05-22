package org.battelle.clodhopper.seeding;

import gnu.trove.map.TIntDoubleMap;
import org.battelle.clodhopper.distance.SparseDistanceMetric;
import org.battelle.clodhopper.tuple.Sparse2DAbstractTupleList;
import org.battelle.clodhopper.tuple.SparseTupleList;

import java.util.Arrays;
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
 * KMeansPlusPlusSeeder.java
 *
 *===================================================================*/

public class SparseKMeansPlusPlusSeeder extends SparseRandomSeeder {

    private SparseDistanceMetric distMetric;

    public SparseKMeansPlusPlusSeeder(long seed, Random random, SparseDistanceMetric distMetric) {
        super(seed, random);
        if (distMetric == null) {
            throw new NullPointerException();
        }
        this.distMetric = distMetric;
    }

    public SparseKMeansPlusPlusSeeder(SparseDistanceMetric distMetric) {
        this(System.nanoTime(), new Random(), distMetric);
    }

    @Override
    public SparseTupleList generateSeeds(SparseTupleList tuples, int seedCount) {

        if (seedCount <= 0) {
            throw new IllegalArgumentException();
        }

        final int tupleCount = tuples.getTupleCount();

        if (tupleCount == 0 && seedCount > 0) {
            throw new IllegalArgumentException("cannot generate seeds from an empty TupleList");
        }

        if (seedCount > tupleCount) {
            // Can't have more seeds that choices.
            seedCount = tupleCount;
        }

        final int tupleLength = tuples.getTupleLength();

        // Set the seed before doing anything using random number generation.
        random.setSeed(seed);

        // Generate the potential seeds.  This is just the tuple indexes shuffled.
        int[] potentialSeeds = getShuffledTupleIndexes(tupleCount, random);

        double[] minSqDists = new double[tupleCount];
        // Set to true to indicate when a potential seed is no longer available.
        boolean[] unavailable = new boolean[tupleCount];

        int[] seedList = new int[seedCount];
        int seedsFound = 0;

        int firstSeed = random.nextInt(tupleCount);

        // For accumulating the indexes of the tuples used as seeds.
        seedList[seedsFound++] = potentialSeeds[firstSeed];
        unavailable[firstSeed] = true;

        // Working buffers
        TIntDoubleMap buffer2 = null;

        // Note that firstSeed is an index into potentialSeeds.  It's not
        // a tuple index itself.
        TIntDoubleMap buffer1 = tuples.getTuple(potentialSeeds[firstSeed]);

        // Initialize minSqDists
        for (int i = 0; i < tupleCount; i++) {
            if (i != firstSeed) {
                buffer2 = tuples.getTuple(potentialSeeds[i]);
                double dist = distMetric.distance(buffer1, buffer2);
                minSqDists[i] = dist * dist;
            }
        }

        while (seedsFound < seedCount) {

            // Sum the elements of minSqDists for those potential seeds that remain available.
            double sqDistSum = 0;
            for (int i = 0; i < tupleCount; i++) {
                if (!unavailable[i]) {
                    sqDistSum += minSqDists[i];
                }
            }

            // Compute a threshold value.
            double threshold = random.nextDouble() * sqDistSum;

            double probSum = 0;
            int newSeedIndex = -1;
            int lastAvailable = -1;

            for (int i = 0; i < tupleCount; i++) {
                if (!unavailable[i]) {
                    lastAvailable = i;
                    probSum += minSqDists[i];
                    if (probSum >= threshold) {
                        newSeedIndex = i;
                        break;
                    }
                }
            }

            if (newSeedIndex == -1) {
                newSeedIndex = lastAvailable;
            }

            if (newSeedIndex >= 0) {

                seedList[seedsFound++] = newSeedIndex;
                unavailable[newSeedIndex] = true;

                if (seedsFound < seedCount) {

                    // Update minSqDists using distance between the new seed index and the available seeds.
                    //
                    buffer1 =tuples.getTuple(potentialSeeds[newSeedIndex]);
                    for (int i = 0; i < tupleCount; i++) {
                        if (!unavailable[i]) {
                            buffer2 = tuples.getTuple(potentialSeeds[i]);
                            double dist = distMetric.distance(buffer1, buffer2);
                            double distSq = dist * dist;
                            // Only update if the distance is smaller.
                            if (distSq < minSqDists[i]) {
                                minSqDists[i] = distSq;
                            }
                        }
                    }
                }

            } else { // newSeedIndex == -1

                // Must break from while loop, since no other seeds are available whether
                // or not seedsFound == seedCount.
                break;
            }
        }

        SparseTupleList seeds = new Sparse2DAbstractTupleList(tupleLength, seedsFound);

        Arrays.sort(seedList, 0, seedsFound);

        for (int i = 0; i < seedsFound; i++) {
            TIntDoubleMap tuple = tuples.getTuple(seedList[i]);
            seeds.setTuple(i, tuple);
        }

        return seeds;

    }


}
