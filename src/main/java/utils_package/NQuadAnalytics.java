package utils_package;

import cern.colt.matrix.DoubleMatrix2D;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import ranking.CosineSimilarity;
import ranking.JaccardSimilarity;
import ranking.LanguageModels;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by besnik on 23/09/2014.
 */
public class NQuadAnalytics {
    public static void main(String[] args) throws ParseException, IOException {
        String operation = "", out_dir = "", data_dir = "", stop_words = "",
                comparisons = "", entity_pairs_path = "", literal_types_path = "",
                sim_metric = "", btc_types = "", graph_expr_props = "";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-operation")) {
                operation = args[i + 1];
                i++;
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[i + 1];
                i++;
            } else if (args[i].equals("-data_dir")) {
                data_dir = args[i + 1];
                i++;
            } else if (args[i].equals("-stop_words")) {
                stop_words = args[i + 1];
                i++;
            } else if (args[i].equals("-comparisons")) {
                comparisons = args[i + 1];
                i++;
            } else if (args[i].equals("-entity_pairs")) {
                entity_pairs_path = args[i + 1];
                i++;
            } else if (args[i].equals("-literal_types")) {
                literal_types_path = args[i + 1];
                i++;
            } else if (args[i].equals("-sim_metric")) {
                sim_metric = args[i + 1];
                i++;
            } else if (args[i].equals("-btc_types")) {
                btc_types = args[i + 1];
                i++;
            } else if (args[i].equals("-graph_expr_props")) {
                graph_expr_props = args[i + 1];
                i++;
            }
        }

        NQuadAnalytics nqa = new NQuadAnalytics();
        if (operation.equals("similarity")) {
            Set<String> entity_textual_datatypes = FileUtils.readIntoSet(literal_types_path, "\n", false);
            List<String[]> entity_pairs = nqa.loadEntityPairs(entity_pairs_path);

            //add randomly k pairs for comparison
            Set<String[]> random_entity_pairs_sample = new HashSet<>();
            Random rand = new Random();
            while (random_entity_pairs_sample.size() < Integer.valueOf(comparisons) && random_entity_pairs_sample.size() != entity_pairs.size()) {
                int rand_indice = rand.nextInt(entity_pairs.size() - 1);
                random_entity_pairs_sample.add(entity_pairs.get(rand_indice));
            }

            Map<String, String> entity_profiles = nqa.loadEntities(data_dir, random_entity_pairs_sample, entity_textual_datatypes);

            StringBuffer sb = new StringBuffer();
            if (sim_metric.equals("lm")) {
                System.out.println("Computing the language models between entities.");
                Set<String> stop_words_set = FileUtils.readIntoSet(stop_words, "\n", false);
                LanguageModels lm = new LanguageModels(stop_words_set);

                //add the documents through the pipe
                entity_profiles.keySet().forEach(entity -> {
                    lm.addDocument(entity, entity_profiles.get(entity));
                });

                //measure the similarity
                for (String[] entity_pair : entity_pairs) {
                    String entity_a = entity_pair[0];
                    String entity_b = entity_pair[1];

                    if (!entity_profiles.containsKey(entity_a) || entity_profiles.containsKey(entity_b)) {
                        continue;
                    }

                    double lm_sim = lm.getCrossEnthropy(entity_a, entity_b);
                    sb.append(entity_a).append("\t").append(entity_b).append("\t").append(lm_sim).append("\n");
                }
            } else if (sim_metric.equals("lexical_sim")) {
                System.out.println("Computing the similarity between entities.");
                CosineSimilarity cosine = new CosineSimilarity(entity_profiles, stop_words);
                DoubleMatrix2D tfidf_matrix = cosine.computeTFIDFDcoumentScores(entity_profiles.size());
                System.out.println("Finished computing the tfxidf vectors.");
                //compute the pairwise document similarity
                System.out.println("Computing the pairwise cosine similarity between entities.");
                Map<String, Map<String, Map<String, Double>>> similarities = nqa.getEntitySimilarity(entity_profiles, random_entity_pairs_sample, cosine, tfidf_matrix);
                String json_sim = nqa.getCSVRepresentation(similarities);
                sb.append(json_sim);
            }
            FileUtils.saveText(sb.toString(), out_dir + "/" + sim_metric + "_btc12_similarities.csv");

        } else if (operation.equals("resource_types")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);
            nqa.extractResourceTypeAssociation(files, out_dir);
        } else if (operation.equals("co_occurrence_context")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);

            String results = nqa.computeContextualizedTypeCoOccurrence(files);
            FileUtils.saveText(results, out_dir + "/context_type_co_occurrence.csv");
        } else if (operation.equals("type_extraction")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);
            nqa.extractTypes(files, out_dir);
        } else if (operation.equals("co_occurrence")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);
            nqa.extractCoOcurringTypes(files, btc_types, out_dir);
        } else if (operation.equals("type_hist")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);

            Map<String, Integer> type_hist = nqa.computeTypeHistograms(files);
            StringBuffer sb = new StringBuffer();
            for (String type : type_hist.keySet()) {
                sb.append(type).append("\t").append(type_hist.get(type)).append("\n");
            }
            FileUtils.saveText(sb.toString(), out_dir + "/type_histogram.txt");
        } else if (operation.equals("predicate_hist")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);

            Map<String, Integer> type_hist = nqa.computePredicateHistograms(files);
            StringBuffer sb = new StringBuffer();
            for (String type : type_hist.keySet()) {
                sb.append(type).append("\t").append(type_hist.get(type)).append("\n");
            }
            FileUtils.saveText(sb.toString(), out_dir + "/predicate_histogram.txt");
        } else if (operation.equals("dataset_structure")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);
            Set<String> finished_files = new HashSet<>();
            if (FileUtils.fileExists(out_dir + "/finished_files.csv", true)) {
                finished_files = FileUtils.readIntoSet(out_dir + "/finished_files.csv", "\n", false);
            }
            files.removeAll(finished_files);
            nqa.measureGraphExpressitivity(files, out_dir);
        } else if (operation.equals("graph_expressitivity_dist")) {
            //read the graph attributes line by line.
            BufferedReader reader = FileUtils.getFileReader(data_dir);
            Set<String> expr_props = FileUtils.readIntoSet(graph_expr_props, "\n", false);
            System.out.println("Properties to analyze: " + expr_props.toString());

            String current_graph = "";
            Map<String, Map<String, Integer>> graph_props = new HashMap<>();
            Map<String, Integer> prop_counts = new HashMap<>();
            while (reader.ready()) {
                String[] line = reader.readLine().split("\t");

                if (!expr_props.contains(line[1])) {
                    continue;
                }
                String graph_name = line[0];
                if (graph_name.contains("/")) {
                    graph_name = graph_name.substring(0, graph_name.lastIndexOf("/"));
                }
                //we reached a new graph URI.
                if (!current_graph.isEmpty() && current_graph.equals(graph_name)) {
                    if (graph_props.containsKey(current_graph)) {
                        //add the values
                        Map<String, Integer> tmp = graph_props.get(current_graph);
                        tmp.keySet().forEach(prop -> {
                            Integer val = prop_counts.get(prop);
                            val = val == null ? 0 : val;
                            val += tmp.get(prop);
                            tmp.put(prop, val);
                        });
                        graph_props.put(current_graph, tmp);
                        prop_counts.clear();
                    } else {
                        Map<String, Integer> tmp = new HashMap<>();
                        prop_counts.keySet().forEach(prop -> tmp.put(prop, prop_counts.get(prop)));
                        graph_props.put(current_graph, tmp);
                    }
                }

                Integer count = prop_counts.get(line[1]);
                count = count == null ? 0 : count;
                count += Integer.valueOf(line[2]);
                prop_counts.put(line[1], count);

                current_graph = graph_name;
            }
            reader.close();

            StringBuffer sb = new StringBuffer();
            graph_props.keySet().forEach(graph -> graph_props.get(graph).keySet().forEach(prop ->
                    sb.append(graph).append("\t").
                            append(prop).append("\t").
                            append(graph_props.get(graph).get(prop)).append("\n")));
            FileUtils.saveText(sb.toString(), out_dir + "/graph_expr_pred_counts.csv");
        } else if (operation.equals("utils_package")) {
            BufferedReader reader = FileUtils.getFileReader(data_dir);
            Map<String, Map<String, Integer>> rst = new HashMap<>();
            while (reader.ready()) {
                String line = reader.readLine();
                String[] tmp = line.split("\t");
                String graph = tmp[0];
                if (graph.contains("/")) {
                    graph = graph.substring(0, graph.lastIndexOf("/"));
                }

                Map<String, Integer> sub_rst = rst.get(graph);
                sub_rst = sub_rst == null ? new HashMap<>() : sub_rst;
                rst.put(graph, sub_rst);

                Integer count = sub_rst.get(tmp[1]);
                count = count == null ? 0 : count;
                count += Integer.valueOf(tmp[2]);
                sub_rst.put(tmp[1], count);
            }
            StringBuffer sb = new StringBuffer();
            for (String graph : rst.keySet()) {
                for (String prop : rst.get(graph).keySet()) {
                    sb.append(graph).append("\t").
                            append(prop).append("\t").
                            append(rst.get(graph).get(prop)).append("\n");

                    if (sb.length() > 10000) {
                        FileUtils.saveText(sb.toString(), out_dir + "/graph_expr_all_pred_counts.csv", true);
                        sb.delete(0, sb.length());
                    }
                }
            }
            FileUtils.saveText(sb.toString(), out_dir + "/graph_expr_all_pred_counts.csv", true);
        } else if (operation.equals("graph_types")) {
            Set<String> files = new HashSet<>();
            FileUtils.getFilesList(data_dir, files);

            Set<String> finished_files = new HashSet<>();
            if (FileUtils.fileExists(out_dir + "/finished_files.csv", true)) {
                finished_files = FileUtils.readIntoSet(out_dir + "/finished_files.csv", "\n", false);
            }
            files.removeAll(finished_files);

            nqa.getGraphDataTypes(files, out_dir);
        }
    }

    /**
     * Loads the distinct resource types for all graphs in the BTC dataset.
     *
     * @param files
     * @param out_dir
     */
    public void getGraphDataTypes(Set<String> files, String out_dir) {
        files.parallelStream().forEach(data_path -> {
            try {
                BufferedReader reader = FileUtils.getCompressedFileReader(data_path);
                Map<String, Set<String>> graph_types = new HashMap<>();
                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);
                    if (!triple[1].toString().equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        continue;
                    }
                    String graph = triple[3].toString();
                    if (graph.contains("/")) {
                        graph = graph.substring(0, graph.lastIndexOf("/"));
                    }

                    Set<String> types = graph_types.get(graph);
                    types = types == null ? new HashSet<String>() : types;
                    graph_types.put(graph, types);
                    types.add(triple[2].toString());
                }
                reader.close();
                System.out.println("Finished processing file: " + data_path);
                StringBuffer sb = new StringBuffer();
                graph_types.keySet().forEach(graph -> sb.append(graph).append("\t").append(graph_types.get(graph)).append("\n"));
                FileUtils.saveText(sb.toString(), out_dir + "/graph_types.csv", true);
                FileUtils.saveText(data_path + "\n", out_dir + "/finished_files.csv", true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Measures the distribution of specific types in BTC.
     *
     * @param files
     * @return
     */

    public void measureGraphExpressitivity(Set<String> files, String out_dir) {
        files.parallelStream().forEach(data_path -> {
            try {
                Map<String, Map<String, Integer>> graph_expr = new HashMap<>();
                BufferedReader reader = FileUtils.getCompressedFileReader(data_path);

                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);
                    if (triple.length != 4) {
                        continue;
                    }
                    String graph = triple[3].toString();
                    String pred = triple[1].toString();

                    Map<String, Integer> sub_graph_expr = graph_expr.get(graph);
                    sub_graph_expr = sub_graph_expr == null ? new HashMap<>() : sub_graph_expr;
                    graph_expr.put(graph, sub_graph_expr);

                    Integer count = sub_graph_expr.get(pred);
                    count = count == null ? 0 : count;
                    count += 1;
                    sub_graph_expr.put(pred, count);
                }
                reader.close();
                System.out.println("Finished processing file: " + data_path);
                StringBuffer sb = new StringBuffer();
                int line_counter = 0;
                for (String graph : graph_expr.keySet()) {
                    for (String pred : graph_expr.get(graph).keySet()) {
                        sb.append(graph).append("\t").append(pred).append("\t").append(graph_expr.get(graph).get(pred)).append("\n");
                        line_counter++;

                        if (line_counter != 0 && line_counter % 10000 == 0) {
                            FileUtils.saveText(sb.toString(), out_dir + "/graph_expressitivity.txt", true);
                            sb.delete(0, sb.length());
                        }
                    }
                }
                FileUtils.saveText(sb.toString(), out_dir + "/graph_expressitivity.txt", true);
                FileUtils.saveText(data_path + "\n", out_dir + "/finished_files.csv", true);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    /**
     * Measures the distribution of specific types in BTC.
     *
     * @param files
     * @return
     */
    public Map<String, Integer> computeTypeHistograms(Set<String> files) {
        Map<String, Integer> type_hist = new HashMap<>();
        files.parallelStream().forEach(data_path -> {
            try {
                InputStream fileStream = new FileInputStream(data_path);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader reader = new BufferedReader(decoder);

                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);

                    if (!triple[1].toN3().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                        continue;
                    }

                    String type = triple[2].toN3();
                    Integer count = type_hist.get(type);
                    count = count == null ? 0 : count;
                    count += 1;
                    type_hist.put(type, count);
                }
                reader.close();
                System.out.println("Finished processing file: " + data_path);
            } catch (Exception e) {
                System.out.printf("Error processing file: %s with error message: %s.\n", data_path, e.getMessage());
            }
        });
        return type_hist;
    }


    /**
     * Measures the distribution of the frequency of the used predicates in the BTC12 dataset.
     *
     * @param files
     * @return
     */
    public Map<String, Integer> computePredicateHistograms(Set<String> files) {
        Map<String, Integer> predicate_hist = new HashMap<>();
        files.parallelStream().forEach(data_path -> {
            try {
                InputStream fileStream = new FileInputStream(data_path);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader reader = new BufferedReader(decoder);

                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);

                    String predicate = triple[1].toN3();
                    Integer count = predicate_hist.get(predicate);
                    count = count == null ? 0 : count;
                    count += 1;
                    predicate_hist.put(predicate, count);
                }
                reader.close();
                System.out.println("Finished processing file: " + data_path);
            } catch (Exception e) {
                System.out.printf("Error processing file: %s with error message: %s.\n", data_path, e.getMessage());
            }
        });
        return predicate_hist;
    }

    /**
     * Loads the entity pairs which we use to measure the relatedness between entity pairs.
     *
     * @param datatype_path
     * @return
     * @throws IOException
     */
    public List<String[]> loadEntityPairs(String datatype_path) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(datatype_path)));

        List<String[]> entity_pairs = new ArrayList<>();
        while (reader.ready()) {
            String line = reader.readLine();
            String[] entity_pair = line.split("\\s{1,}");
            if (entity_pair.length == 2) {
                entity_pairs.add(entity_pair);
            }
        }
        return entity_pairs;
    }

    private String getCSVRepresentation(Map<String, Map<String, Map<String, Double>>> sim) {
        StringBuffer sb = new StringBuffer();

        for (String entity_a : sim.keySet()) {
            for (String entity_b : sim.get(entity_a).keySet()) {
                for (String metric : sim.get(entity_a).get(entity_b).keySet()) {
                    double value = sim.get(entity_a).get(entity_b).get(metric);
                    sb.append(entity_a).append("\t").append(entity_b).append("\t").append(metric).append("\t").append(value).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Measure the lexical similarities of entities.
     *
     * @param entity_profiles
     * @param entity_pairs
     * @return
     */
    private Map<String, Map<String, Map<String, Double>>> getEntitySimilarity(Map<String, String> entity_profiles,
                                                                              Set<String[]> entity_pairs,
                                                                              CosineSimilarity cosine,
                                                                              DoubleMatrix2D tfidf_matrix) {
        Map<String, Map<String, Map<String, Double>>> entity_similarities = new HashMap<>();

        for (String[] entity_pair : entity_pairs) {
            String entity_a = entity_pair[0];
            String entity_b = entity_pair[1];

            //measure the pairwise similarity

            Map<String, Map<String, Double>> sub_sim_a = entity_similarities.get(entity_a);
            sub_sim_a = sub_sim_a == null ? new HashMap<String, Map<String, Double>>() : sub_sim_a;
            entity_similarities.put(entity_a, sub_sim_a);

            Map<String, Double> sub_sim_b = sub_sim_a.get(entity_b);
            sub_sim_b = sub_sim_b == null ? new HashMap<String, Double>() : sub_sim_b;
            sub_sim_a.put(entity_b, sub_sim_b);

            double sim = cosine.getDocumentSimilarity(entity_a, entity_b, tfidf_matrix);
            double jaccard_sim = 0;
            String entity_profile_a = entity_profiles.get(entity_a);
            String entity_profile_b = entity_profiles.get(entity_b);
            if (entity_profile_a == null || entity_profile_b == null) {
                jaccard_sim = 0;
            } else {
                JaccardSimilarity jaccard = new JaccardSimilarity(entity_profile_a, entity_profile_b);
                jaccard_sim = jaccard.computeJaccardSimilarity();
            }

            sub_sim_b.put("cosine", sim);
            sub_sim_b.put("jaccard", jaccard_sim);
        }

        return entity_similarities;
    }


    /**
     * Loads the entities profile consisting of their textual description.
     *
     * @param data_dir
     * @return
     * @throws ParseException
     */
    private Map<String, String> loadEntities(String data_dir, Set<String[]> entity_pairs,
                                             Set<String> entity_textual_datatypes)
            throws ParseException, IOException {

        //convert the pairs into a flat set
        Set<String> valid_entities = new HashSet<>();
        for (String[] pair : entity_pairs) {
            valid_entities.add(pair[0]);
            valid_entities.add(pair[1]);
        }

        //store the entity profiles into a map datastructure
        Map<String, StringBuffer> entity_profiles = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(data_dir)));

        while (reader.ready()) {
            String line = reader.readLine();
            Node[] triple = NxParser.parseNodes(line);

            if (valid_entities.contains(triple[0].toN3())) {
                if (entity_textual_datatypes.contains(triple[1].toN3())) {
                    String entity_text = triple[2].toString();
                    StringBuffer old_text = entity_profiles.get(triple[0].toN3());
                    old_text = old_text == null ? new StringBuffer() : old_text;
                    old_text.append(entity_text).append("\n");
                    entity_profiles.put(triple[0].toN3(), old_text);
                } else {
                    StringBuffer old_text = entity_profiles.get(triple[0].toN3());
                    old_text = old_text == null ? new StringBuffer() : old_text;
                    entity_profiles.put(triple[0].toN3(), old_text);
                }
            }

            if (entity_profiles.size() == valid_entities.size()) {
                break;
            }
        }

        Map<String, String> results = new HashMap<>();
        entity_profiles.keySet().forEach(entity -> {
            results.put(entity, entity_profiles.get(entity).toString());
        });

        return results;
    }

    /**
     * Loads the associations of resource types and the particular instances.
     *
     * @param type_resources_dir
     * @return
     */
    private Map<String, Set<Integer>> loadResourceTypeAssociations(String type_resources_dir) {
        Set<String> type_resources_files = new HashSet<>();
        FileUtils.getFilesList(type_resources_dir, type_resources_files);
        Map<String, Set<Integer>> type_resources = new HashMap<>();
        for (String type_resource_file : type_resources_files) {
            List<String> tmp_lst = FileUtils.readLargeText(type_resource_file);
            for (String tmp_line : tmp_lst) {
                String[] tmp = tmp_line.split("\n");
                for (String line : tmp) {
                    String[] tmp_type_res = line.split("\t");
                    Set<Integer> resources = type_resources.get(tmp_type_res[0]);
                    resources = resources == null ? new HashSet<>() : resources;
                    type_resources.put(tmp_type_res[0], resources);

                    //add the instances
                    String res_str = tmp_type_res[1];
                    res_str = res_str.substring(1, res_str.length() - 1);
                    String[] res_instances = res_str.trim().split(", ");
                    for (String s : res_instances) {
                        resources.add(Integer.valueOf(s));
                    }
                }
            }
            System.out.printf("Finished loading type-resource associations for file: %s/\n", type_resource_file);
        }

        return type_resources;
    }

    /**
     * Extracts the type of a resource.
     *
     * @param resource
     * @param type_resources
     * @return
     */
    private String getResourceType(String resource, Map<String, Set<Integer>> type_resources) {
        int res_hashcode = resource.hashCode();
        for (String type : type_resources.keySet()) {
            if (type_resources.get(type).contains(res_hashcode))
                return type;
        }
        return null;
    }

    /**
     * Stores the associations of the different resource types.
     *
     * @param files
     * @param type_resources_dir
     * @param out_dir
     */
    public void extractCoOcurringTypes(Set<String> files, String type_resources_dir, String out_dir) {
        Map<String, Set<Integer>> type_resources = loadResourceTypeAssociations(type_resources_dir);

        files.parallelStream().forEach(data_path -> {
            try {
                int start_index = data_path.indexOf("btc-2012/") + "btc-2012/".length() + 1;
                int end_index = data_path.indexOf(".", start_index);
                String file_name = data_path.substring(start_index, end_index);
                file_name = file_name.replaceAll("/", "_");

                InputStream fileStream = new FileInputStream(data_path);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader reader = new BufferedReader(decoder);

                Map<String, Map<String, Integer>> type_co_occurrence = new HashMap<>();
                Map<String, Map<String, Map<String, Integer>>> type_co_occurrence_datatype = new HashMap<>();

                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);

                    if (triple.length < 3 || triple[1].toN3().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>") ||
                            triple[2].toN3().startsWith("\"")) {
                        continue;
                    }

                    //check first if it contains the particular resource in the type-resource mappings.
                    String type_a = getResourceType(triple[1].toN3(), type_resources);
                    String type_b = getResourceType(triple[2].toN3(), type_resources);

                    if (type_a == null || type_b == null) {
                        continue;
                    }
                    Map<String, Integer> sub_type_co_occurrence = type_co_occurrence.get(type_a);
                    sub_type_co_occurrence = sub_type_co_occurrence == null ? new HashMap<String, Integer>() : sub_type_co_occurrence;
                    type_co_occurrence.put(type_a, sub_type_co_occurrence);

                    Integer count = sub_type_co_occurrence.get(type_b);
                    count = count == null ? 0 : count;
                    count += 1;
                    sub_type_co_occurrence.put(type_b, count);

                    Map<String, Map<String, Integer>> sub_type_co_occurrence_datatype = type_co_occurrence_datatype.get(type_a);
                    sub_type_co_occurrence_datatype = sub_type_co_occurrence_datatype == null ? new HashMap<>() : sub_type_co_occurrence_datatype;
                    type_co_occurrence_datatype.put(type_a, sub_type_co_occurrence_datatype);

                    Map<String, Integer> datatype_counts = sub_type_co_occurrence_datatype.get(type_b);
                    datatype_counts = datatype_counts == null ? new HashMap<>() : datatype_counts;
                    sub_type_co_occurrence_datatype.put(type_b, datatype_counts);

                    Integer datatype_count = datatype_counts.get(triple[1].toN3());
                    datatype_count = datatype_count == null ? 0 : datatype_count;
                    datatype_count += 1;
                    datatype_counts.put(triple[1].toN3(), datatype_count);
                }
                reader.close();

                //write the results about type co-occurrence and the corresponding datatypes that interlink two types
                StringBuffer sb = new StringBuffer();
                for (String type_a : type_co_occurrence.keySet()) {
                    for (String type_b : type_co_occurrence.get(type_a).keySet()) {
                        int count = type_co_occurrence.get(type_a).get(type_b);
                        Map<String, Integer> datatypes = type_co_occurrence_datatype.get(type_a).get(type_b);

                        sb.append(type_a).append("\t").append(type_b).append("\t").append(count).append("\t").append(datatypes.toString()).append("\n");
                    }
                }
                FileUtils.saveText(sb.toString(), out_dir + "/" + file_name + "_co_occurrence.csv");
                System.out.printf("Finished processing the type co-occurrence for file: %s.\n", file_name);
            } catch (Exception e) {
                System.out.printf("Error processing file: %s with error message: %s.\n", data_path, e.getMessage());
            }
        });
    }

    /**
     * Computes the co-occurrence of resource types.
     *
     * @param files
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public void extractResourceTypeAssociation(Set<String> files, String out_dir) {
        files.parallelStream().forEach(data_path -> {
            try {
                int start_index = data_path.indexOf("btc-2012/") + "btc-2012/".length() + 1;
                int end_index = data_path.indexOf(".", start_index);
                String file_name = data_path.substring(start_index, end_index);
                file_name = file_name.replaceAll("/", "_");

                if (FileUtils.fileExists(out_dir + "/" + file_name + "_res_types.csv", true)) {
                    return;
                }

                InputStream fileStream = new FileInputStream(data_path);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader reader = new BufferedReader(decoder);

                Map<String, Set<Integer>> type_resources = new HashMap<>();
                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);

                    if (triple.length < 3 || !triple[1].toN3().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                        continue;
                    }

                    //add all the instances for a given resource type
                    Set<Integer> resources = type_resources.get(triple[2].toN3());
                    resources = resources == null ? new HashSet<>() : resources;
                    type_resources.put(triple[2].toN3(), resources);
                    resources.add(triple[0].toN3().hashCode());
                }
                reader.close();

                StringBuffer sb = new StringBuffer();
                for (String type : type_resources.keySet()) {
                    sb.append(type).append("\t").append(type_resources.get(type)).append("\n");

                    if (sb.length() != 0 && sb.length() % 10000 == 0) {
                        //write the results for each file
                        FileUtils.saveText(sb.toString(), out_dir + "/" + file_name + "_res_types.csv", true);
                        sb.delete(0, sb.length());
                    }
                }
                //write the results for each file
                FileUtils.saveText(sb.toString(), out_dir + "/" + file_name + "_res_types.csv", true);
                System.out.printf("Finished processing file: %s\n", file_name);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });
    }

    /**
     * Computes the co-occurrence of resource types.
     *
     * @param files
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public void extractTypes(Set<String> files, String out_dir) throws IOException, ParseException {
        Set<String> types = new HashSet<>();
        for (String data_path : files) {
            InputStream fileStream = new FileInputStream(data_path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
            BufferedReader reader = new BufferedReader(decoder);

            while (reader.ready()) {
                String line = reader.readLine();
                Node[] triple = NxParser.parseNodes(line);

                if (triple.length < 3) {
                    continue;
                }

                String type = triple[2].toN3();
                if (triple[1].toN3().equals("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>")) {
                    types.add(type);
                }
            }

            System.out.println("Finished processing file: " + data_path);
        }

        StringBuffer sb = new StringBuffer();
        for (String type : types) {
            sb.append(type).append("\n");
        }
        FileUtils.saveText(sb.toString(), out_dir + "/btc_types.csv");
    }

    /**
     * Computes the co-occurrence of resource types.
     *
     * @param files
     * @return
     * @throws IOException
     * @throws ParseException
     */
    public String computeContextualizedTypeCoOccurrence(Set<String> files) throws IOException, ParseException {
        Map<String, Map<String, Set<Integer>>> type_occurrence = new HashMap<>();
        for (String data_path : files) {
            InputStream fileStream = new FileInputStream(data_path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
            BufferedReader reader = new BufferedReader(decoder);

            while (reader.ready()) {
                String line = reader.readLine();
                Node[] triple = NxParser.parseNodes(line);

                if (triple.length == 4) {
                    String type = triple[2].toN3();
                    if (type.startsWith("<")) {
                        String graph = triple[3].toN3();
                        Map<String, Set<Integer>> graph_occurrence = type_occurrence.get(graph);
                        graph_occurrence = graph_occurrence == null ? new HashMap<>() : graph_occurrence;
                        type_occurrence.put(graph, graph_occurrence);

                        Set<Integer> sub_type_occurrence = graph_occurrence.get(type);
                        sub_type_occurrence = sub_type_occurrence == null ? new HashSet<>() : sub_type_occurrence;
                        graph_occurrence.put(type, sub_type_occurrence);

                        sub_type_occurrence.add(triple[0].toN3().hashCode());


                    }
                }
            }

            System.out.println("Finished processing file: " + data_path);
        }


        //from the type occurrence measure their overlap based on a pairwise comparison
        Map<String, Map<String, Map<String, Integer>>> type_co_occurrence = new HashMap<>();

        for (String graph : type_occurrence.keySet()) {
            Map<String, Map<String, Integer>> graph_co_occurrence = new HashMap<>();
            type_co_occurrence.put(graph, graph_co_occurrence);

            int i = 0;
            for (String type_a : type_occurrence.get(graph).keySet()) {
                Map<String, Integer> sub_types = new HashMap<>();
                graph_co_occurrence.put(type_a, sub_types);

                int j = 0;
                for (String type_b : type_occurrence.get(graph).keySet()) {
                    if (j <= i) {
                        j++;
                        continue;
                    }

                    Set<Integer> type_a_occurrence = type_occurrence.get(graph).get(type_a);
                    Set<Integer> type_b_occurrence = type_occurrence.get(graph).get(type_b);

                    Set<Integer> common = new HashSet<>(type_a_occurrence);
                    common.retainAll(type_b_occurrence);
                    int co_occurrence = common.size();

                    sub_types.put(type_b, co_occurrence);
                    j++;
                }
                i++;
            }
        }


        //print the results
        StringBuffer sb = new StringBuffer();
        for (String graph : type_co_occurrence.keySet()) {
            for (String type_a : type_co_occurrence.get(graph).keySet()) {
                for (String type_b : type_co_occurrence.get(graph).get(type_a).keySet()) {
                    sb.append(graph).append("\t").append(type_a).append("\t").append(type_b).append("\t").append(type_co_occurrence.get(type_a).get(type_b)).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
