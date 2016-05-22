package clustering;

import com.google.common.base.Splitter;
import edu.ucla.sspace.clustering.Assignments;
import edu.ucla.sspace.clustering.NormalizedSpectralClustering;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseSymmetricMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;
import edu.ucla.sspace.vector.DoubleVector;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.battelle.clodhopper.Cluster;
import org.battelle.clodhopper.distance.EuclideanDistanceMetric;
import org.battelle.clodhopper.tuple.SparseMap2DTupleList;
import org.battelle.clodhopper.tuple.TupleList;
import org.battelle.clodhopper.util.IntIterator;
import org.battelle.clodhopper.xmeans.XMeansClusterer;
import org.battelle.clodhopper.xmeans.XMeansParams;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;
import utils_package.FileUtils;
import utils_package.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by besnik on 11/5/14.
 */
public class EntityClustering {

    public static void main(String[] args) throws Exception {
        String operation = "", btc_data = "", out_dir = "",
                data_file = "", lsh_bins = "",
                stop_words_path = "", type_filter = "",
                type_index_path = "", loaded_type_index = "",
                feature_data = "";

        int k = 0, ngrams = 1;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-btc_data")) {
                btc_data = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-data_file")) {
                data_file = args[++i];
            } else if (args[i].equals("-stop_words")) {
                stop_words_path = args[++i];
            } else if (args[i].equals("-type_filters")) {
                type_filter = args[++i];
            } else if (args[i].equals("-k")) {
                k = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-ngrams")) {
                ngrams = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-lsh_bins")) {
                lsh_bins = args[++i];
            } else if (args[i].equals("-type_index")) {
                type_index_path = args[++i];
            } else if (args[i].equals("-loaded_type_index")) {
                loaded_type_index = args[++i];
            } else if (args[i].equals("-feature_data")) {
                feature_data = args[++i];
            }
        }

        EntityClustering ec = new EntityClustering();

        if (operation.equals("load_data")) {
            THashMap<String, Integer> type_index = new THashMap<>();
            ec.loadTypeIndex(type_index, type_index_path);
            System.out.printf("Loading entity-type index with %s items.\n", type_index.size());
            ec.loadEntitiesForGraphAndTypes(type_index, btc_data, out_dir);
        } else if (operation.equals("type_index")) {
            Set<String> type_filter_set = FileUtils.readIntoSet(type_filter, "\n", false);
            ec.generateTypeIndex(type_filter_set, btc_data, out_dir);
        } else if (operation.equals("entity_cluster_profiles")) {
            Set<String> stop_words_set = FileUtils.readIntoSet(stop_words_path, "\n", false);
            Set<String> filter_predicates = new HashSet<>();
            filter_predicates.add("http://purl.org/dc/terms/format");
            filter_predicates.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
            filter_predicates.add("http://purl.uniprot.org/core/version");

            Set<String> type_files = new HashSet<>();
            FileUtils.getFilesList(data_file, type_files);
            ec.buildClusterProfiles(type_files, type_index_path, loaded_type_index, data_file, out_dir, stop_words_set, filter_predicates);
        } else if (operation.equals("type_instances_features")) {
            Set<String> stop_words = FileUtils.readIntoSet(stop_words_path, "\n", false);
            Set<String> type_files = new HashSet<>();
            FileUtils.getFilesList(data_file, type_files);
            ec.generateTypeInstanceFeatures(type_files, out_dir, data_file, ngrams, stop_words);
        } else if (operation.equals("cluster_features")) {
            Set<String> dirs = new HashSet<>();
            if ((new File(data_file).isFile())) {
                dirs = FileUtils.readIntoSet(data_file, "\n", false);
                ec.computeClusterFeaturesQuery(dirs, lsh_bins, out_dir, feature_data);
            } else {
                FileUtils.getDirList(data_file, dirs);
                ec.computeClusterFeatures(dirs, lsh_bins, out_dir, data_file);
            }
        } else if (operation.equals("xmeans")) {
            ec.performCOXMeans(out_dir, data_file, k, feature_data);
        } else if (operation.equals("spectral_clustering")) {
            ec.performSpectralClustering(out_dir, data_file, k);
        }
    }

    /**
     * Generate the individual features of entities of specific entity types.
     *
     * @param type_files
     * @param out_dir
     * @param data_file_f
     * @param ngram_f
     * @param stop_words
     */
    private void generateTypeInstanceFeatures(Set<String> type_files, String out_dir, String data_file_f, int ngram_f, Set<String> stop_words) {
        type_files.parallelStream().forEach(type_file -> {
            int start = type_file.lastIndexOf("_") + 1;
            int end = type_file.indexOf(".", start);
            int type_id = Integer.valueOf(type_file.substring(start, end));

            Set<String> file_list = new HashSet<>();
            FileUtils.getFilesList(out_dir + "/" + type_id, file_list);

            if (file_list.size() == 11) {
                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(out_dir + "/" + type_id + "/entity_text.obj");
                if (entity_text != null) {
                    return;
                }
            }
            try {
                String type_data = data_file_f + "/type_" + type_id + ".csv.gz";
                processEntityFeatures(type_data, stop_words, out_dir, type_id, ngram_f);
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.printf("Finished processing type: %d.\n", type_id);
        });
    }

    /**
     * Constructs the cluster profiles for the individual entity types.
     *
     * @param type_files
     * @param type_index_path_f
     * @param loaded_type_index_f
     * @param data_file_f
     * @param out_dir_f
     * @param stop_words_set
     * @param filter_predicates
     */
    private void buildClusterProfiles(Set<String> type_files, String type_index_path_f, String loaded_type_index_f, String data_file_f, String out_dir_f, Set<String> stop_words_set, Set<String> filter_predicates) {
        type_files.parallelStream().forEach(type_file -> {
            try {
                int start = type_file.lastIndexOf("/") + 1;
                int end = type_file.indexOf("_", start);
                int type_id = Integer.valueOf(type_file.substring(start, end));

                //load the set of entities of a given entity type.
                THashSet<String> entity_intances = loadEntityTypeInstances(type_id, type_index_path_f);
                THashMap<Integer, THashSet<String>> entity_location = loadEntityTypeLocations(type_id, loaded_type_index_f, entity_intances);
                entity_intances = null;

                computeEntityClusterFeatures(data_file_f, entity_location, type_id, out_dir_f, stop_words_set, filter_predicates);
                System.out.printf("Finished computing features for entities of type: %d.\n", type_id);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.printf("Error processing type: %s with message: %s.\n", type_file, e.getMessage());
            }
        });
    }

    /**
     * Computes the entity cluster features.
     *
     * @param dirs
     * @param lsh_bins_f
     * @param out_dir_f
     * @param data_file_f
     */
    private void computeClusterFeatures(Set<String> dirs, String lsh_bins_f, String out_dir_f, String data_file_f) {
        dirs.parallelStream().forEach(type -> {
            try {
                if (!FileUtils.fileExists(lsh_bins_f + "/" + type + "_LSH_clusters.csv.gz", false)) {
                    return;
                }

                FileUtils.checkDir(out_dir_f + "/" + type);
                //compute the features based on the given LSH clustered bins.
                THashMap<Integer, TIntHashSet> lsh_bin_entities = loadLSHEntityBinsSimple(lsh_bins_f + "/" + type + "_LSH_clusters.csv.gz", 0);

                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(data_file_f + "/" + type + "/entity_labels.obj");
                THashMap<Integer, TIntHashSet> entity_objects = (THashMap<Integer, TIntHashSet>) FileUtils.readObject(data_file_f + "/" + type + "/entity_objects.obj");
                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(data_file_f + "/" + type + "/entity_text.obj");
                TIntIntHashMap entity_graph = (TIntIntHashMap) FileUtils.readObject(data_file_f + "/" + type + "/entity_graph.obj");

                //compute the entity features based on the entity objects and textual values for their labels and entity body
                computeClusteringTupleFeatures(lsh_bin_entities, entity_labels, entity_objects, entity_text, entity_graph, out_dir_f, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Computes the entity cluster features.
     *
     * @param lsh_bins_f
     * @param out_dir_f
     */
    private void computeClusterFeaturesQuery(Set<String> files, String lsh_bins_f, String out_dir_f, String feature_data) {
        files.parallelStream().forEach(file -> {
            try {
                int start_index = file.indexOf("cluster_results/") + "cluster_results/".length();
                String type = file.substring(start_index, file.indexOf("/", start_index));
                String lsh_bin = file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("_"));

                FileUtils.checkDir(out_dir_f + "/" + type);
                //compute the features based on the given LSH clustered bins.
                THashMap<Integer, TIntHashSet> lsh_bin_entities = loadLSHEntityBinsSimple(lsh_bins_f + "/" + type + "_LSH_clusters.csv.gz", 0);

                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(feature_data + "/" + type + "/entity_labels.obj");
                THashMap<Integer, TIntHashSet> entity_objects = (THashMap<Integer, TIntHashSet>) FileUtils.readObject(feature_data + "/" + type + "/entity_objects.obj");
                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(feature_data + "/" + type + "/entity_text.obj");
                TIntIntHashMap entity_graph = (TIntIntHashMap) FileUtils.readObject(feature_data + "/" + type + "/entity_graph.obj");

                //compute the entity features based on the entity objects and textual values for their labels and entity body
                computeClusteringTupleFeatures(lsh_bin_entities, entity_labels, entity_objects, entity_text, entity_graph, out_dir_f, type, lsh_bin);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Perform spectral clusteirng of entities.
     *
     * @param out_dir_f
     * @param data_file_f
     * @param finalK
     */
    private void performSpectralClustering(String out_dir_f, String data_file_f, int finalK) {

        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(data_file_f, files);
        files.parallelStream().forEach(file -> {
            if (file.contains("indices")) {
                return;
            }
            NormalizedSpectralClustering nsc = new NormalizedSpectralClustering();
            int start_type_index = file.indexOf("cluster_results/") + "cluster_results/".length();
            String type = file.substring(start_type_index, file.indexOf("/", start_type_index));
            FileUtils.checkDir(out_dir_f + "/" + type + "/");

            //load the corresponding LSH bins of entities
            int start_index = file.lastIndexOf("/") + 1;
            int end_index = file.indexOf("_", start_index);
            String lsh_bin = file.substring(start_index, end_index);

            String out_file = out_dir_f + "/" + type + "/" + lsh_bin + "_sf.csv";
            if (FileUtils.fileExists(out_file, false) || FileUtils.fileExists(out_file + ".gz", false)) {
                return;
            }
            final List<int[][][]> tpl_splits = optimizedLoadTuple2DArray(file);
            if (tpl_splits == null) {
                return;
            }
            List<Assignments> split_assignments = new ArrayList<>();
            IntStream.range(0, tpl_splits.size()).parallel().forEach(split -> {
                int[][][] tpl = tpl_splits.get(split);
                Matrix m = new SparseSymmetricMatrix(new YaleSparseMatrix(tpl.length, tpl.length));
                IntStream.range(0, tpl.length).forEach(i -> {
                    int[][] vector_a = tpl[i];
                    if (vector_a == null) {
                        return;
                    }
                    IntStream.range(i, tpl.length).forEach(j -> {
                        int[][] vector_b = tpl[j];
                        if (vector_b == null) {
                            return;
                        }
                        m.set(i, j, Utils.getEucledianDistance(vector_a, vector_b));
                    });
                });

                System.out.printf("Finished loading the symmetric matrix for file %s and split %d.\n", file, split);
                Assignments asg = nsc.cluster(m, finalK, null);
                split_assignments.add(asg);
            });

            while (split_assignments.size() != tpl_splits.size()) ;
            Map<Integer, Set<Integer>> clusters = generateClusterAssignmentMappings(split_assignments);

            TIntIntHashMap entity_indices = FileUtils.readCompressedIntoIntMap(data_file_f + "/" + type + "/" + lsh_bin + "_indices.csv.gz", "\t");
            StringBuffer sb = new StringBuffer();
            for (int cluster_id : clusters.keySet()) {
                Set<Integer> cluster_entities = clusters.get(cluster_id);
                sb.append(cluster_id).append("\t[");
                int index = 0;
                for (int entity_indice : cluster_entities) {
                    int entity_id = entity_indices.get(entity_indice);
                    if (index != 0) {
                        sb.append(", ");
                    }
                    sb.append(entity_id);
                }
                sb.append("]\n");
            }
            FileUtils.saveText(sb.toString(), out_file, false, true);
            System.out.printf("Finished computing spectral clusters for bin %s and type %s.\n", lsh_bin, type);
        });
    }


    /**
     * Map the cluster centroids and their corresponding entity instances.
     *
     * @param assignments
     * @return
     */
    private Map<Integer, Set<Integer>> generateClusterAssignmentMappings(List<Assignments> assignments) {
        //pick one split that has the highest number of entities to compute the cluster assignment mappings
        Map<Integer, Set<Integer>> all_clusters = new HashMap<>();

        Assignments assignment = assignments.get(0);
        DoubleVector[] centroids = assignment.getCentroids();

        //add the initial entity instances from the first assignment
        for (int cluster_id = 0; cluster_id < assignment.clusters().size(); cluster_id++) {
            all_clusters.put(cluster_id, assignment.clusters().get(cluster_id));
        }

        for (int j = 1; j < assignments.size(); j++) {
            Assignments assignment_cmp = assignments.get(j);
            DoubleVector[] centroids_cmp = assignment_cmp.getCentroids();

            //map the centroids
            for (int k = 0; k < centroids_cmp.length; k++) {
                DoubleVector centroid = centroids_cmp[k];
                int centroid_id = getClosestCentroidID(centroids, centroid);

                if (!all_clusters.containsKey(centroid_id)) {
                    all_clusters.put(centroid_id, new HashSet<>());
                }
                all_clusters.get(centroid_id).addAll(assignment_cmp.clusters().get(k));
            }
        }
        return all_clusters;
    }

    /**
     * Map the cluster centroids and their corresponding entity instances.
     *
     * @param assignments
     * @return
     */
    private Map<Integer, Set<Integer>> generateClusterAssignmentMappingsKMeans(List<List<Cluster>> assignments) {
        //pick one split that has the highest number of entities to compute the cluster assignment mappings
        Map<Integer, Set<Integer>> all_clusters = new HashMap<>();

        List<Cluster> assignment = assignments.get(0);
        double[][] centroids = new double[assignment.size()][];
        for (int cluster_id = 0; cluster_id < assignment.size(); cluster_id++) {
            Cluster centroid = assignment.get(cluster_id);
            centroids[cluster_id] = centroid.getCenter();

            //add the inital entity instances from the first assignment
            Set<Integer> cluster_members = all_clusters.get(cluster_id);
            cluster_members = cluster_members == null ? new HashSet<>() : cluster_members;
            all_clusters.put(cluster_id, cluster_members);

            IntIterator entity_iterator = centroid.getMembers();
            while (entity_iterator.hasNext()) {
                int entity_id = entity_iterator.getNext();
                cluster_members.add(entity_id);
            }
        }

        for (int j = 1; j < assignments.size(); j++) {
            List<Cluster> assignment_cmp = assignments.get(j);
            double[][] centroids_cmp = getCentroids(assignment_cmp);

            //map the centroids
            for (int k = 0; k < centroids_cmp.length; k++) {
                double[] centroid = centroids_cmp[k];
                int centroid_id = getClosestCentroidID(centroids, centroid);

                if (!all_clusters.containsKey(centroid_id)) {
                    all_clusters.put(centroid_id, new HashSet<>());
                }

                //add the inital entity instances from the first assignment
                Set<Integer> cluster_members = all_clusters.get(centroid_id);
                cluster_members = cluster_members == null ? new HashSet<>() : cluster_members;
                all_clusters.put(centroid_id, cluster_members);

                IntIterator entity_iterator = assignment_cmp.get(k).getMembers();
                while (entity_iterator.hasNext()) {
                    int entity_id = entity_iterator.getNext();
                    cluster_members.add(entity_id);
                }
            }
        }
        return all_clusters;
    }

    /**
     * Return the centroids of the clusters in a matrix
     *
     * @param clusters
     * @return
     */
    private double[][] getCentroids(List<Cluster> clusters) {
        double[][] rst = new double[clusters.size()][];
        for (int cluster_id = 0; cluster_id < clusters.size(); cluster_id++) {
            Cluster centroid = clusters.get(cluster_id);
            rst[cluster_id] = centroid.getCenter();
        }
        return rst;
    }

    /**
     * Returns the centroid id when merging two centroids from the cluster output of two matrices.
     *
     * @param centroids
     * @param centroid
     * @return
     */
    private int getClosestCentroidID(double[][] centroids, double[] centroid) {
        double tmp_distance = Double.MAX_VALUE;
        int centroid_id = -1;

        for (int k = 0; k < centroids.length; k++) {
            double[] centroid_cmp = centroids[k];
            double euclid_dist = 0.0;
            for (int i = 0; i < centroid.length; i++) {
                euclid_dist += Math.pow(centroid[i] - centroid_cmp[i], 2);
            }
            euclid_dist = Math.sqrt(euclid_dist);

            if (euclid_dist < tmp_distance) {
                tmp_distance = euclid_dist;
                centroid_id = k;
            }
        }

        return centroid_id;
    }

    /**
     * Returns the centroid id when merging two centroids from the cluster output of two matrices.
     *
     * @param centroids
     * @param centroid
     * @return
     */
    private int getClosestCentroidID(DoubleVector[] centroids, DoubleVector centroid) {
        double tmp_distance = Double.MAX_VALUE;
        int centroid_id = -1;

        for (int k = 0; k < centroids.length; k++) {
            DoubleVector centroid_cmp = centroids[k];
            double euclid_dist = 0.0;
            for (int i = 0; i < centroid.length(); i++) {
                euclid_dist += Math.pow(centroid.get(i) - centroid_cmp.get(i), 2);
            }
            euclid_dist = Math.sqrt(euclid_dist);

            if (euclid_dist < tmp_distance) {
                tmp_distance = euclid_dist;
                centroid_id = k;
            }
        }

        return centroid_id;
    }

    /**
     * Perform XMeans clustering
     *
     * @param out_dir_f
     * @param data_file_f
     * @param k
     */
    private void performCOXMeans(String out_dir_f, String data_file_f, int k, String data_dir) {
        XMeansParams xparams = new XMeansParams();
        int min_k = 2;
        int max_k = k;
        xparams.setMinClusters(min_k);
        xparams.setMaxClusters(max_k);
        xparams.setDistanceMetric(new EuclideanDistanceMetric());
        xparams.setWorkerThreadCount(10);

        Set<String> files = new HashSet<>();
        if ((new File(data_file_f)).isFile()) {
            files = FileUtils.readIntoSet(data_file_f, "\n", false);
        } else {
            FileUtils.getFilesList(data_file_f, files);
        }

        files.parallelStream().forEach(file -> {
            if (file.contains("indices") || !FileUtils.fileExists(file, false)) {
                return;
            }
            int start_type_index = file.indexOf("sparse_features/") + "sparse_features/".length();
            String type = file.substring(start_type_index, file.indexOf("/", start_type_index));
            FileUtils.checkDir(out_dir_f + "/" + type + "/");
            try {
                int start_index = file.lastIndexOf("/") + 1;
                int end_index = file.indexOf("_", start_index);
                String lsh_bin = file.substring(start_index, end_index);
                String out_file = out_dir_f + "/" + type + "/" + lsh_bin + "_xmeans.csv.gz";
                try {
                    //check if the tuple file exists and if its already computed the clusters.
                    if (FileUtils.fileExists(out_file, false)) {
                        return;
                    }
                    List<TupleList> tpl_splits = loadSparseTuples(file);
                    if (tpl_splits == null) {
                        return;
                    }

                    List<List<Cluster>> split_clusters = new ArrayList<>();

                    IntStream.range(0, tpl_splits.size()).parallel().forEach(tuple_index -> {
                        TupleList tpl = tpl_splits.get(tuple_index);
                        try {
                            System.out.printf("[C/S] Lsh_bin [%s] has %d/%d tuples/length for type %s for split %d\n", lsh_bin, tpl.getTupleCount(), tpl.getTupleLength(), type, tuple_index);

                            XMeansClusterer xmeans = new XMeansClusterer(tpl, xparams);
                            Thread th = new Thread(xmeans);
                            th.start();
                            List<Cluster> clusters = xmeans.get();
                            System.out.printf("[C/F] Lsh_bin [%s] and type [%s]: result -- %s and %d clusters for split %d\n", lsh_bin, type, xmeans.getTaskOutcome().toString(), clusters.size(), tuple_index);
                            split_clusters.add(clusters);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    while (split_clusters.size() != tpl_splits.size()) ;

                    //merge the clusters
                    Map<Integer, Set<Integer>> clusters = generateClusterAssignmentMappingsKMeans(split_clusters);
                    TIntIntHashMap entity_indices = FileUtils.readCompressedIntoIntMap(data_dir + "/" + type + "/" + lsh_bin + "_indices.csv.gz", "\t");
                    if (entity_indices == null || entity_indices.isEmpty()) {
                        System.out.printf("There is no data about %s\n", data_dir + "/" + type + "/" + lsh_bin + "_indices.csv.gz");
                        return;
                    }
                    StringBuffer sb = new StringBuffer();
                    for (int cluster_id : clusters.keySet()) {
                        Set<Integer> cluster_entities = clusters.get(cluster_id);
                        for (int entity_indice : cluster_entities) {
                            int entity_id = entity_indices.get(entity_indice);
                            sb.append(cluster_id).append("\t").append(entity_id).append("\n");
                        }
                    }
                    FileUtils.saveText(sb.toString(), out_file, false, true);
                    System.out.printf("Finished computing x-means for bin %s and type %s.\n", lsh_bin, type);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Load all entity URI instances of a given entity type.
     *
     * @param loaded_type_index
     * @return
     */
    private THashSet<String> loadEntityTypeInstances(int type_id, String loaded_type_index) throws IOException {
        THashSet<String> entity_instances = new THashSet<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(loaded_type_index);

        while (reader.ready()) {
            String[] tmp = reader.readLine().trim().split("\t");
            if (tmp.length != 2) {
                continue;
            }
            int type_tmp = Integer.valueOf(tmp[0]);
            if (type_tmp != type_id) {
                continue;
            }
            entity_instances.add(tmp[1]);
        }
        return entity_instances;
    }

    /**
     * Loads the locations (the types out of the multiple types of an entity) where the entities are located.
     *
     * @param loaded_type_index
     * @param entity_instances
     * @return
     */
    private THashMap<Integer, THashSet<String>> loadEntityTypeLocations(int type_id, String loaded_type_index, THashSet<String> entity_instances) throws IOException {
        THashMap<Integer, THashSet<String>> entity_loc = new THashMap<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(loaded_type_index);

        while (reader.ready()) {
            String[] tmp = reader.readLine().split("\t");
            int type = Integer.valueOf(tmp[1]);
            if (entity_instances.contains(tmp[0]) && type != type_id) {
                THashSet<String> sub_entity_loc = entity_loc.get(type);
                sub_entity_loc = sub_entity_loc == null ? new THashSet<>() : sub_entity_loc;
                entity_loc.put(type, sub_entity_loc);
                sub_entity_loc.add(tmp[0]);

                //remove those entity URIs that we've found in another type.
                entity_instances.remove(tmp[0]);
            }
        }
        return entity_loc;
    }


    /**
     * Load the type index which consists of types and entity associations.
     *
     * @param type_index_path
     * @return
     */

    private void loadTypeIndex(THashMap<String, Integer> type_index,
                               String type_index_path) throws IOException {
        BufferedReader reader = FileUtils.getCompressedFileReader(type_index_path);
        while (reader.ready()) {
            String line = reader.readLine().trim();

            String[] tmp = line.split("\t");
            if (tmp.length != 2) {
                continue;
            }

            if (!type_index.containsKey(tmp[1])) {
                type_index.put(tmp[1], Integer.valueOf(tmp[0]));
            }
        }
    }

    /**
     * Filters out features that occur below a given threshold.
     *
     * @param text_counts
     */
    private void filterFeatureObjects(THashMap<Integer, TIntIntHashMap> text_counts,
                                      TIntIntHashMap object_counts,
                                      int total_entities) {
        if (text_counts != null) {
            for (int ngram : text_counts.keySet()) {
                TIntIterator term_iterator = text_counts.get(ngram).keySet().iterator();
                while (term_iterator.hasNext()) {
                    int count = text_counts.get(ngram).get(term_iterator.next());
                    if (total_entities < 100000) {
                        if (count < 2) {
                            term_iterator.remove();
                        }
                    } else {
                        if (count < 10) {
                            term_iterator.remove();
                        }
                    }
                }
            }
        } else {
            if (object_counts.size() != 0) {
                TIntIterator object_iterator = object_counts.keySet().iterator();
                while (object_iterator.hasNext()) {
                    int count = object_counts.get(object_iterator.next());
                    if (total_entities < 100000) {
                        if (count < 2) {
                            object_iterator.remove();
                        }
                    } else {
                        if (count < 10) {
                            object_iterator.remove();
                        }
                    }
                }
            }
        }
    }


    /**
     * Processes the entity features for the entity clustering algorithm.
     *
     * @param data_file
     * @param stop_words
     * @throws Exception
     */
    public void processEntityFeatures(String data_file, Set<String> stop_words, String out_dir, int type, int ngrams) throws Exception {
        BufferedReader reader = FileUtils.getCompressedFileReader(data_file);

        //keep the counts of the label textual terms.
        THashMap<Integer, TIntIntHashMap> label_counts = new THashMap<>();
        THashMap<Integer, TIntIntHashMap> text_counts = new THashMap<>();

        //keeps unique identifiers for the different textual terms and object values
        THashMap<String, THashMap<Integer, THashMap<String, Integer>>> terms_dictionary = new THashMap<>();

        THashMap<String, Integer> entity_dictionary = new THashMap<>();
        THashMap<String, Integer> graph_dictionary = new THashMap<>();
        THashMap<String, Integer> objects_dictionary = new THashMap<>();

        terms_dictionary.put("labels", new THashMap<>());
        terms_dictionary.put("text", new THashMap<>());

        //add the different place holders for ngrams
        for (int i = 1; i <= ngrams; i++) {
            THashMap<String, Integer> l_ngram_terms_dict = new THashMap<>();
            THashMap<String, Integer> t_ngram_terms_dict = new THashMap<>();

            terms_dictionary.get("labels").put(i, l_ngram_terms_dict);
            terms_dictionary.get("text").put(i, t_ngram_terms_dict);

            TIntIntHashMap l_ngram_count = new TIntIntHashMap();
            label_counts.put(i, l_ngram_count);

            TIntIntHashMap t_ngram_count = new TIntIntHashMap();
            text_counts.put(i, t_ngram_count);
        }

        //keeps track of the different values for a given entity for the textual terms, objects and the terms in entity labels
        THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels = new THashMap<>();
        THashMap<Integer, TIntHashSet> entity_objects = new THashMap<>();
        THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text = new THashMap<>();
        TIntIntHashMap entity_graph = new TIntIntHashMap();
        TIntIntHashMap object_counts = new TIntIntHashMap();

        int last_graph_index = 0, l_last_term_index = 0, t_last_term_index = 0, last_object_index = 0, last_entity_index = 0;
        while (reader.ready()) {
            String line = reader.readLine();
            String[] tmp = line.split("\t");

            String graph = tmp[0];
            String entity = tmp[1];
            String entity_label = tmp[2].toLowerCase();
            String objects = tmp[3];
            objects = objects.replaceAll("\\[|\\]", "");
            String text = tmp[4].toLowerCase();
            text = text.replaceAll("\\[|\\]", "");

            if (!graph_dictionary.containsKey(graph)) {
                last_graph_index++;
                graph_dictionary.put(graph, last_graph_index);
            }

            if (!entity_dictionary.containsKey(entity)) {
                last_entity_index++;
                entity_dictionary.put(entity, last_entity_index);
            }

            //set the graph URI of the entity
            entity_graph.put(entity_dictionary.get(entity), graph_dictionary.get(graph));

            //add the terms of the entity label
            String[] label_terms = entity_label.split("\\s{1,}");
            THashMap<Integer, TIntIntHashMap> sub_entity_labels = entity_labels.get(entity_dictionary.get(entity));
            sub_entity_labels = sub_entity_labels == null ? new THashMap<>() : sub_entity_labels;
            entity_labels.put(entity_dictionary.get(entity), sub_entity_labels);

            //add the unigrams, bigrams and trigrams from an entity label
            l_last_term_index = Utils.addNGrams(label_terms, sub_entity_labels, label_counts, l_last_term_index, terms_dictionary.get("labels"), stop_words, ngrams);

            //add the textual values
            THashMap<Integer, TIntIntHashMap> sub_entity_text = entity_text.get(entity_dictionary.get(entity));
            sub_entity_text = sub_entity_text == null ? new THashMap<>() : sub_entity_text;
            entity_text.put(entity_dictionary.get(entity), sub_entity_text);

            String[] text_terms = text.split(", ");
            t_last_term_index = Utils.addNGrams(text_terms, sub_entity_text, text_counts, t_last_term_index, terms_dictionary.get("text"), stop_words, ngrams);

            //add the object URI values
            String[] tmp_objects = objects.split(", ");

            TIntHashSet sub_entity_objects = entity_objects.get(entity_dictionary.get(entity));
            sub_entity_objects = sub_entity_objects == null ? new TIntHashSet() : sub_entity_objects;
            entity_objects.put(entity_dictionary.get(entity), sub_entity_objects);

            //count the occurrence of different object values
            for (String object : tmp_objects) {
                if (!objects_dictionary.containsKey(object)) {
                    last_object_index++;
                    objects_dictionary.put(object, last_object_index);
                }

                sub_entity_objects.add(objects_dictionary.get(object));

                Integer counts = object_counts.get(objects_dictionary.get(object));
                counts = counts == null ? 0 : counts;
                counts += 1;
                object_counts.put(objects_dictionary.get(object), counts);
            }
        }

        //write the terms, objects and entity indexes into their corresponding data structures
        FileUtils.checkDir(out_dir);
        FileUtils.checkDir(out_dir + "/" + type);

        FileUtils.saveObject(label_counts, out_dir + "/" + type + "/label_counts.obj");
        FileUtils.saveObject(text_counts, out_dir + "/" + type + "/text_counts.obj");
        FileUtils.saveObject(object_counts, out_dir + "/" + type + "/object_counts.obj");

        FileUtils.saveObject(terms_dictionary, out_dir + "/" + type + "/dict_terms.obj");
        FileUtils.saveObject(objects_dictionary, out_dir + "/" + type + "/dict_objects.obj");
        FileUtils.saveObject(entity_dictionary, out_dir + "/" + type + "/dict_entity.obj");
        FileUtils.saveObject(graph_dictionary, out_dir + "/" + type + "/dict_graph.obj");

        FileUtils.saveObject(entity_labels, out_dir + "/" + type + "/entity_labels.obj");
        FileUtils.saveObject(entity_objects, out_dir + "/" + type + "/entity_objects.obj");
        FileUtils.saveObject(entity_text, out_dir + "/" + type + "/entity_text.obj");
        FileUtils.saveObject(entity_graph, out_dir + "/" + type + "/entity_graph.obj");
    }


    /**
     * @param entity_labels
     * @param entity_objects
     * @param entity_text
     * @param entity_graph
     */
    private void computeClusteringTupleFeatures(THashMap<Integer, TIntHashSet> lsh_bins,
                                                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels,
                                                THashMap<Integer, TIntHashSet> entity_objects,
                                                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text,
                                                TIntIntHashMap entity_graph,
                                                String out_dir, String type) throws IOException {
        lsh_bins.keySet().parallelStream().forEach(bin_entry -> {
            try {
                TIntHashSet entity_indices = lsh_bins.get(bin_entry);
                int total_entities = entity_indices.size();
                String bin_name = "" + bin_entry;
                String feature_file = out_dir + "/" + type + "/" + bin_name + "_tuples.obj";

                if (FileUtils.fileExists(feature_file, false) || FileUtils.fileExists(feature_file + ".gz", false) || total_entities < 10) {
                    return;
                }

                //filter the entity objects and ngrams from the feature set.
                TIntIntHashMap entity_object_indices = new TIntIntHashMap();
                THashMap<Integer, TIntIntHashMap> text_ngram_indices = new THashMap<>();
                THashMap<Integer, TIntIntHashMap> label_ngram_indices = new THashMap<>();

                for (int entity_id : entity_indices.toArray()) {
                    for (int object_id : entity_objects.get(entity_id).toArray()) {
                        Integer obj_count = entity_object_indices.get(object_id);
                        obj_count = obj_count == null ? 0 : obj_count;
                        obj_count++;
                        entity_object_indices.put(object_id, obj_count);
                    }
                }

                //add the text terms and label terms
                for (int entity_id : entity_indices.toArray()) {
                    for (int ngram : entity_text.get(entity_id).keySet()) {
                        TIntIntHashMap ngram_indices = text_ngram_indices.get(ngram);
                        ngram_indices = ngram_indices == null ? new TIntIntHashMap() : ngram_indices;
                        text_ngram_indices.put(ngram, ngram_indices);

                        for (int term_id : entity_text.get(entity_id).get(ngram).keys()) {
                            Integer count = ngram_indices.get(term_id);
                            count = count == null ? 0 : count;
                            count += 1;

                            ngram_indices.put(term_id, count);
                        }
                    }
                    for (int ngram : entity_labels.get(entity_id).keySet()) {
                        TIntIntHashMap ngram_indices = label_ngram_indices.get(ngram);
                        ngram_indices = ngram_indices == null ? new TIntIntHashMap() : ngram_indices;
                        label_ngram_indices.put(ngram, ngram_indices);

                        for (int term_id : entity_labels.get(entity_id).get(ngram).keys()) {
                            Integer count = ngram_indices.get(term_id);
                            count = count == null ? 0 : count;
                            count += 1;

                            ngram_indices.put(term_id, count);
                        }
                    }
                }

                //filter out first the text terms that do not appear frequently
                filterFeatureObjects(text_ngram_indices, null, total_entities);
                filterFeatureObjects(label_ngram_indices, null, total_entities);
                filterFeatureObjects(null, entity_object_indices, total_entities);

                int text_terms_total = 0, label_terms_total = 0;
                for (int ngram : text_ngram_indices.keySet()) {
                    text_terms_total += text_ngram_indices.get(ngram).size();
                }
                for (int ngram : label_ngram_indices.keySet()) {
                    label_terms_total += label_ngram_indices.get(ngram).size();
                }

                //add an extra attribute for the entity graph.
                int total_attributes = text_terms_total + label_terms_total + entity_object_indices.size() + 2;

                System.out.printf("%d entities in the LSH bin %s and for type %s, with %d objects, %d text-terms and %d label-terms.\n", total_entities, bin_name, type, entity_object_indices.size(), text_terms_total, label_terms_total);
                //perform the LSH of entities.
                computeTupleFeatures(entity_indices, entity_objects, entity_labels, entity_text, entity_graph, out_dir, type, bin_name, entity_object_indices.keys(), text_ngram_indices, label_ngram_indices, total_attributes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * @param entity_labels
     * @param entity_objects
     * @param entity_text
     * @param entity_graph
     */
    private void computeClusteringTupleFeatures(THashMap<Integer, TIntHashSet> lsh_bins,
                                                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels,
                                                THashMap<Integer, TIntHashSet> entity_objects,
                                                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text,
                                                TIntIntHashMap entity_graph,
                                                String out_dir, String type, String lsh_bin) throws IOException {
        lsh_bins.keySet().parallelStream().forEach(bin_entry -> {
            try {
                TIntHashSet entity_indices = lsh_bins.get(bin_entry);
                int total_entities = entity_indices.size();
                String bin_name = "" + bin_entry;
                String feature_file = out_dir + "/" + type + "/" + bin_name + "_tuples.obj";

                if (FileUtils.fileExists(feature_file, false) || FileUtils.fileExists(feature_file + ".gz", false) ||
                        !bin_name.equals(lsh_bin) || total_entities < 10) {
                    return;
                }

                //filter the entity objects and ngrams from the feature set.
                TIntIntHashMap entity_object_indices = new TIntIntHashMap();
                THashMap<Integer, TIntIntHashMap> text_ngram_indices = new THashMap<>();
                THashMap<Integer, TIntIntHashMap> label_ngram_indices = new THashMap<>();

                for (int entity_id : entity_indices.toArray()) {
                    for (int object_id : entity_objects.get(entity_id).toArray()) {
                        Integer obj_count = entity_object_indices.get(object_id);
                        obj_count = obj_count == null ? 0 : obj_count;
                        obj_count++;
                        entity_object_indices.put(object_id, obj_count);
                    }
                }

                //add the text terms and label terms
                for (int entity_id : entity_indices.toArray()) {
                    for (int ngram : entity_text.get(entity_id).keySet()) {
                        TIntIntHashMap ngram_indices = text_ngram_indices.get(ngram);
                        ngram_indices = ngram_indices == null ? new TIntIntHashMap() : ngram_indices;
                        text_ngram_indices.put(ngram, ngram_indices);

                        for (int term_id : entity_text.get(entity_id).get(ngram).keys()) {
                            Integer count = ngram_indices.get(term_id);
                            count = count == null ? 0 : count;
                            count += 1;

                            ngram_indices.put(term_id, count);
                        }
                    }
                    for (int ngram : entity_labels.get(entity_id).keySet()) {
                        TIntIntHashMap ngram_indices = label_ngram_indices.get(ngram);
                        ngram_indices = ngram_indices == null ? new TIntIntHashMap() : ngram_indices;
                        label_ngram_indices.put(ngram, ngram_indices);

                        for (int term_id : entity_labels.get(entity_id).get(ngram).keys()) {
                            Integer count = ngram_indices.get(term_id);
                            count = count == null ? 0 : count;
                            count += 1;

                            ngram_indices.put(term_id, count);
                        }
                    }
                }

                //filter out first the text terms that do not appear frequently
                filterFeatureObjects(text_ngram_indices, null, total_entities);
                filterFeatureObjects(label_ngram_indices, null, total_entities);
                filterFeatureObjects(null, entity_object_indices, total_entities);

                int text_terms_total = 0, label_terms_total = 0;
                for (int ngram : text_ngram_indices.keySet()) {
                    text_terms_total += text_ngram_indices.get(ngram).size();
                }
                for (int ngram : label_ngram_indices.keySet()) {
                    label_terms_total += label_ngram_indices.get(ngram).size();
                }

                //add an extra attribute for the entity graph.
                int total_attributes = text_terms_total + label_terms_total + entity_object_indices.size() + 2;

                System.out.printf("%d entities in the LSH bin %s and for type %s, with %d objects, %d text-terms and %d label-terms.\n", total_entities, bin_name, type, entity_object_indices.size(), text_terms_total, label_terms_total);
                //perform the LSH of entities.
                computeTupleFeatures(entity_indices, entity_objects, entity_labels, entity_text, entity_graph, out_dir, type, bin_name, entity_object_indices.keys(), text_ngram_indices, label_ngram_indices, total_attributes);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


    /**
     * After bucketing of entities based on the LSH approach. We compute the entity clustering features for the
     * localized entity groups.
     *
     * @param entity_objects
     * @param entity_labels
     * @param entity_text
     * @param entity_graph
     * @param out_dir
     * @param type
     */
    private void computeTupleFeatures(TIntHashSet entity_indices,
                                      THashMap<Integer, TIntHashSet> entity_objects,
                                      THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels,
                                      THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text,
                                      TIntIntHashMap entity_graph,
                                      String out_dir, String type, String lsh_bin,
                                      int[] object_indices,
                                      THashMap<Integer, TIntIntHashMap> text_ngram_indices,
                                      THashMap<Integer, TIntIntHashMap> label_ngram_indices,
                                      int total_attributes) throws IOException {

        //store the entity and the corresponding indices.
        StringBuffer sb_entity_indices = new StringBuffer();

        //filter out entities that have all zero feature items.
        filterZeroEntities(entity_indices, object_indices, text_ngram_indices, label_ngram_indices, entity_objects, entity_labels, entity_text);

        //load first the tuple list.
        int tuple_no = entity_indices.size();
        int entity_index = 0;
        int feature_index = 0;
        StringBuffer sb = new StringBuffer();
        for (int entity_id : entity_indices.toArray()) {
            feature_index = 0;
            sb_entity_indices.append(entity_index).append("\t").append(entity_id).append("\n");
            sb.append(feature_index).append(";").append(entity_id).append("\t");
            //count non-zero elements.
            feature_index++;

            for (int object_id : object_indices) {
                int entity_has_object = entity_objects.get(entity_id).contains(object_id) ? 1 : 0;
                if (entity_has_object != 0) {
                    sb.append(feature_index).append(";").append(entity_has_object).append("\t");
                }
                feature_index++;
            }

            //append the label values
            THashMap<Integer, TIntIntHashMap> sub_entity_labels = entity_labels.get(entity_id);
            for (int ngram : label_ngram_indices.keySet()) {
                for (int term_id : label_ngram_indices.get(ngram).keys()) {
                    int entity_freq = sub_entity_labels.containsKey(ngram) &&
                            sub_entity_labels.get(ngram).containsKey(term_id) ? sub_entity_labels.get(ngram).get(term_id) : 0;

                    //append the weight of the particular term as tf-idf.
                    if (entity_freq != 0) {
                        sb.append(feature_index).append(";").append(entity_freq).append("\t");
                    }
                    feature_index++;
                }
            }

            //append the text values
            THashMap<Integer, TIntIntHashMap> sub_entity_text = entity_text.get(entity_id);
            for (int ngram : text_ngram_indices.keySet()) {
                for (int term_id : text_ngram_indices.get(ngram).keys()) {
                    int entity_freq = sub_entity_text.containsKey(ngram) &&
                            sub_entity_text.get(ngram).containsKey(term_id) ? sub_entity_text.get(ngram).get(term_id) : 0;

                    if (entity_freq != 0) {
                        sb.append(feature_index).append(";").append(entity_freq).append("\t");
                    }
                    feature_index++;
                }
            }

            int graph_id = entity_graph != null && entity_graph.containsKey(entity_id) ? entity_graph.get(entity_id) : 0;
            sb.append(feature_index).append(";").append(graph_id).append("\n");
            entity_index++;

            if (sb.length() != 0 && sb.length() > 1000) {
                FileUtils.saveText(sb.toString(), out_dir + "/" + type + "/" + lsh_bin + "_tuples.obj", true, false);
                sb.delete(0, sb.length());
            }
        }

        System.out.printf("Finished creating tuples list with %d attributes and %d tuples for type %s.\n", total_attributes, tuple_no, type);
        FileUtils.saveText(sb.toString(), out_dir + "/" + type + "/" + lsh_bin + "_tuples.obj", true, false);
        FileUtils.saveText(sb_entity_indices.toString(), out_dir + "/" + type + "/" + lsh_bin + "_indices.csv.gz", false, true);
    }

    /**
     * Filters out entities that do not contain none of the feature items.
     *
     * @param entity_indices
     * @param object_indices
     * @param text_ngram_indices
     * @param label_ngram_indices
     * @param entity_objects
     * @param entity_labels
     * @param entity_text
     */
    private void filterZeroEntities(TIntHashSet entity_indices, int[] object_indices,
                                    THashMap<Integer, TIntIntHashMap> text_ngram_indices,
                                    THashMap<Integer, TIntIntHashMap> label_ngram_indices,
                                    THashMap<Integer, TIntHashSet> entity_objects,
                                    THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_labels,
                                    THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text) {
        int total = entity_indices.size();

        //store the entity id values for those entities that do not have any feature value.
        Set<Integer> remove_entities = new HashSet<>();
        Arrays.stream(entity_indices.toArray()).parallel().forEach(entity_id -> {
            int non_zero_features = 0;
            for (int object_id : object_indices) {
                if (entity_objects.get(entity_id).contains(object_id)) {
                    non_zero_features++;
                }
            }

            //append the label values
            THashMap<Integer, TIntIntHashMap> sub_entity_labels = entity_labels.get(entity_id);
            for (int ngram : label_ngram_indices.keySet()) {
                for (int term_id : label_ngram_indices.get(ngram).keys()) {
                    int entity_freq = sub_entity_labels.containsKey(ngram) && sub_entity_labels.get(ngram).containsKey(term_id) ? sub_entity_labels.get(ngram).get(term_id) : 0;
                    if (entity_freq != 0) {
                        non_zero_features++;
                    }
                }
            }

            //append the text values
            THashMap<Integer, TIntIntHashMap> sub_entity_text = entity_text.get(entity_id);
            for (int ngram : text_ngram_indices.keySet()) {
                for (int term_id : text_ngram_indices.get(ngram).keys()) {
                    int entity_freq = sub_entity_text.containsKey(ngram) && sub_entity_text.get(ngram).containsKey(term_id) ? sub_entity_text.get(ngram).get(term_id) : 0;
                    if (entity_freq != 0) {
                        non_zero_features++;
                    }
                }
            }

            if (non_zero_features == 0) {
                remove_entities.add(entity_id);
            }
        });

        entity_indices.removeAll(remove_entities);
        System.out.printf("After removing entities with zero features %d/%d of entities.\n", total, entity_indices.size());
    }


    /**
     * Computes the raw features of entities for clustering.
     *
     * @param data_file
     * @param type_indice
     * @param out_dir
     * @param stop_words
     * @param filter_predicates
     * @throws IOException
     * @throws ParseException
     */
    private void computeEntityClusterFeatures(String data_file, THashMap<Integer, THashSet<String>> entity_location,
                                              int type_indice, String out_dir, Set<String> stop_words,
                                              Set<String> filter_predicates) throws IOException, ParseException {
        String out_file = out_dir + "/type_" + type_indice + ".csv.gz";
        BufferedReader reader = FileUtils.getCompressedFileReader(data_file + "/" + type_indice + "_data.nq.gz");
        loadEntityClusterProfiles(reader, out_file, stop_words, filter_predicates, null);

        //load the cluster profiles for the remainder of entities that are stored at different type files.
        for (int type_id : entity_location.keySet()) {
            reader = FileUtils.getCompressedFileReader(data_file + "/" + type_id + "_data.nq.gz");
            loadEntityClusterProfiles(reader, out_file, stop_words, filter_predicates, entity_location.get(type_id));
        }
    }

    private void loadEntityClusterProfiles(BufferedReader reader, String out_file, Set<String> stop_words, Set<String> filter_predicates, THashSet<String> entity_uris) throws IOException {
        List<String[]> entity_triples = new ArrayList<>();
        String current_entity = "";
        while (reader.ready()) {
            String line = reader.readLine();
            String[] triples = line.split("\t");

            String entity_tmp = triples[0].trim();
            if (entity_uris != null && !entity_uris.contains(entity_tmp)) {
                continue;
            }

            if (!current_entity.isEmpty() && !current_entity.equals(entity_tmp)) {
                StringBuffer text_content = new StringBuffer();
                StringBuffer inter_entity_links = new StringBuffer();
                String label = "";
                String graph = "";
                //write the features for this entity
                for (String[] triple : entity_triples) {
                    if (triple.length != 4) {
                        continue;
                    }
                    graph = triple[3];

                    String predicate = triple[1];
                    if (predicate.contains("label") || predicate.contains("title") || predicate.contains("name")) {
                        label = triple[2];
                        label = label.contains("/") ? label.substring(label.lastIndexOf("/") + 1) : label;
                    } else if (triple[2].startsWith("<")) {
                        inter_entity_links.append(triple[2]).append(";");
                    } else { //check if its only a number
                        String tmp_value = triple[2].replaceAll("\"", "").trim();
                        tmp_value = tmp_value.contains("^^") ? tmp_value.substring(0, tmp_value.indexOf("^")) : tmp_value;
                        tmp_value = tmp_value.contains("@") ? tmp_value.substring(0, tmp_value.indexOf("@")) : tmp_value;

                        if (!StringUtils.isNumeric(tmp_value) && !(tmp_value.equals("true") || tmp_value.equals("false"))) {
                            text_content.append(tmp_value).append("\n");
                        }
                    }
                }
                String links = inter_entity_links.toString();
                String[] terms = StringUtils.split(text_content.toString());
                Set<String> terms_set = new HashSet<>();
                SnowballProgram sp = new EnglishStemmer();
                for (String s : terms) {
                    sp.setCurrent(s);
                    sp.getCurrent();
                    String val = sp.getCurrent();
                    if (stop_words.contains(val)) {
                        continue;
                    }
                    terms_set.add(val);
                }

                String str_terms = terms_set.toString();
                StringBuffer sb = new StringBuffer();
                sb.append(graph).append("\t").append(entity_tmp).append("\t[").append(label).append("]\t[").append(links).append("]\t").append(str_terms).append("\n");
                FileUtils.saveText(sb.toString(), out_file, true, false);
                entity_triples.clear();
            }
            current_entity = entity_tmp;
            if (filter_predicates.contains(triples[1])) {
                continue;
            }
            entity_triples.add(triples);
        }
    }

    /**
     * Generate an index of types and entities, it is later used to get the data for the different entity types.
     *
     * @param type_filter
     * @param btc_data
     * @param out_dir
     */
    public void generateTypeIndex(Set<String> type_filter, String btc_data, String out_dir) {
        Set<String> btc_files = new HashSet<>();
        FileUtils.getFilesList(btc_data, btc_files);
        Map<String, Integer> type_names = new HashMap<>();
        AtomicInteger atm = new AtomicInteger();
        btc_files.parallelStream().forEach(btc_file -> {
            try {
                if (!btc_file.contains("btc-2010")) {
                    return;
                }
                BufferedReader reader = FileUtils.getCompressedFileReader(btc_file);
                StringBuffer sb = new StringBuffer();

                while (reader.ready()) {
                    String line = reader.readLine();
                    try {
                        Node[] triple = NxParser.parseNodes(line);

                        String tmp_entity = triple[0].toString();
                        String tmp_predicate = triple[1].toString();
                        String tmp_object = triple[2].toString();
                        //get the type of the current entity.
                        if (tmp_predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                            //if the current type is not in the current list of types that we are looking for, skip the entity.
                            if (type_filter.contains(tmp_object)) {
                                continue;
                            }

                            if (!type_names.containsKey(tmp_object)) {
                                int type_index = atm.incrementAndGet();
                                type_names.put(tmp_object, type_index);
                            }
                            sb.append(type_names.get(tmp_object)).append("\t").append(tmp_entity).append("\n");
                            if (sb.length() != 0 && sb.length() > 10000) {
                                FileUtils.saveText(sb.toString(), out_dir + "/type_index.csv", true, false);
                                sb.delete(0, sb.length());
                            }
                        }
                    } catch (Exception e) {
                        System.out.printf("Skipping triple: %s with error %s\n", line, e.getMessage());
                    }
                }
                FileUtils.saveText(sb.toString(), out_dir + "/type_index.csv", true, false);
                System.out.printf("Finished processing file %s.\n", btc_file);
            } catch (Exception e) {
                System.out.printf("Skipping file: %s.\n", btc_file);
            }
        });
        StringBuffer sb = new StringBuffer();
        type_names.keySet().forEach(type_name -> sb.append(type_name).append("\t").append(type_names.get(type_name)).append("\n"));
        FileUtils.saveText(sb.toString(), out_dir + "/type_indices.csv", false, false);
    }

    /**
     * Load the set of entities that match a particular set of graph URIs and types.
     *
     * @param btc_data
     * @param out_dir
     */
    public void loadEntitiesForGraphAndTypes(THashMap<String, Integer> type_index,
                                             String btc_data,
                                             String out_dir) {
        Set<String> btc_files = new HashSet<>();
        FileUtils.getFilesList(btc_data, btc_files);

        btc_files.parallelStream().forEach(btc_file -> {
            try {
                BufferedReader reader = FileUtils.getCompressedFileReader(btc_file);

                //keep a map of buffers for all types.
                int triple_count = 0;
                Map<Integer, StringBuffer> type_buffers = new HashMap<>();

                while (reader.ready()) {
                    String line = reader.readLine();
                    try {
                        Node[] triple = NxParser.parseNodes(line);
                        String entity = triple[0].toString();
                        String predicate = triple[1].toN3();
                        String object = triple[2].toN3();
                        String graph = triple[3].toN3();
                        if (!type_index.containsKey(entity)) {
                            continue;
                        }

                        int type = type_index.get(entity);
                        if (!type_buffers.containsKey(type)) {
                            type_buffers.put(type, new StringBuffer());
                        }
                        type_buffers.get(type).append(entity).append("\t").
                                append(predicate).append("\t").
                                append(object).append("\t").
                                append(graph).append("\n");

                        //free the buffers.
                        if (triple_count != 0 && triple_count % 1000 == 0) {
                            for (int type_id : type_buffers.keySet()) {
                                StringBuffer sb = type_buffers.get(type_id);
                                FileUtils.saveText(sb.toString(), out_dir + "/" + type_id + "_data.nq.gz", true, false);
                                sb.delete(0, sb.length());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    triple_count++;
                }

                for (int type_id : type_buffers.keySet()) {
                    StringBuffer sb = type_buffers.get(type_id);
                    FileUtils.saveText(sb.toString(), out_dir + "/" + type_id + "_data.nq.gz", true, false);
                    sb.delete(0, sb.length());
                }

                System.out.println("Finished processing file: \t" + btc_file);
            } catch (Exception e) {
                System.out.printf("Error parsing file: %s with error message: %s\n", btc_file, e.getMessage());
            }
        });
    }

    /**
     * Load the set of entities that belong to the same bins in the LSH clustering.
     *
     * @param lsh_bins
     * @return
     */
    private THashMap<Integer, TIntHashSet> loadLSHEntityBinsSimple(String lsh_bins, int bin_filter) throws IOException {
        BufferedReader reader = FileUtils.getCompressedFileReader(lsh_bins);

        //first read the entities along with their buckets for the given bin range
        THashMap<Integer, TIntHashSet> bin = new THashMap<>();

        while (reader.ready()) {
            String line = reader.readLine();
            String[] tmp = line.split("\t");

            int bin_val = Integer.valueOf(tmp[0]);
            int bin_bucket = Integer.valueOf(tmp[1]);
            int entity_id = Integer.valueOf(tmp[2]);

            //load the first bin
            if (bin_val != bin_filter) {
                continue;
            }

            TIntHashSet sub_bin = bin.get(bin_bucket);
            sub_bin = sub_bin == null ? new TIntHashSet() : sub_bin;
            bin.put(bin_bucket, sub_bin);
            sub_bin.add(entity_id);
        }

        return bin;
    }

    /**
     * A customized tuple loader. This is necessary as the original library doesn't support file compression. It simply
     * reads a file line by line and splits the lines using a comma separator.
     *
     * @param file
     * @return
     * @throws IOException
     */
    public List<TupleList> loadSparseTuples(String file) {
        try {
            int split_size = 5000;
            int entity_no = getFeatureFileCounts(file);

            //compute the number of splits for the matrix. We allow the maximum number to be 5000 entities per matrix
            //after that we split the matrix into chunks of 5000s and compute individually the results, the final output
            //of clusters is merged by comparing the centroids in the clusters.
            int splits = (entity_no / split_size) + 1;
            System.out.printf("Loading tuples for file %s which is split into %d splits from %d entities\n", file, splits, entity_no);
            List<TIntDoubleMap[]> split_features = new ArrayList<>();

            BufferedReader reader = FileUtils.getCompressedFileReader(file);
            if (entity_no == 0) {
                return null;
            }
            //add the split features
            for (int i = 0; i < splits; i++) {
                if (i + 1 == splits) {
                    //if its the last split check how many entities are there
                    int remainder = entity_no - (i * split_size);
                    split_features.add(i, new TIntDoubleMap[remainder]);
                } else {
                    split_features.add(i, new TIntDoubleMap[split_size]);
                }
            }

            int index = 0;
            int split_counter = 0;
            int split_index = 0;
            int max_key = 0;
            boolean is_empty_file = false;
            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    index++;
                    continue;
                }
                is_empty_file = true;
                //increment split counter if its above some value.
                if (index != 0 && index % split_size == 0) {
                    split_counter++;
                    split_index = 0;
                }

                //construct an int array which holds the feature position and its value. Update dynamically the array size.
                Iterator<String> tmp = Splitter.on("\t").split(line).iterator();
                TIntDoubleMap values = new TIntDoubleHashMap();
                while (tmp.hasNext()) {
                    String[] key_val = tmp.next().split(";");

                    int key = Integer.valueOf(key_val[0].intern());
                    int val = Integer.valueOf(key_val[1].intern());
                    values.put(key, val);

                    if (key > max_key) {
                        max_key = key;
                    }
                }
                split_features.get(split_counter)[split_index] = values;

                index++;
                split_index++;
            }
            max_key++;
            reader.close();
            if (!is_empty_file) {
                return null;
            }
            List<TupleList> rst = new ArrayList<>();
            for (TIntDoubleMap[] split_feature : split_features) {
                rst.add(new SparseMap2DTupleList(split_feature, max_key));
            }
            return rst;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * A customized tuple loader. This is necessary as the original library doesn't support file compression. It simply
     * reads a file line by line and splits the lines using a comma separator.
     *
     * @param file
     * @return
     * @throws IOException
     */
    public List<int[][][]> optimizedLoadTuple2DArray(String file) {
        try {
            int entity_no = getFeatureFileCounts(file);

            if (entity_no < 100) {
                return null;
            }
            //compute the number of splits for the matrix. We allow the maximum number to be 5000 entities per matrix
            //after that we split the matrix into chunks of 5000s and compute individually the results, the final output
            //of clusters is merged by comparing the centroids in the clusters.
            int splits = (entity_no / 5000) + 1;
            System.out.printf("Loading tuples for file %s which is split into %d splits\n", file, splits);
            List<int[][][]> split_features = new ArrayList<>();

            //add the split features
            for (int i = 0; i < splits; i++) {
                if (i + 1 == splits) {
                    //if its the last split check how many entities are there
                    int remainder = entity_no - (i * 5000);
                    split_features.add(i, new int[remainder][][]);
                } else {
                    split_features.add(i, new int[5000][][]);
                }
            }

            int index = 0;
            int split_counter = 0;
            int split_index = 0;
            BufferedReader reader = FileUtils.getCompressedFileReader(file);
            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    index++;
                    continue;
                }

                //increment split counter if its above some value.
                if (index != 0 && index % 5000 == 0) {
                    split_counter++;
                    split_index = 0;
                }

                //construct an int array which holds the feature position and its value. Update dynamically the array size.
                Iterator<String> tmp = Splitter.on("\t").split(line).iterator();
                TIntIntHashMap tmp_values = new TIntIntHashMap();

                while (tmp.hasNext()) {
                    String[] key_val = tmp.next().split(";");

                    int key = Integer.valueOf(key_val[0].intern());
                    int val = Integer.valueOf(key_val[1].intern());
                    tmp_values.put(key, val);
                }

                int[][] entity_features = new int[tmp_values.size()][2];
                split_features.get(split_counter)[split_index] = entity_features;

                int non_zero_feature_index = 0;
                for (int i : tmp_values.keys()) {
                    entity_features[non_zero_feature_index][0] = i;
                    entity_features[non_zero_feature_index][1] = tmp_values.get(i);
                    non_zero_feature_index++;
                }
                index++;
                split_index++;
            }

            reader.close();
            return split_features;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private int getFeatureFileCounts(String file) throws IOException {
        int counter = 0;
        BufferedReader reader = FileUtils.getCompressedFileReader(file);
        while (reader.ready()) {
            String line = reader.readLine();
            if (line == null || line.isEmpty()) {
                continue;
            }
            counter++;
        }
        reader.close();
        return counter;
    }
}
