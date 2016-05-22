package query;

import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import utils_package.FileUtils;
import utils_package.Utils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Created by besnik on 1/14/15.
 */
public class HybridModelEM {
    public static void main(String[] args) throws IOException, ParseException, InterruptedException, SQLException {
        String query_file = "", query_gt_file = "",
                btc_index_file = "", rdf3x_engine = "",
                lucene_index_file = "",
                out_dir = "", stop_words_path = "",
                operation = "", data = "";

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-query")) {
                query_file = args[++i];
            } else if (args[i].equals("-query_gt")) {
                query_gt_file = args[++i];
            } else if (args[i].equals("-btc")) {
                btc_index_file = args[++i];
            } else if (args[i].equals("-rdf3x")) {
                rdf3x_engine = args[++i];
            } else if (args[i].equals("-lucene")) {
                lucene_index_file = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-stop_words")) {
                stop_words_path = args[++i];
            } else if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-data")) {
                data = args[++i];
            }
        }

        if (operation.equals("construct_parameters")) {
            //load the results and estimate the parameters for the hybrid model using the EM algorithm.
            Map<String, String> queries = FileUtils.readIntoStringMap(query_file, "\t", false);

            //load the ground truth results
            Map<String, Set<Map.Entry<String, Integer>>> query_gt = loadGTEntities(query_gt_file);
            Map<String, Set<String>> query_baseline_results = query(queries, lucene_index_file);

            Set<String> stop_words = FileUtils.readIntoSet(stop_words_path, "\n", false);
            generateParamEMData(query_gt, query_baseline_results, queries, btc_index_file, rdf3x_engine, stop_words, out_dir);
        } else if (operation.equals("estimate_parameters")) {
            Map<String, Map<String, Map<String, double[]>>> params = loadParameterEstimationData(data);

            Map<Float, Map<String, Map<Integer, List<Map.Entry<Double, Double>>>>> est = new HashMap<>();
            for (int i = 0; i <= 10; i++) {
                float lambda = (float) i / 10.0f;

                Map<String, Map<Integer, List<Map.Entry<Double, Double>>>> l_est = new HashMap<>();
                est.put(lambda, l_est);

                for (String query : params.keySet()) {
                    Map<Integer, List<Map.Entry<Double, Double>>> q_l_est = new HashMap<>();
                    l_est.put(query, q_l_est);

                    //aggregate for each entities and the relevance score the parameter estimation data
                    for (String entity_a : params.get(query).keySet()) {
                        for (String entity_b : params.get(query).get(entity_a).keySet()) {
                            double[] scores = params.get(query).get(entity_a).get(entity_b);

                            double value_lv = lambda * scores[1] + (1 - lambda) * scores[0];
                            double value_jw = lambda * scores[2] + (1 - lambda) * scores[0];
                            int rel_score = (int) scores[3];

                            List<Map.Entry<Double, Double>> lst_scores = q_l_est.get(rel_score);
                            lst_scores = lst_scores == null ? new ArrayList<>() : lst_scores;
                            q_l_est.put(rel_score, lst_scores);

                            lst_scores.add(new AbstractMap.SimpleEntry<>(value_lv, value_jw));
                        }
                    }
                }
            }
            //write into the database the scores.
            writeParamsDB(est, out_dir + "/param_sql_data.csv");
        }
    }

    private static void writeParamsDB(Map<Float, Map<String, Map<Integer, List<Map.Entry<Double, Double>>>>> est, String out_file) throws SQLException {
        StringBuffer sb = new StringBuffer();

        for (double lambda : est.keySet()) {
            for (String query : est.get(lambda).keySet()) {
                for (int rel_score : est.get(lambda).get(query).keySet()) {
                    for (Map.Entry<Double, Double> value_entry : est.get(lambda).get(query).get(rel_score)) {
                        sb.append(lambda).append(",").append(rel_score).append(",").
                                append(query).append(",").append(value_entry.getKey()).append(",").
                                append(value_entry.getValue()).append("\n");
                    }
                }
            }
        }
        FileUtils.saveText(sb.toString(), out_file);
    }


    /**
     * Load parameter data for estimation.
     *
     * @param data
     * @return
     */
    private static Map<String, Map<String, Map<String, double[]>>> loadParameterEstimationData(String data) {
        String[] lines = FileUtils.readText(data).split("\n");
        Map<String, Map<String, Map<String, double[]>>> params = new HashMap<>();
        for (String line : lines) {
            String[] tmp = line.split("\t");
            String query = tmp[0];

            Map<String, Map<String, double[]>> q_params = params.get(query);
            q_params = q_params == null ? new HashMap<>() : q_params;
            params.put(query, q_params);

            String bs_entity = tmp[1];
            String gt_entity = tmp[2];

            Map<String, double[]> entity_qp = q_params.get(bs_entity);
            entity_qp = entity_qp == null ? new HashMap<>() : entity_qp;
            q_params.put(bs_entity, entity_qp);

            double rel = Double.valueOf(tmp[3]);

            double lv_bs = Double.valueOf(tmp[4]);
            double jw_bs = Double.valueOf(tmp[5]);

            double lv_gt = Double.valueOf(tmp[6]);
            double jw_gt = Double.valueOf(tmp[7]);

            int rel_assesment = Integer.valueOf(tmp[8]);

            double lv_sim = lv_gt / lv_bs;
            lv_sim = Double.isNaN(lv_sim) || Double.isInfinite(lv_sim) ? 0.0 : lv_sim;

            double jw_sim = jw_gt / jw_bs;
            jw_sim = Double.isNaN(jw_sim) || Double.isInfinite(jw_sim) ? 0.0 : jw_sim;

            double[] scores = new double[4];
            scores[0] = rel;
            scores[1] = lv_sim;
            scores[2] = jw_sim;
            scores[3] = rel_assesment;
            entity_qp.put(gt_entity, scores);
        }
        return params;
    }

    /**
     * Computes the different relatedness measures between entities retrieved by the baseline approach and the ground
     * truth entities. The data is used to estimate the parameters through the EM approach.
     *
     * @param query_gt
     * @param query_baseline_results
     */
    public static void generateParamEMData(Map<String, Set<Map.Entry<String, Integer>>> query_gt,
                                           Map<String, Set<String>> query_baseline_results,
                                           Map<String, String> queries, String btc_index,
                                           String rdf3x_engine, Set<String> stop_words, String out_dir) throws IOException, InterruptedException {
        Set<String> entities = new HashSet<>();
        query_baseline_results.keySet().stream().forEach(qid -> entities.addAll(query_baseline_results.get(qid)));
        query_gt.keySet().forEach(qid -> query_gt.get(qid).forEach(gt_entry -> entities.add(gt_entry.getKey())));

        System.out.printf("Loading entity profiles from the BTC index. Number of entities is %d\n", entities.size());
        Map<String, Map<String, Set<String>>> entity_profiles = loadEntityProfiles(btc_index, rdf3x_engine, entities);
        System.out.printf("Loaded entity profiles from the BTC index\n");

        //for every query estimate the parameters.
        String header = "Query\tBS_Entity\tGT_Entity\tEntity_REL\tLV_BS\tJW_BS\tLV_GT\tJW_GT\tREL\n";
        FileUtils.saveText(header, out_dir + "/parameter_estimation.csv");

        for (String qid : queries.keySet()) {
            String query_string = queries.get(qid);

            //ground-truth entities
            Set<Map.Entry<String, Integer>> gt_entities = query_gt.get(qid);
            //baseline entities (relevant)
            Set<String> bs_entities = query_baseline_results.get(qid);

            //compute the relatedness scores between ground truth and baseline entities.
            StringBuffer sb = new StringBuffer();
            for (Map.Entry<String, Integer> gt_entity_entry : gt_entities) {
                String gt_entity = gt_entity_entry.getKey();
                int rel_score = gt_entity_entry.getValue();

                for (String bs_entity : bs_entities) {
                    double[] rel_scores = getEntityRelatednessScore(bs_entity, gt_entity, query_string, entity_profiles, stop_words);
                    if (rel_scores == null) {
                        continue;
                    }
                    sb.append(query_string).append("\t").append(bs_entity).append("\t").
                            append(gt_entity).append("\t").append(rel_scores[0]).append("\t").
                            append(rel_scores[1]).append("\t").append(rel_scores[2]).append("\t").
                            append(rel_scores[3]).append("\t").append(rel_scores[4]).append("\t").
                            append(rel_score).append("\n");
                }
            }

            FileUtils.saveText(sb.toString(), out_dir + "/parameter_estimation.csv", true, false);
        }
    }

    /**
     * Computes the relatedness score between pairs of entities.
     *
     * @param entity_a
     * @param entity_b
     * @param query
     * @return
     */
    public static double[] getEntityRelatednessScore(String entity_a, String entity_b, String query, Map<String, Map<String, Set<String>>> entities, Set<String> stop_words) {
        double[] rel_score = new double[5];
        rel_score[0] = rel_score[1] = rel_score[2] = rel_score[3] = rel_score[4] = Double.MAX_VALUE;

        //entity profile
        Map<String, Set<String>> entity_profile_a = entities.get(entity_a);
        Map<String, Set<String>> entity_profile_b = entities.get(entity_b);

        //object values
        Set<String> objects_a = new HashSet<>();
        Set<String> objects_b = new HashSet<>();

        //load the entity profiles.
        StringBuffer txt_a = new StringBuffer();
        StringBuffer txt_b = new StringBuffer();
        if (entity_profile_a != null) {
            for (String predicate : entity_profile_a.keySet()) {
                for (String value : entity_profile_a.get(predicate)) {
                    if (!value.startsWith("<")) {
                        txt_a.append(value).append(" ");
                    } else {
                        objects_a.add(value);
                    }
                }
            }
        } else {
            return rel_score;
        }
        if (entity_profile_b != null) {
            for (String predicate : entity_profile_b.keySet()) {
                for (String value : entity_profile_b.get(predicate)) {
                    if (!value.startsWith("<")) {
                        txt_b.append(value).append(" ");
                    } else {
                        objects_b.add(value);
                    }
                }
            }
        } else {
            return rel_score;
        }

        //measure the entity relatedness through the Euclidean distance.
        THashMap<Integer, THashMap<String, Integer>> terms_dictionary = new THashMap<>();
        String[] terms_a = txt_a.toString().split(" ");
        THashMap<Integer, TIntIntHashMap> map_terms_a = new THashMap<>();
        THashMap<Integer, TIntIntHashMap> counts_a = new THashMap<>();
        String[] terms_b = txt_a.toString().split(" ");
        THashMap<Integer, TIntIntHashMap> map_terms_b = new THashMap<>();
        THashMap<Integer, TIntIntHashMap> counts_b = new THashMap<>();

        //add the different place holders for ngrams
        for (int i = 1; i <= 3; i++) {
            terms_dictionary.put(i, new THashMap<>());
            counts_a.put(i, new TIntIntHashMap());
            counts_b.put(i, new TIntIntHashMap());
        }

        int last_term_index = 0;
        for (int i = 0; i < 3; i++) {
            last_term_index = Utils.addNGrams(terms_a, map_terms_a, counts_a, last_term_index, terms_dictionary, stop_words, i + 1);
            last_term_index = Utils.addNGrams(terms_b, map_terms_b, counts_b, last_term_index, terms_dictionary, stop_words, i + 1);
        }

        //count the occurrence of different object values
        THashMap<String, Integer> objects_dictionary = new THashMap<>();
        THashMap<String, TIntHashSet> entity_objects = new THashMap<>();
        entity_objects.put(entity_a, new TIntHashSet());
        entity_objects.put(entity_b, new TIntHashSet());

        int last_object_index = 0;
        for (String object : objects_a) {
            if (!objects_dictionary.containsKey(object)) {
                last_object_index++;
                objects_dictionary.put(object, last_object_index);
            }

            entity_objects.get(entity_a).add(objects_dictionary.get(object));
        }
        for (String object : objects_b) {
            if (!objects_dictionary.containsKey(object)) {
                last_object_index++;
                objects_dictionary.put(object, last_object_index);
            }

            entity_objects.get(entity_b).add(objects_dictionary.get(object));
        }
        int[] feature_a = getEntityFeatureVector(terms_dictionary, entity_a, counts_a, entity_objects, objects_dictionary);
        int[] feature_b = getEntityFeatureVector(terms_dictionary, entity_b, counts_b, entity_objects, objects_dictionary);

        double entity_rel = 1.0;
        if (feature_a.length != 0 && feature_b.length != 0) {
            entity_rel = Utils.getEucledianDistance(feature_a, feature_b, 0);
        }

        double[] label_query_sim = getEntityLabelQuerySimilarity(entity_a, entity_b, query, entities);

        rel_score[0] = entity_rel;
        rel_score[1] = label_query_sim[0];
        rel_score[2] = label_query_sim[1];
        rel_score[3] = label_query_sim[2];
        rel_score[4] = label_query_sim[3];
        return rel_score;
    }

    /**
     * Measure string similarity.
     *
     * @param entity_a
     * @param query
     * @param entities
     * @return
     */
    private static double[] getEntityLabelQuerySimilarity(String entity_a, String entity_b, String query, Map<String, Map<String, Set<String>>> entities) {
        double[] rst = new double[4];
        rst[0] = rst[1] = rst[2] = rst[3] = Double.MAX_VALUE;
        Map<String, Set<String>> entity_profile_a = entities.get(entity_a);
        Map<String, Set<String>> entity_profile_b = entities.get(entity_b);

        String label_a = null, label_b = null;
        if (entity_profile_a == null || entity_profile_b == null) {
            return rst;
        }
        for (String predicate : entity_profile_a.keySet()) {
            String label_pred = predicate.toLowerCase();
            if (label_pred.contains("label") || label_pred.contains("title") || label_pred.contains("name")) {
                label_a = entity_profile_a.get(predicate).toString();
                break;
            }
        }
        for (String predicate : entity_profile_b.keySet()) {
            String label_pred = predicate.toLowerCase();
            if (label_pred.contains("label") || label_pred.contains("title") || label_pred.contains("name")) {
                label_b = entity_profile_b.get(predicate).toString();
                break;
            }
        }
        double lv_a = 0, jw_a = 0, lv_b = 0, jw_b = 0;
        if (label_a != null && !label_a.isEmpty()) {
            label_a = label_a.replaceAll("\\[|\\]", "").trim().toLowerCase();
            int length = label_a.toLowerCase().length() > query.length() ? label_a.length() : query.length();
            lv_a = 1 - (StringUtils.getLevenshteinDistance(label_a, query.toLowerCase()) / (double) length);
            jw_a = 1 - StringUtils.getJaroWinklerDistance(label_a, query.toLowerCase());
        } else {
            return rst;
        }

        if (label_b != null && !label_b.isEmpty()) {
            label_b = label_b.replaceAll("\\[|\\]", "").trim();
            int length = label_b.toLowerCase().length() > query.length() ? label_b.length() : query.length();
            lv_b = (StringUtils.getLevenshteinDistance(label_b, query.toLowerCase()) / (double) length);
            jw_b = StringUtils.getJaroWinklerDistance(label_b, query.toLowerCase());
        } else {
            return rst;
        }

        rst[0] = lv_a;
        rst[1] = jw_a;
        rst[2] = lv_b;
        rst[3] = jw_b;

        return rst;
    }


    private static int[] getEntityFeatureVector(THashMap<Integer, THashMap<String, Integer>> terms_dictionary,
                                                String entity,
                                                THashMap<Integer, TIntIntHashMap> map_terms,
                                                THashMap<String, TIntHashSet> entity_objects,
                                                THashMap<String, Integer> objects_dictionary) {
        int feature_length = objects_dictionary.size();
        for (int k : terms_dictionary.keySet()) {
            feature_length += terms_dictionary.get(k).size();
        }

        int[] features = new int[feature_length];
        int feature_index = 0;

        for (int k : terms_dictionary.keySet()) {
            for (String term : terms_dictionary.get(k).keySet()) {
                int term_id = terms_dictionary.get(k).get(term);
                int term_value = map_terms.get(k).get(term_id);
                features[feature_index] = term_value;
                feature_index++;
            }
        }

        //add the object values
        for (String object : objects_dictionary.keySet()) {
            int object_id = objects_dictionary.get(object);
            int object_value = entity_objects.get(entity).contains(object_id) ? 1 : 0;
            features[feature_index] = object_value;
            feature_index++;
        }
        return features;
    }

    /**
     * Load the triples from the entities stored in the BTC index.
     *
     * @param btc_index
     * @param rdf3x_engine
     * @param entities
     * @return
     */
    private static Map<String, Map<String, Set<String>>> loadEntityProfiles(String btc_index, String rdf3x_engine, Set<String> entities) throws IOException, InterruptedException {
        Map<String, Map<String, Set<String>>> rst = new HashMap<>();

        for (String entity : entities) {
            Map<String, Set<String>> entity_triples = Utils.loadRDF3XEntityTriples(entity, btc_index, rdf3x_engine);
            if (entity_triples == null) {
                continue;
            }
            System.out.printf("Loaded profile for entity %s with %d triples.\n", entity, entity_triples.size());
            rst.put(entity, entity_triples);
        }

        return rst;
    }


    /**
     * Load top-10 entities using the standard retrieval approach, BM25F.
     *
     * @param queries
     * @param index
     * @return
     * @throws IOException
     * @throws ParseException
     */
    private static Map<String, Set<String>> query(Map<String, String> queries, String index) throws IOException, ParseException {
        RetrievalModel rm = new RetrievalModel();
        IndexSearcher index_searcher = rm.getLuceneIndex(index);

        Map<String, Set<String>> rst = new HashMap<>();
        for (String qid : queries.keySet()) {
            String query_str = queries.get(qid);
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_40);
            QueryParser parser = new QueryParser(Version.LUCENE_40, "body", analyzer);
            Query query = parser.parse(query_str);

            //search the Lucene index using the standard approach.
            Set<String> query_results = rm.bm25SearchMap(index_searcher, query, 100);
            rst.put(qid, query_results);
            System.out.printf("Finished querying for query %s with %d entities.\n", query_str, query_results.size());
        }
        return rst;
    }

    /**
     * Loads the ground truth entities for queries in the btc dataset.
     *
     * @param file
     * @return
     */
    private static Map<String, Set<Map.Entry<String, Integer>>> loadGTEntities(String file) {
        Map<String, Set<Map.Entry<String, Integer>>> gt = new HashMap<>();
        String[] lines = FileUtils.readText(file).split("\n");

        for (String line : lines) {
            String[] tmp = line.split("\t");

            String qid = tmp[0];
            String entity = tmp[2];
            int rel_score = Integer.valueOf(tmp[3]);

            Set<Map.Entry<String, Integer>> sub_gt = gt.get(qid);
            sub_gt = sub_gt == null ? new HashSet<>() : sub_gt;
            gt.put(qid, sub_gt);

            sub_gt.add(new AbstractMap.SimpleEntry<>(entity, rel_score));
        }

        return gt;
    }
}
