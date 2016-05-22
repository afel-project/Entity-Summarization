package org.battelle.clodhopper.xmeans;

import org.battelle.clodhopper.distance.SparseDistanceMetric;
import org.battelle.clodhopper.kmeans.SparseKMeansSplittingParams;
import org.battelle.clodhopper.seeding.SparseClusterSeeder;

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
 * XMeansParams.java
 *
 *===================================================================*/

public class SparseXMeansParams extends SparseKMeansSplittingParams {

    private boolean useOverallBIC = true;

    public SparseXMeansParams() {
    }

    public boolean getUseOverallBIC() {
        return useOverallBIC;
    }

    public void setUseOverallBIC(boolean b) {
        useOverallBIC = b;
    }

    public static class Builder {

        private SparseXMeansParams params = new SparseXMeansParams();

        public Builder() {
        }

        public Builder minClusters(int minClusters) {
            params.setMinClusters(minClusters);
            return this;
        }

        public Builder maxClusters(int maxClusters) {
            params.setMaxClusters(maxClusters);
            return this;
        }

        public Builder minClusterToMeanThreshold(double minClusterToMeanThreshold) {
            params.setMinClusterToMeanThreshold(minClusterToMeanThreshold);
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

        public Builder workerThreadCount(int workerThreadCount) {
            params.setWorkerThreadCount(workerThreadCount);
            return this;
        }

        public Builder userOverallBIC(boolean b) {
            params.setUseOverallBIC(b);
            return this;
        }

        public SparseXMeansParams build() {
            return params;
        }
    }
}
