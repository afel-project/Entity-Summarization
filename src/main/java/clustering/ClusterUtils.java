package clustering;

import utils_package.FileUtils;
import utils_package.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by besnik on 12/5/14.
 */
public class ClusterUtils {
    public static void main(String[] args) throws IOException, InterruptedException {
        String data_dir = "", out_dir = "",
                btc_index = "", rdf3xengine = "",
                operation = "", queries = "", tag = "",
                cluster_dir = "", entity_index = "", baseline_entities_path = "";

        int cluster_size = 10, entity_size = 10;
        boolean is_spectral = false, is_baseline = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-data_dir")) {
                data_dir = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-cluster_size")) {
                cluster_size = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-entity_size")) {
                entity_size = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-btc_index")) {
                btc_index = args[++i];
            } else if (args[i].equals("-rdf3x")) {
                rdf3xengine = args[++i];
            } else if (args[i].equals("-is_spectral")) {
                is_spectral = Boolean.valueOf(args[++i]);
            } else if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-queries")) {
                queries = args[++i];
            } else if (args[i].equals("-cluster_dir")) {
                cluster_dir = args[++i];
            } else if (args[i].equals("-entity_index")) {
                entity_index = args[++i];
            } else if (args[i].equals("-tag")) {
                tag = args[++i];
            } else if (args[i].equals("-is_baseline")) {
                is_baseline = Boolean.valueOf(args[++i]);
            } else if (args[i].equals("-baseline_entities")) {
                baseline_entities_path = args[++i];
            }
        }

        if (operation.equals("clusters")) {
            Set<String> cluster_files = new HashSet<>();
            FileUtils.getFilesList(cluster_dir, cluster_files);
            String[] files_array = new String[cluster_files.size()];
            cluster_files.toArray(files_array);

            Random rand = new Random();
            Set<String> sampled_clusters = new HashSet<>();

            while (sampled_clusters.size() < cluster_size) {
                int index = rand.nextInt(cluster_files.size());
                String cluster_file = files_array[index];

                Map<Integer, Set<Integer>> cluster_data = null;
                try {
                    if (is_spectral) {
                        cluster_data = FileUtils.readIntMapNestedSet(cluster_file, "\t");
                    } else {
                        cluster_data = FileUtils.readIntMapSet(cluster_file, "\t");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                double avg_entities = 0.0;
                for (int cluster_id : cluster_data.keySet()) {
                    avg_entities += cluster_data.get(cluster_id).size();
                }
                avg_entities /= cluster_data.size();
                if (cluster_data.size() < 10 || avg_entities < 20) {
                    continue;
                }
                System.out.printf("Adding %s file for evaluation.\n", cluster_file);
                sampled_clusters.add(cluster_file);
            }

            //load the samples
            Map<String, Map<Integer, Set<Integer>>> data = loadEntityEvalClusters(sampled_clusters, entity_size, is_spectral);
            loadEntityProfiles(data, btc_index, rdf3xengine, out_dir, is_spectral, entity_index);
        } else if (operation.equals("entities")) {
            //load the entity set
            Map<String, String> query_map = FileUtils.readIntoStringMap(queries, "\t", false);
            Map<String, Map<String, Double>> entities = loadEntities(data_dir, entity_size);

            Map<String, Map<String, Double>> baseline_entities = null;
            if (!is_baseline) {
                baseline_entities = loadEntities(baseline_entities_path, entity_size);
            }
            System.out.printf("Loaded data for file %s with %d queries\n", data_dir, entities.size());

            //load the entity profiles.
            StringBuffer sb_entity = new StringBuffer();

            for (String qid : query_map.keySet()) {
                Map<String, Double> qid_entities = null;
                if (!entities.containsKey(qid) && !is_baseline) {
                    qid_entities = baseline_entities.get(qid);
                } else {
                    qid_entities = entities.get(qid);
                }
                if (qid_entities != null) {
                    for (String entity : qid_entities.keySet()) {
                        double score = qid_entities.get(entity);
                        sb_entity.append(qid).append("\t").append(entity).append("\t").append(score).append("\n");
                    }
                }
            }
            FileUtils.saveText(sb_entity.toString(), out_dir + tag + "_entities.csv.gz", false, true);
        } else if (operation.equals("ground_truth")) {
            Set<String> ranked_lists = new HashSet<>();
            FileUtils.getFilesList(data_dir, ranked_lists);

            Map<String, Set<String>> gt = new HashMap<>();


            Map<String, String> query_map = FileUtils.readIntoStringMap(queries, "\t", false);
            //load the set of entities per query
            for (String qid : query_map.keySet()) {
                System.out.printf("Finished query %s.\n", query_map.get(qid));
                Set<String> sub_gt = new LinkedHashSet<>();
                gt.put(qid, sub_gt);

                //add the top-50 entities from all files
                List<List<String>> qid_data = new ArrayList<>();
                Set<String> all_entities = new LinkedHashSet<>();
                boolean is_empty = true;
                for (String file : ranked_lists) {
                    List<String> sub_qid_data = loadQueryEntities(file, entity_size, qid);
                    qid_data.add(sub_qid_data);

                    if (sub_qid_data != null && sub_qid_data.size() != 0) {
                        is_empty = false;
                        all_entities.addAll(sub_qid_data);
                    }
                }

                //get the entities
                int entity_counter = 0;
                while (sub_gt.size() < entity_size && !is_empty && entity_counter < all_entities.size()) {
                    for (List<String> sub_qid_data : qid_data) {
                        if (sub_qid_data.size() > entity_counter) {
                            sub_gt.add(sub_qid_data.get(entity_counter));
                        }
                    }
                    entity_counter++;
                }
            }

            StringBuffer sb = new StringBuffer();
            for (String qid : gt.keySet()) {
                int rank = 0;
                for (String entity : gt.get(qid)) {
                    sb.append(query_map.get(qid)).append("\t").append(rank).append("\t").append(entity).append("\n");
                    rank++;
                }
            }
            FileUtils.saveText(sb.toString(), out_dir + tag + "_ground_truth.csv.gz", false, true);
        }
    }

    /**
     * load baseline entities.
     *
     * @param data
     * @return
     */
    private static Map<String, Map<String, Double>> loadEntities(String data, int entity_size) {
        String[] lines = FileUtils.readText(data).split("\n");
        Map<String, Map<String, Double>> rst = new HashMap<>();

        for (String line : lines) {
            String[] tmp = line.split("\t");
            String qid = tmp[0];
            String entity = tmp[1];
            double bm25score = Double.valueOf(tmp[2]);

            Map<String, Double> sub_rst = rst.get(qid);
            sub_rst = sub_rst == null ? new HashMap<>() : sub_rst;
            rst.put(qid, sub_rst);

            //check if the cluster has reached its size limit
            if (sub_rst.size() <= entity_size) {
                sub_rst.put(entity, bm25score);
            }
        }

        return rst;
    }

    /**
     * load baseline entities.
     *
     * @param data
     * @return
     */
    private static List<String> loadQueryEntities(String data, int entity_size, String qid_filter) {
        String[] lines = FileUtils.readText(data, true).split("\n");

        List<String> sorted_entities = new LinkedList<>();
        for (String line : lines) {
            String[] tmp = line.split("\t");
            String qid = tmp[0];
            String entity = tmp[1];

            if (!qid_filter.equals(qid)) {
                continue;
            }

            //check if the cluster has reached its size limit
            if (sorted_entities.size() <= entity_size) {
                sorted_entities.add(entity);
            }
        }

        return sorted_entities;
    }

    /**
     * Load the entity URIs from the index.
     *
     * @param entity_index
     * @param entities
     * @param is_spectral
     * @return
     * @throws IOException
     */
    private static Map<String, Map<Integer, String>> loadEntityURIs(String entity_index, Map<String, Map<Integer, Set<Integer>>> entities, boolean is_spectral) throws IOException {
        Map<String, Map<Integer, String>> rst = new HashMap<>();
        String tag = is_spectral ? "spectral/" : "xmeans/";

        StringBuffer sb = new StringBuffer();
        for (String cluster_file : entities.keySet()) {
            int start_index = cluster_file.indexOf(tag) + tag.length();
            String type = cluster_file.substring(start_index, cluster_file.indexOf("/", start_index));
            Map<Integer, String> sub_rst = new HashMap<>();
            rst.put(type, sub_rst);

            for (int cluster_id : entities.get(cluster_file).keySet()) {
                for (int entity_id : entities.get(cluster_file).get(cluster_id)) {
                    sub_rst.put(entity_id, "");
                    sb.append(type).append("\t").append(entity_id).append("\n");
                }
            }
        }
        FileUtils.saveText(sb.toString(), "cluster_entity_debug.log");

        //load the entity URIs
        BufferedReader reader = FileUtils.getCompressedFileReader(entity_index);
        while (reader.ready()) {
            String line = reader.readLine();
            String[] tmp = line.split("\t");

            if (tmp.length != 3) {
                continue;
            }

            int entity_id = Integer.valueOf(tmp[1]);
            if (!rst.containsKey(tmp[0]) || !rst.get(tmp[0]).containsKey(entity_id)) {
                continue;
            }

            rst.get(tmp[0]).put(entity_id, tmp[2]);
        }
        reader.close();
        return rst;
    }

    /**
     * Load entity profiles from the index.
     *
     * @param data
     * @param btc_index
     * @param rdf3xengine
     * @throws IOException
     * @throws InterruptedException
     */
    private static void loadEntityProfiles(Map<String, Map<Integer, Set<Integer>>> data,
                                           String btc_index, String rdf3xengine, String out_dir,
                                           boolean is_spectral, String entity_index) throws IOException, InterruptedException {
        Map<String, Map<Integer, String>> entity_uris = loadEntityURIs(entity_index, data, is_spectral);

        String tag = is_spectral ? "spectral" : "xmeans";
        for (String file : data.keySet()) {
            String tmp_tag = tag + "/";
            int start_index = file.indexOf(tmp_tag) + tmp_tag.length();
            String type = file.substring(start_index, file.indexOf("/", start_index));
            System.out.printf("Loading data for file %s\n", file);

            //get entity uris
            Map<Integer, String> sub_entity_uri = entity_uris.get(type);
            StringBuffer sb = new StringBuffer();
            StringBuffer sb_txt = new StringBuffer();
            for (int cluster_id : data.get(file).keySet()) {
                Set<Integer> entities = data.get(file).get(cluster_id);
                for (int entity_id : entities) {
                    String entity = sub_entity_uri.get(entity_id);
                    Map<String, Set<String>> triples = Utils.loadRDF3XEntityTriples(entity, btc_index, rdf3xengine);

                    if (triples == null || triples.isEmpty()) {
                        continue;
                    }

                    //output the values.
                    Set<String> added_str = new HashSet<>();
                    StringBuffer tmp = new StringBuffer();
                    for (String predicate : triples.keySet()) {
                        for (String object : triples.get(predicate)) {
                            sb.append(file).append("\t").append(entity).append("\t").append(predicate).append("\t").append(object).append("\t").append(cluster_id).append("\n");
                            if (added_str.contains(object) || object.startsWith("<")) {
                                continue;
                            }
                            tmp.append(object).append("\n");
                            added_str.add(object);
                        }
                    }

                    sb_txt.append(file).append("\t").append(entity).append("\t").append(tmp.toString().replaceAll("\n{1,}", " ").replaceAll("\t", " ").trim()).append("\t").append(cluster_id).append("\n");
                }
            }

            FileUtils.saveText(sb.toString(), out_dir + "/" + tag + "_cluster_eval_triples.csv", true, false);
            FileUtils.saveText(sb_txt.toString(), out_dir + "/" + tag + "_cluster_eval_text.csv", true, false);
            System.out.printf("Finished cluster for file %s\n", file);
        }
    }

    private static Map<String, Map<Integer, Set<Integer>>> loadEntityEvalClusters(Set<String> cluster_files,
                                                                                  int entity_size,
                                                                                  boolean is_spectral) throws IOException {
        Map<String, Map<Integer, Set<Integer>>> rst = new HashMap<>();

        cluster_files.parallelStream().forEach(cluster_file -> {
            System.out.printf("Started processing file %s with %d clusters and %d entities\n", cluster_file, entity_size, entity_size);
            //load first the entity clusters
            Map<Integer, Set<Integer>> entity_cluster = null;
            if (!is_spectral) {
                entity_cluster = FileUtils.readIntMapSet(cluster_file, "\t");
            } else {
                entity_cluster = FileUtils.readIntMapNestedSet(cluster_file, "\t");
            }

            //cluster id
            Integer[] cluster_arr = new Integer[entity_cluster.size()];
            entity_cluster.keySet().toArray(cluster_arr);

            Random rand = new Random();
            Map<Integer, Set<Integer>> sampled_clusters = new HashMap<>();
            while (sampled_clusters.size() < entity_size) {
                int cluster_index = rand.nextInt(cluster_arr.length);
                if (sampled_clusters.containsKey(cluster_arr[cluster_index]) || entity_cluster.get(cluster_index).size() < 10) {
                    continue;
                }
                sampled_clusters.put(cluster_arr[cluster_index], new HashSet<>());
            }

            //sample for entity indices
            for (int cluster_id : sampled_clusters.keySet()) {
                Integer[] entity_indice_array = new Integer[entity_cluster.get(cluster_id).size()];
                entity_cluster.get(cluster_id).toArray(entity_indice_array);
                Set<Integer> sampled_entities = sampled_clusters.get(cluster_id);
                while (sampled_entities.size() < entity_size) {
                    int entity_index = rand.nextInt(entity_indice_array.length);
                    int entity_id = entity_indice_array[entity_index];

                    sampled_entities.add(entity_id);
                }
            }
            rst.put(cluster_file, sampled_clusters);
        });
        return rst;
    }
}
