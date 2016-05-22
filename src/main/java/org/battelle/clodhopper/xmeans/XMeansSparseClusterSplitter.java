package org.battelle.clodhopper.xmeans;

import org.apache.log4j.Logger;
import org.battelle.clodhopper.AbstractSparseClusterSplitter;
import org.battelle.clodhopper.Cluster;
import org.battelle.clodhopper.ClusterStats;
import org.battelle.clodhopper.kmeans.SparseKMeansClusterer;
import org.battelle.clodhopper.kmeans.SparseKMeansParams;
import org.battelle.clodhopper.task.TaskOutcome;
import org.battelle.clodhopper.tuple.SparseFilteredTupleList;
import org.battelle.clodhopper.tuple.SparseTupleList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
 * XMeansClusterSplitter.java
 *
 *===================================================================*/

public class XMeansSparseClusterSplitter extends AbstractSparseClusterSplitter {

    private static Logger logger = Logger.getLogger(XMeansClusterSplitter.class);

    private static final int[] SPLITS_TO_TRY = new int[]{
            2, 3, 5, 7, 11
    };

    private SparseTupleList tuples;
    private List<Cluster> clusters;
    private SparseXMeansParams params;
    private double overallBIC;

    public XMeansSparseClusterSplitter(SparseTupleList tuples, List<Cluster> clusters, double overallBIC, SparseXMeansParams params) {
        if (tuples == null || clusters == null || params == null) {
            throw new NullPointerException();
        }
        this.tuples = tuples;
        this.clusters = clusters;
        this.params = params;
        this.overallBIC = overallBIC;
    }

    @Override
    public boolean prefersSplit(Cluster origCluster, List<Cluster> splitClusters) {
        // The calculation of which split is best is actually done in split() for this
        // kind of splitter.
        return splitClusters.size() > 0;
    }

    @Override
    public List<Cluster> performSplit(Cluster cluster) {

        boolean useOverallBIC = params.getUseOverallBIC();

        double bicThreshold = overallBIC;
        if (!useOverallBIC) {
            bicThreshold = ClusterStats.computeBIC(tuples, clusters);
        }

        List<Cluster> result = null;
        int sz = cluster.getMemberCount();
        int clusterCount = clusters.size();

        int lim = SPLITS_TO_TRY.length;
        if (clusterCount <= 3) {
            // Try fewer splits when the number of clusters is few.
            lim = Math.min(2, lim);
        }

        for (int i = 0; i < lim; i++) {

            int splitDiv = SPLITS_TO_TRY[i];

            if (sz >= splitDiv) {

                try {

                    List<Cluster> children = split(cluster, splitDiv);

                    int numChildren = children != null ? children.size() : 0;

                    if (numChildren > 1) {

                        double bic = 0;

                        if (useOverallBIC) {
                            bic = ClusterStats.computeBIC(tuples,
                                    prepareClusterList(clusters, children, cluster));
                        } else {
                            bic = ClusterStats.computeBIC(tuples, children);
                        }

                        if (!Double.isNaN(bic) && bic > bicThreshold) {
                            result = children;
                            break;
                        }
                    }

                    if (numChildren < splitDiv) {
                        break;
                    }

                } catch (Exception e) {

                    logger.error("problem splitting cluster", e);

                }

            }

        } // for

        if (result == null) {
            result = Arrays.asList(cluster);
        }

        return result;
    }

    private List<Cluster> split(Cluster cluster, int howMany) throws Exception {

        int[] memberIndices = cluster.getMembers().toArray();

        SparseFilteredTupleList fcs = new SparseFilteredTupleList(memberIndices, tuples);

        SparseKMeansParams kparams = new SparseKMeansParams.Builder()
                .clusterCount(howMany)
                .movesGoal(0)
                .workerThreadCount(1) // Perform the split themselves in parallel, but use single-threaded kmeans on each split.
                .clusterSeeder(params.getClusterSeeder())
                .build();

        SparseKMeansClusterer kmeans = new SparseKMeansClusterer(fcs, kparams);

        // Call directly in this case.
        kmeans.run();

        if (kmeans.getTaskOutcome() == TaskOutcome.SUCCESS) {

            List<Cluster> kmeansClusters = kmeans.get();
            final int sz = kmeansClusters.size();

            // The member indexes need to be mapped back to indexes in tuples.
            //
            List<Cluster> splitClusters = new ArrayList<>(sz);
            for (int i = 0; i < sz; i++) {
                Cluster c = kmeansClusters.get(i);
                int mc = c.getMemberCount();
                int[] translatedMembers = new int[mc];
                for (int j = 0; j < mc; j++) {
                    translatedMembers[j] = fcs.getFilteredIndex(c.getMember(j));
                }
                splitClusters.add(new Cluster(translatedMembers, c.getCenter()));
            }

            return splitClusters;

        } else {

            logger.error(String.format("kmeans outcome = %s\n", kmeans.getTaskOutcome()));

            if (kmeans.getTaskOutcome() == TaskOutcome.ERROR) {
                Throwable t = kmeans.getError();
                logger.error("kmeans error", t);
            }

        }

        return null;
    }

    private static List<Cluster> prepareClusterList(List<Cluster> clusterList,
                                                    List<Cluster> splitClusters, Cluster original) {
        int numClusters = clusterList.size();
        List<Cluster> clist = new ArrayList<Cluster>(numClusters + splitClusters.size() - 1);
        for (int i = 0; i < numClusters; i++) {
            Cluster c = clusterList.get(i);
            if (c != original) {
                clist.add(c);
            }
        }
        clist.addAll(splitClusters);
        return clist;
    }
}
