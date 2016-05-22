package org.battelle.clodhopper.kmeans;

import org.battelle.clodhopper.distance.SparseDistanceMetric;
import org.battelle.clodhopper.distance.SparseEuclideanDistanceMetric;
import org.battelle.clodhopper.seeding.SparseClusterSeeder;
import org.battelle.clodhopper.seeding.SparseKMeansPlusPlusSeeder;

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
 * KMeansParams.java
 *
 *===================================================================*/

public class SparseKMeansParams {

    private int clusterCount;
    private boolean replaceEmptyClusters = true;
    private int maxIterations = Integer.MAX_VALUE;
    private int movesGoal;
    private int workerThreadCount;
    private SparseDistanceMetric distanceMetric;
    private SparseClusterSeeder seeder;

    public SparseKMeansParams() {
        workerThreadCount = Runtime.getRuntime().availableProcessors();
        distanceMetric = new SparseEuclideanDistanceMetric();
        seeder = new SparseKMeansPlusPlusSeeder(distanceMetric);
    }

    public int getClusterCount() {
        return clusterCount;
    }

    public void setClusterCount(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("cluster count must be greater than 0");
        }
        this.clusterCount = n;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("max iterations must be greater than 0");
        }
        this.maxIterations = n;
    }

    public int getMovesGoal() {
        return movesGoal;
    }

    public void setMovesGoal(int n) {
        if (n < 0) throw new IllegalArgumentException("moves goal cannot be negative");
        this.movesGoal = n;
    }

    public int getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("worker thread count must be greater than 0");
        }
        this.workerThreadCount = n;
    }

    public boolean getReplaceEmptyClusters() {
        return replaceEmptyClusters;
    }

    public void setReplaceEmptyClusters(boolean b) {
        replaceEmptyClusters = b;
    }

    public SparseDistanceMetric getDistanceMetric() {
        return distanceMetric;
    }

    public void setDistanceMetric(SparseDistanceMetric distanceMetric) {
        if (distanceMetric == null) {
            throw new NullPointerException();
        }
        this.distanceMetric = distanceMetric;
    }

    public SparseClusterSeeder getClusterSeeder() {
        return seeder;
    }

    public void setClusterSeeder(SparseClusterSeeder seeder) {
        if (seeder == null) {
            throw new NullPointerException();
        }
        this.seeder = seeder;
    }

    public static class Builder {

        private SparseKMeansParams params;

        public Builder() {
            params = new SparseKMeansParams();
        }

        public Builder clusterCount(int n) {
            params.setClusterCount(n);
            return this;
        }

        public Builder maxIterations(int n) {
            params.setMaxIterations(n);
            return this;
        }

        public Builder movesGoal(int n) {
            params.setMovesGoal(n);
            return this;
        }

        public Builder workerThreadCount(int n) {
            params.setWorkerThreadCount(n);
            return this;
        }

        public Builder replaceEmptyClusters(boolean b) {
            params.setReplaceEmptyClusters(b);
            return this;
        }

        public Builder distanceMetric(SparseDistanceMetric distanceMetric) {
            params.setDistanceMetric(distanceMetric);
            return this;
        }

        public Builder clusterSeeder(SparseClusterSeeder seeder) {
            params.setClusterSeeder(seeder);
            return this;
        }

        public SparseKMeansParams build() {
            return params;
        }
    }

}
