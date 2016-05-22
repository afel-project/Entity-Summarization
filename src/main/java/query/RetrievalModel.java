package query;

import externalFeature.DBpediaResource;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import utils_package.FileUtils;
import utils_package.Utils;

import java.io.*;
import java.util.*;

/**
 * Created by besnik on 12/22/14.
 */
public class RetrievalModel {
    public static void main(String[] args) throws ParseException, IOException, InterruptedException {
        String query_file = "", cluster_dir = "", index = "", operation = "", field = "", out_dir = "",
                feature_dir = "", entity_type_index = "", lsh_bins = "", rdf3x_engine = "",
                base_results = "", stop_words_path = "", query_affinity = "", query_mappings = "",
                results = "", type_raw = "", tag = "", cluster_features = "", type = "Thing";

//        if(true) {
//            DBpediaResource dbr = new DBpediaResource("Forrest Gump");
//            System.exit(1);
//        }

        int top_k = 100, cut_off = 10;
        boolean is_spectral = false, add_lsh_clusters = false, inex_queries = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-query_file")) {
                query_file = args[++i];
            } else if (args[i].equals("-clusters")) {
                cluster_dir = args[++i];
            } else if (args[i].equals("-index")) {
                index = args[++i];
            } else if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-field")) {
                field = args[++i];
            } else if (args[i].equals("-top_k")) {
                top_k = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-entity_type_index")) {
                entity_type_index = args[++i];
            } else if (args[i].equals("-lsh_bins")) {
                lsh_bins = args[++i];
            } else if (args[i].equals("-feature_dir")) {
                feature_dir = args[++i];
            } else if (args[i].equals("-is_spectral")) {
                is_spectral = Boolean.valueOf(args[++i]);
            } else if (args[i].equals("-rdf3x_engine")) {
                rdf3x_engine = args[++i];
            } else if (args[i].equals("-base_results")) {
                base_results = args[++i];
            } else if (args[i].equals("-stop_words")) {
                stop_words_path = args[++i];
            } else if (args[i].equals("-query_affinity")) {
                query_affinity = args[++i];
            } else if (args[i].equals("-query_mappings")) {
                query_mappings = args[++i];
            } else if (args[i].equals("-results")) {
                results = args[++i];
            } else if (args[i].equals("-type_raw")) {
                type_raw = args[++i];
            } else if (args[i].equals("-tag")) {
                tag = args[++i];
            } else if (args[i].equals("-add_lsh_clusters")) {
                add_lsh_clusters = Boolean.valueOf(args[++i]);
            } else if (args[i].equals("-cluster_features")) {
                cluster_features = args[++i];
            } else if (args[i].equals("-inex")) {
                inex_queries = Boolean.valueOf(args[++i]);
            } else if (args[i].equals("-cutoff")) {
                cut_off = Integer.valueOf(args[++i]);
            }
            else if (args[i].equals("-type")) {
                type = args[++i];
            }
        }

        RetrievalModel rm = new RetrievalModel();

        if (operation.equals("bm25f")) {
            Map<String, String> queries = FileUtils.readIntoStringMap(query_file, "\t", false);

            IndexSearcher index_searcher = rm.getLuceneIndex(index);

//            //for getting dataset info
//            IndexReader ir = index_searcher.getIndexReader();
//            System.out.println("Document number:\t" + ir.numDocs());
//
//            System.exit(502);

              String[] typeFields = loadFileds(type);
//            String[] fieldList = null;
//            while(index_searcher.getIndexReader().);


            //String out_file_body = out_dir + "/" + tag + "_bm25_query_body.csv";
            //System.out.println("Number of queries: " + queries.size());
            for (String qid : queries.keySet()) {
                System.out.println("QueryId:\t"+qid);
                String query_str = queries.get(qid);
                Analyzer analyzer = new StandardAnalyzer();
//                QueryParser parser = new QueryParser(field, analyzer);
                QueryParser parser = new MultiFieldQueryParser(typeFields, analyzer);
                Query query = parser.parse(query_str);

                //search the Lucene index using the standard approach.
                String out_file = out_dir + "/" + tag +"_"+qid+ "_bm25_query.csv";
                rm.bm25Search(index_searcher, query, top_k, out_file, qid, type);
                System.out.printf("Finished querying for query \"%s\".\n", query_str);
            }
        } else if (operation.equals("hybrid")) {
            //query for related entities. For each query and the BM25F retrieved entities get entities within a cluster.
            Map<String, String> queries = FileUtils.readIntoStringMap(query_file, "\t", false);

            Map<String, Map<String, Double>> entities = rm.loadEntities(base_results);
            Set<String> all_entities = new HashSet<>();
            entities.keySet().forEach(query -> all_entities.addAll(entities.get(query).keySet()));
            System.out.printf("Loading related entities for %d baseline entities and %d queries\n", all_entities.size(), queries.size());

            //load the type index of the queried entities based on the BM25F model.
            THashMap<String, Integer> type_index = null;
            if (!FileUtils.fileExists(out_dir + tag + "_entity_type_index.obj", false)) {
                type_index = rm.loadEntityTypeIndex(entity_type_index, all_entities);
                FileUtils.saveObject(type_index, out_dir + tag + "_entity_type_index.obj");
            } else {
                type_index = (THashMap<String, Integer>) FileUtils.readObject(out_dir + tag + "_entity_type_index.obj");
            }

            //load the LSH bins for each of the entities based on their type index.
            THashMap<String, Integer> lsh_bin_index = null;
            if (!FileUtils.fileExists(out_dir + tag + "_entity_lsh_index.obj", false)) {
                lsh_bin_index = rm.loadEntityLSHBins(lsh_bins, type_index, feature_dir);
                FileUtils.saveObject(lsh_bin_index, out_dir + tag + "_entity_lsh_index.obj");
            } else {
                lsh_bin_index = (THashMap<String, Integer>) FileUtils.readObject(out_dir + tag + "_entity_lsh_index.obj");
            }
            System.out.printf("Loaded entity type and LSH index with %d type and %d bins\n", type_index.size(), lsh_bin_index.size());

            //load the entity clusters for all the retrieved entities.
            THashMap<Integer, THashMap<Integer, THashMap<Integer, THashSet<String>>>> type_lsh_clusters = new THashMap<>();
            if (!FileUtils.fileExists(out_dir + tag + "_type_lsh_clusters.obj", false)) {
                rm.loadEntityClusters(lsh_bin_index, type_index, type_lsh_clusters, cluster_dir, lsh_bins, feature_dir, is_spectral, add_lsh_clusters, cluster_features);
                FileUtils.saveObject(type_lsh_clusters, out_dir + tag + "_type_lsh_clusters.obj");
            } else {
                type_lsh_clusters = (THashMap<Integer, THashMap<Integer, THashMap<Integer, THashSet<String>>>>) FileUtils.readObject(out_dir + tag + "_type_lsh_clusters.obj");
            }
            System.out.printf("Loaded cluster information for the baseline entities %d\n", type_lsh_clusters.size());

            //related entities from the type and cluster index
            THashMap<String, THashMap<String, Map<Integer, Set<String>>>> rel_entities = rm.loadRelatedEntities(entities, type_index, lsh_bin_index, type_lsh_clusters);
            System.out.printf("Finished loading entity related entities.\n");

            //load the triple values
            Map<String, Map<String, Set<String>>> triples = new HashMap<>();
            if (FileUtils.fileExists(out_dir + "triples.csv.gz", false)) {
                triples = rm.loadTriples(out_dir + "triples.csv.gz");
            }
            //load stop words
            Set<String> stop_words = FileUtils.readIntoSet(stop_words_path, "\n", false);
            System.out.println("Computing entity scores");
            rm.computeEntityScores(queries, rel_entities, triples, entities, stop_words, index, rdf3x_engine, out_dir, type_raw, tag);
        } else if (operation.equals("ranking")) {
            Map<String, Map<String, Set<String>>> triples = rm.loadTriples(out_dir + "triples.csv.gz");
            //load the queries we are ranking the entities for
            Map<String, String> queries = FileUtils.readIntoStringMap(query_file, "\t", false);
            //load the query mappings.
            Map<String, Set<String>> qm = FileUtils.readMapSet(query_mappings, "\t");
            // load the query affinity scores
            Map<String, Map<String, Double>> query_affinity_score = rm.loadQueryAffinity(query_affinity);

            //if its an inex query set add the affinity scores
            Map<String, Set<String>> query_terms = new HashMap<>();
            if (inex_queries) {
                Set<String> stop_words = FileUtils.readIntoSet(stop_words_path, "\n", false);
                for (String qid : queries.keySet()) {
                    Set<String> terms = rm.analyzeUserQuery(qid, queries.get(qid), query_affinity_score, stop_words);
                    query_terms.put(qid, terms);
                }
            }

            //for every entity we find one of the matching types and get the corresponding scores
            Map<String, Map<String, Double>> hybrid_entities = rm.loadEntitiesHybrid(results, cut_off, top_k);

            Set<String> all_entities = new HashSet<>();
            for (String qid : hybrid_entities.keySet()) {
                all_entities.addAll(hybrid_entities.get(qid).keySet());
            }
            System.out.printf("Loading entity types for %d entities.\n", all_entities.size());
            Map<String, Set<String>> entity_types = Utils.loadBTCEntityTypes(all_entities, index, rdf3x_engine);

            //score the documents
            StringBuffer sb = new StringBuffer();
            for (String qid : hybrid_entities.keySet()) {
                Set<String> terms = query_terms.get(qid);
                Map<String, Double> sub_qa = query_affinity_score.get(qid);
                for (String entity : hybrid_entities.get(qid).keySet()) {
                    double prob = 0;
                    if (sub_qa != null && !sub_qa.isEmpty()) {
                        prob = rm.getQueryTypeAffinity(sub_qa, qm, entity_types.get(entity));
                    }

                    double cx_score = inex_queries ? rm.getQueryContextTermOverlap(terms, entity, triples) : 0;
                    double score = hybrid_entities.get(qid).get(entity) + prob + cx_score;
                    sb.append(qid).append("\t").append(entity).append("\t").append(score).append("\n");
                }
            }
            FileUtils.saveText(sb.toString(), out_dir + "/" + tag + "_hybrid_ranked_entities.csv");
        }
    }

    public static String[] loadFileds(String type) throws IOException {
        System.out.println("Loading field list.");

        String[] fieldList = null;

        BufferedReader br = new BufferedReader(new FileReader("fieldList"));

        String line = "";

        while( (line = br.readLine()) != null){
            if(line.startsWith(type+" ")){
                return line.substring(type.length()+1).split(" ");
            }
            if(line.startsWith("Thing ")){
                fieldList = line.substring(6).split(" ");
                //System.out.println(fieldList);
            }
        }

        br.close();
        System.out.println("Could not find the specified type, returned fields of Thing instead.");
        return fieldList;
    }

    private Map<String, Map<String, Set<String>>> loadTriples(String file) {
        //load the triple values
        Map<String, Map<String, Set<String>>> triples = new HashMap<>();
        String[] lines = FileUtils.readText(file, true).split("\n");
        for (String line : lines) {
            String[] tmp = line.split("\t");
            if (tmp.length != 3) {
                continue;
            }
            Map<String, Set<String>> entity = triples.get(tmp[0]);
            entity = entity == null ? new HashMap<>() : entity;
            triples.put(tmp[0], entity);
            Set<String> pred = entity.get(tmp[1]);
            pred = pred == null ? new HashSet<>() : pred;
            entity.put(tmp[1], pred);
            pred.add(tmp[2]);
        }
        return triples;
    }

    /**
     * Measure the entity relatedness score based on the query, clusters and the suggested BM25F entities.
     *
     * @param queries
     * @param rel_entities
     * @param triples
     * @param entities
     * @param stop_words
     * @param index_f
     * @param rdf3x_f
     * @param out_dir
     * @param type_raw
     * @param tag
     */
    private void computeEntityScores(Map<String, String> queries,
                                     THashMap<String, THashMap<String, Map<Integer, Set<String>>>> rel_entities,
                                     Map<String, Map<String, Set<String>>> triples, Map<String, Map<String, Double>> entities,
                                     Set<String> stop_words, String index_f, String rdf3x_f, String out_dir,
                                     String type_raw, String tag) {
        double lambda = 0.6;
        //store the relatedness values
        HybridModelEM hm = new HybridModelEM();

        StringBuffer sb_final = new StringBuffer();

        Map<String, Map<String, Map<String, Double>>> query_rel_scores = new HashMap<>();
        queries.keySet().parallelStream().forEach(qid -> {
            String query = queries.get(qid);
            Map<String, Map<String, Double>> sub_q_rel_scores = new HashMap<>();
            query_rel_scores.put(qid, sub_q_rel_scores);

            if (!rel_entities.containsKey(qid)) {
                return;
            }
            THashMap<String, Map<Integer, Set<String>>> query_related_entities = rel_entities.get(qid);

            query_related_entities.keySet().parallelStream().forEach(bs_entity -> {
                Map<String, Double> entity_qrel_scores = new HashMap<>();
                sub_q_rel_scores.put(bs_entity, entity_qrel_scores);

                if (!triples.containsKey(bs_entity)) {
                    Map<String, Set<String>> entity_triples = Utils.loadRDF3XEntityTriples(bs_entity, index_f, rdf3x_f);
                    if (entity_triples != null) {
                        triples.put(bs_entity, entity_triples);
                    }
                }

                //load entities related to the baseline entity
                if (query_related_entities.containsKey(bs_entity)) {
                    //if the number of entities is too large use the bulk loading approach.
                    Map<Integer, Set<String>> related_entities = query_related_entities.get(bs_entity);
                    related_entities.keySet().parallelStream().forEach(type_id -> {
                        Set<String> tmp_entities = new HashSet<>(related_entities.get(type_id));

                        if (tmp_entities.size() < 100) {
                            Utils.loadRDF3XEntityTriplesBulk(tmp_entities, index_f, rdf3x_f, triples);
                        } else {
                            String type_raw_file = type_raw + "/" + type_id + "_data.nq.gz";
                            Utils.loadRDF3XEntityTriples(tmp_entities, type_raw_file, triples);
                        }
                    });
                } else {
                    return;
                }

                System.out.printf("Finished loading triples for entity %s and query %s \n", bs_entity, qid);
                //compute the scores
                query_related_entities.get(bs_entity).keySet().parallelStream().forEach(type_rel_entity -> {
                    query_related_entities.get(bs_entity).get(type_rel_entity).parallelStream().forEach(rl_entity -> {
                        if (!triples.containsKey(rl_entity)) {
                            return;
                        }
                        try {
                            double[] scores = hm.getEntityRelatednessScore(bs_entity, rl_entity, query, triples, stop_words);
                            double query_sim = (scores[4] / scores[2]);
                            query_sim = Double.isNaN(query_sim) || Double.isInfinite(query_sim) ? 100 : query_sim;
                            double value_jw = lambda * query_sim + (1 - lambda) * scores[0];
                            entity_qrel_scores.put(rl_entity, value_jw);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                });
            });
        });

        //write the triples to a file
        StringBuffer sb_triples = new StringBuffer();
        for (String entity : triples.keySet()) {
            for (String predicate : triples.get(entity).keySet()) {
                sb_triples.append(entity).append("\t").append(predicate).append("\t").append(triples.get(entity).get(predicate)).append("\n");
            }
        }
        FileUtils.saveText(sb_triples.toString(), out_dir + "triples.csv.gz", false, true);

        //store the triples
        for (String qid : query_rel_scores.keySet()) {
            if (!entities.containsKey(qid) || query_rel_scores.get(qid) == null) {
                continue;
            }
            double min = Collections.min(entities.get(qid).values());
            double max = Collections.max(entities.get(qid).values());
            for (String bs_entity : query_rel_scores.get(qid).keySet()) {
                double bs_score = 1 - ((entities.get(qid).get(bs_entity) - min) / (max - min));
                sb_final.append(qid).append("\t").append(bs_entity).append("\t").append(bs_score).append("\n");

                //add first for every baseline entity only the top-k entities
                if (query_rel_scores.get(qid).get(bs_entity) == null || query_rel_scores.get(qid).get(bs_entity).isEmpty()) {
                    continue;
                }
                for (String rl_entity : query_rel_scores.get(qid).get(bs_entity).keySet()) {
                    try {
                        double score_entry = query_rel_scores.get(qid).get(bs_entity).get(rl_entity);
                        double bm25_rank = 1 / entities.get(qid).get(bs_entity);

                        double score = bm25_rank * score_entry;
                        sb_final.append(qid).append("\t").append(bs_entity).append("\t").append(rl_entity).append("\t").append(score).append("\n");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        FileUtils.saveText(sb_final.toString(), out_dir + "/" + tag + "_hybrid_entities.csv");
    }

    /**
     * Given a user query, we disambiguate the entities and look for their corresponding type.
     *
     * @param query
     */
    public Set<String> analyzeUserQuery(String qid, String query, Map<String, Map<String, Double>> query_affinity_score, Set<String> stop_words) {
        Set<String> ned_spots = new HashSet<>();
        Map<String, Set<String>> ned_query = Utils.disambiguateQueryTerms(query, 0.3, ned_spots);

        //all entity types
        Set<String> all_types = new HashSet<>();
        ned_query.keySet().forEach(entity -> all_types.addAll(ned_query.get(entity)));

        //get the closes distribution based on the type overlap :)
        String max_qid = "";
        int max_overlap = -1;
        for (String qid_cmp : query_affinity_score.keySet()) {
            Set<String> tmp = new HashSet<>(query_affinity_score.get(qid_cmp).keySet());
            tmp.retainAll(all_types);

            if (max_overlap < tmp.size()) {
                max_qid = qid_cmp;
                max_overlap = tmp.size();
            }
        }

        //add the resulting query affinity score
        query_affinity_score.put(qid, query_affinity_score.get(max_qid));

        //construct the query contextual terms
        String query_tmp = query;
        for (String spot : ned_spots) {
            query_tmp = query_tmp.replace(spot, " ");
        }
        String[] context_terms = query_tmp.toLowerCase().trim().split(" ");
        Set<String> terms = new HashSet<>();
        for (String s : context_terms) {
            if (stop_words.contains(s.trim().toLowerCase())) {
                continue;
            }
            terms.add(s);
        }
        return terms;
    }

    /**
     * Given a user query e.g. `Voice over IP' we check what is the coverage of contextual terms, that is those terms that
     * do not represent a named entity or concept.
     *
     * @param query_context_terms
     * @param entity
     * @param triples
     * @return
     */
    public double getQueryContextTermOverlap(Set<String> query_context_terms, String entity, Map<String, Map<String, Set<String>>> triples) {
        double avg_coverage = 0.0;

        StringBuffer sb = new StringBuffer();
        if (!triples.containsKey(entity)) {
            for (String predicate : triples.get(entity).keySet()) {
                for (String object : triples.get(entity).get(predicate)) {
                    if (object.trim().startsWith("<")) {
                        continue;
                    }
                    sb.append(object).append(" ");
                }
            }
        }
        String[] tmp = sb.toString().toLowerCase().split(" ");
        Set<String> terms = new HashSet<>();
        for (String s : tmp) {
            terms.add(s);
        }
        for (String query_term : query_context_terms) {
            if (terms.contains(query_term)) {
                avg_coverage += 1;
            }
        }

        return avg_coverage / query_context_terms.size();
    }

    /**
     * Compute the query affinity score with the corresponding entity type.
     *
     * @param query_affinity_score
     * @param qm
     * @param entity_types
     * @return
     */
    public double getQueryTypeAffinity(Map<String, Double> query_affinity_score, Map<String, Set<String>> qm, Set<String> entity_types) {
        if (query_affinity_score == null || entity_types == null) {
            return 0;
        }

        for (String type : entity_types) {
            boolean has_match = query_affinity_score.containsKey(type);
            String type_match = type;
            if (!has_match) {
                for (String type_cmp : qm.keySet()) {
                    if (qm.get(type_cmp).contains(type)) {
                        has_match = true;
                        type_match = type_cmp;
                        break;
                    }
                }
            }
            if (has_match) {
                if (!query_affinity_score.containsKey(type_match)) {
                    return 1.0;
                }
                double prob = query_affinity_score.get(type_match);
                double neg_prob = 0.0;

                for (String type_match_cmp : query_affinity_score.keySet()) {
                    if (!type_match_cmp.equals(type_match)) {
                        neg_prob += (1 - query_affinity_score.get(type_match_cmp));
                    }
                }
                prob /= neg_prob;
                return prob;
            }

        }
        return 0;
    }

    /**
     * @param query_affinity
     * @return
     */
    private Map<String, Map<String, Double>> loadQueryAffinity(String query_affinity) {
        Map<String, Map<String, Double>> qa = new HashMap<>();
        Map<String, Integer> qa_total = new HashMap<>();

        String[] lines = FileUtils.readText(query_affinity).split("\n");
        for (String line : lines) {
            String[] tmp = line.split("\t");
            if (tmp.length != 3) {
                continue;
            }
            String qid = tmp[0];
            String type = tmp[1];
            int counts = Integer.valueOf(tmp[2]);

            Map<String, Double> sub_qa = qa.get(qid);
            sub_qa = sub_qa == null ? new HashMap<>() : sub_qa;
            qa.put(qid, sub_qa);

            Double count = sub_qa.get(type);
            count = count == null ? 0 : count;
            count += counts;
            sub_qa.put(type, count);

            Integer total = qa_total.get(qid);
            total = total == null ? 0 : total;
            total += counts;
            qa_total.put(qid, total);
        }

        //turn into probabilities
        for (String qid : qa.keySet()) {
            for (String type : qa.get(qid).keySet()) {
                double prob = qa.get(qid).get(type) / qa_total.get(qid);
                qa.get(qid).put(type, prob);
            }
        }

        return qa;
    }


    /**
     * load baseline entities.
     *
     * @param data
     * @return
     */
    private static Map<String, Map<String, Double>> loadEntities(String data) {
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
            sub_rst.put(entity, bm25score);
        }

        return rst;
    }

    /**
     * load baseline entities.
     *
     * @param data
     * @return
     */
    private static Map<String, Map<String, Double>> loadEntitiesHybrid(String data, int cutoff, int top_k) {
        String[] lines = FileUtils.readText(data).split("\n");
        Map<String, Map<String, Map<String, Double>>> bs_rl_entities = new HashMap<>();

        //keep for baseline entities
        Map<String, Map<String, Double>> entity_set = new HashMap<>();
        for (String line : lines) {
            String[] tmp = line.split("\t");

            int length = tmp.length;
            if (length == 4) {
                String qid = tmp[0];
                String bs_entity = tmp[1];
                String rl_entity = tmp[2];
                double score = Double.valueOf(tmp[3]);
                if (score > 100) {
                    continue;
                }

                Map<String, Map<String, Double>> q_e = bs_rl_entities.get(qid);
                q_e = q_e == null ? new HashMap<>() : q_e;
                bs_rl_entities.put(qid, q_e);

                Map<String, Double> bqe = q_e.get(bs_entity);
                bqe = bqe == null ? new HashMap<>() : bqe;
                q_e.put(bs_entity, bqe);

                bqe.put(rl_entity, score);
            } else if (length == 3) {
                String qid = tmp[0];
                String bs_entity = tmp[1];
                double score = Double.valueOf(tmp[2]);

                Map<String, Double> bsq = entity_set.get(qid);
                bsq = bsq == null ? new HashMap<>() : bsq;
                entity_set.put(qid, bsq);
                bsq.put(bs_entity, score);
            }
        }

        //filter out those related entities that have either a high score or there are too many
        for (String qid : bs_rl_entities.keySet()) {
            for (String bs_entity : bs_rl_entities.get(qid).keySet()) {
                int size = bs_rl_entities.get(qid).get(bs_entity).size();
                if (size > cutoff) {
                    bs_rl_entities.get(qid).put(bs_entity, new HashMap<>());
                }
            }
        }

        //return the list of entities
        for (String qid : bs_rl_entities.keySet()) {
            Map<String, Double> sub_qrel = entity_set.get(qid);
            for (String bs_entity : bs_rl_entities.get(qid).keySet()) {
                if (bs_rl_entities.get(qid).get(bs_entity).isEmpty()) {
                    continue;
                }

                //add the entities
                Map<Double, Set<String>> tmp_entity_sorted = new TreeMap<>();
                for (String rl_entity : bs_rl_entities.get(qid).get(bs_entity).keySet()) {
                    double score = bs_rl_entities.get(qid).get(bs_entity).get(rl_entity);
                    if (sub_qrel.containsKey(rl_entity) && sub_qrel.get(rl_entity) < score) {
                        continue;
                    }
                    Set<String> sub_tmp = tmp_entity_sorted.get(score);
                    sub_tmp = sub_tmp == null ? new LinkedHashSet<>() : sub_tmp;
                    tmp_entity_sorted.put(score, sub_tmp);
                    sub_tmp.add(rl_entity);
                }

                int counter = 0;
                for (double score : tmp_entity_sorted.keySet()) {
                    if (Double.isInfinite(score) || Double.isNaN(score)) {
                        continue;
                    }
                    for (String rl_entity : tmp_entity_sorted.get(score)) {
                        sub_qrel.put(rl_entity, score);
                        counter++;
                    }

                    if (counter >= top_k) {
                        break;
                    }
                }

                System.out.printf("Query %s and entity %s have %d entities\n", qid, bs_entity, sub_qrel.size());
            }
        }

        return entity_set;
    }

    /**
     * Return a set of related entities that belong to the same clusters in our cluster index. The starting point comes from the BM25F entities.
     *
     * @param entities
     * @param type_index
     * @param lsh_bin_index
     * @param type_lsh_clusters
     * @return
     */
    private THashMap<String, THashMap<String, Map<Integer, Set<String>>>> loadRelatedEntities(Map<String, Map<String, Double>> entities,
                                                                                              THashMap<String, Integer> type_index,
                                                                                              THashMap<String, Integer> lsh_bin_index,
                                                                                              THashMap<Integer, THashMap<Integer, THashMap<Integer, THashSet<String>>>> type_lsh_clusters) {
        THashMap<String, THashMap<String, Map<Integer, Set<String>>>> rel_entities = new THashMap<>();
        for (String qid : entities.keySet()) {
            THashMap<String, Map<Integer, Set<String>>> query_rel_entities = new THashMap<>();
            rel_entities.put(qid, query_rel_entities);

            for (String entity : entities.get(qid).keySet()) {
                if (!type_index.containsKey(entity) || !lsh_bin_index.containsKey(entity)) {
                    continue;
                }

                //get the entity type
                int type_id = type_index.get(entity);
                int lsh_bin = lsh_bin_index.get(entity);

                if (!type_lsh_clusters.containsKey(type_id) || !type_lsh_clusters.get(type_id).containsKey(lsh_bin)) {
                    continue;
                }
                int cluster_id = getEntityClusterID(type_lsh_clusters.get(type_id).get(lsh_bin), entity);
                if (cluster_id == -1) {
                    continue;
                }

                //entities within a given cluster.
                THashSet<String> related_entities = type_lsh_clusters.get(type_id).get(lsh_bin).get(cluster_id);
                System.out.printf("There are %d related entities for entity %s coming from type %d and lsh bin %d.\n", related_entities.size(), entity, type_id, lsh_bin);
                Map<Integer, Set<String>> type_related_entities = query_rel_entities.get(entity);
                type_related_entities = type_related_entities == null ? new HashMap<>() : type_related_entities;
                query_rel_entities.put(entity, type_related_entities);

                type_related_entities.put(type_id, related_entities);
            }
        }
        return rel_entities;
    }

    /**
     * Returns the cluster id of an entity.
     *
     * @param clusters
     * @param entity
     * @return
     */
    private int getEntityClusterID(THashMap<Integer, THashSet<String>> clusters, String entity) {
        for (int cluster_id : clusters.keySet()) {
            if (clusters.get(cluster_id).contains(entity))
                return cluster_id;
        }
        return -1;
    }

    /**
     * Loads the entity clusters for all the retrieved entities. In addition it populates the data structure that holds
     * the entities of a given cluster for a given entity type.
     *
     * @param lsh_bin_index
     * @param type_index
     * @param type_lsh_clusters
     * @return
     */
    private void loadEntityClusters(THashMap<String, Integer> lsh_bin_index,
                                    THashMap<String, Integer> type_index,
                                    THashMap<Integer, THashMap<Integer, THashMap<Integer, THashSet<String>>>> type_lsh_clusters,
                                    String cluster_dir, String lsh_folder, String feature_dir,
                                    boolean is_spectral, boolean add_lsh_clusters, String cluster_features) throws IOException {
        StringBuffer sb_missing = new StringBuffer();
        type_index.keySet().parallelStream().forEach(entity_uri -> {
            if (!type_index.containsKey(entity_uri) || !lsh_bin_index.containsKey(entity_uri)) {
                return;
            }
            int type_id = type_index.get(entity_uri);
            int lsh_bin = lsh_bin_index.get(entity_uri);

            //check first if the clusters exist.
            String cluster_file = cluster_dir + "/" + type_id + "/" + lsh_bin;
            cluster_file = is_spectral ? cluster_file + "_sf.csv" : cluster_file + "_xmeans.csv.gz";

            THashMap<Integer, THashMap<Integer, THashSet<String>>> lsh_clusters = type_lsh_clusters.get(type_id);
            lsh_clusters = lsh_clusters == null ? new THashMap<>() : lsh_clusters;
            type_lsh_clusters.put(type_id, lsh_clusters);

            if (!FileUtils.fileExists(cluster_file, false)) {
                sb_missing.append(cluster_file).append("\n");
            }

            if (!FileUtils.fileExists(cluster_file, false) && add_lsh_clusters) {
                //load the entities directly from the LSH buckets
                if (!lsh_clusters.containsKey(lsh_bin)) {
                    THashMap<Integer, THashSet<String>> rst = loadEntityClustersLSH(lsh_folder + "/" + type_id + "_LSH_clusters.csv.gz", type_id, feature_dir);
                    lsh_clusters.put(lsh_bin, rst);
                    System.out.printf("[LSH] Adding related entities for entity type %d and %d with %d bins\n", type_id, lsh_bin, rst.size());
                }
                return;
            }

            if (!lsh_clusters.containsKey(lsh_bin) && FileUtils.fileExists(cluster_file, false)) {
                System.out.printf("Adding related entities from type %d and lsh bin %d\n", type_id, lsh_bin);
                THashMap<Integer, THashSet<String>> clusters = loadEntityClusters(cluster_file, type_id, lsh_bin, feature_dir, is_spectral, cluster_features);
                if (clusters == null) {
                    return;
                }
                lsh_clusters.put(lsh_bin, clusters);
            }
        });
        FileUtils.saveText(sb_missing.toString(), "missing_entity_types_lsh.log");
    }

    /**
     * Loads the entity clusters for a given entity type and lsh bin.
     *
     * @param cluster_file
     * @param type
     * @param feature_dir
     * @return
     */
    private THashMap<Integer, THashSet<String>> loadEntityClustersLSH(String cluster_file,
                                                                      int type,
                                                                      String feature_dir) {
        THashMap<Integer, String> entity_dict = Utils.loadEntityDictionary(feature_dir + "/" + type + "/dict_entity.obj");
        if (entity_dict == null || entity_dict.isEmpty()) {
            return null;
        }

        THashMap<Integer, TIntHashSet> lsh_cluster = Utils.loadLSHEntityBinsSimple(cluster_file, 0);
        THashMap<Integer, THashSet<String>> rst = new THashMap<>();
        for (int lsh_bin_id : lsh_cluster.keySet()) {
            rst.put(lsh_bin_id, new THashSet<>());

            for (int entity_id : lsh_cluster.get(lsh_bin_id).toArray()) {
                String entity_uri = entity_dict.get(entity_id);
                rst.get(lsh_bin_id).add(entity_uri);
            }
        }
        return rst;
    }

    /**
     * Loads the entity clusters for a given entity type and lsh bin.
     *
     * @param cluster_file
     * @param type
     * @param feature_dir
     * @return
     */
    private THashMap<Integer, THashSet<String>> loadEntityClusters(String cluster_file,
                                                                   int type, int lsh_bin,
                                                                   String feature_dir,
                                                                   boolean is_spectral,
                                                                   String cluster_features) {
        THashMap<Integer, String> entity_dict = Utils.loadEntityDictionary(feature_dir + "/" + type + "/dict_entity.obj");
        String indices_file = cluster_features + "/" + type + "/" + lsh_bin + "_indices.csv.gz";
        if (entity_dict == null || entity_dict.isEmpty()) {
            return null;
        }
        if (!is_spectral) {
            //check first if its nested.
            checkIfNestedClusterFile(cluster_file, indices_file);
            return Utils.loadEntityClustersNested(cluster_file, entity_dict);
        }
        if (!FileUtils.fileExists(indices_file, false)) {
            return null;
        }
        TIntIntHashMap entity_indices = FileUtils.readCompressedIntoIntMap(indices_file, "\t");
        //load first the entity indices from the indices file located in the cluster results file.
        return Utils.loadSpectralEntityClustersNested(cluster_file, entity_dict, entity_indices);
    }

    /**
     * In cases the xmeans is nested file fix it
     *
     * @param file
     * @param indices_file
     * @throws IOException
     */
    public void checkIfNestedClusterFile(String file, String indices_file) {
        try {
            BufferedReader reader = FileUtils.getCompressedFileReader(file);
            boolean is_nested = false;
            while (reader.ready()) {
                String line = reader.readLine();
                String[] tmp = line.split("\t");

                if (tmp.length != 2) {
                    continue;
                }

                if (line.contains("[")) {
                    is_nested = true;
                    break;
                } else {
                    is_nested = false;
                    break;
                }
            }
            reader.close();

            //if it is nested convert the file to the standard format
            if (is_nested) {
                TIntIntHashMap entity_indices = FileUtils.readCompressedIntoIntMap(indices_file, "\t");
                THashMap<Integer, THashSet<String>> rst = new THashMap<>();
                reader = FileUtils.getCompressedFileReader(file);

                StringBuffer sb = new StringBuffer();
                //load the entity index-indices pairs
                while (reader.ready()) {
                    String[] data = reader.readLine().split("\t");
                    int cluster_id = Integer.valueOf(data[0]);
                    THashSet<String> sub_rst = rst.get(cluster_id);
                    sub_rst = sub_rst == null ? new THashSet<>() : sub_rst;
                    rst.put(cluster_id, sub_rst);

                    String[] entities = data[1].replaceAll("\\[|\\]", "").split(", ");
                    for (String entity : entities) {
                        int entity_id = entity_indices.get(Integer.valueOf(entity));
                        sb.append(cluster_id).append("\t").append(entity_id).append("\n");
                    }
                }
                reader.close();
                FileUtils.saveText(sb.toString(), file, false, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * Loads the LSH bins for the entities based on their entity type index.
     *
     * @param lsh_bins
     * @param type_index
     * @return
     */
    private THashMap<String, Integer> loadEntityLSHBins(String lsh_bins, THashMap<String, Integer> type_index, String instance_dir) throws IOException {
        THashSet<Integer> types = new THashSet<>(type_index.values());
        THashMap<String, Integer> entity_bins = new THashMap<>();
        for (int type : types) {
            String lsh_bin_file = lsh_bins + "/" + type + "_LSH_clusters.csv.gz";
            String entity_dict_file = instance_dir + "/" + type + "/dict_entity.obj";
            if (!FileUtils.fileExists(lsh_bin_file, true) || !FileUtils.fileExists(entity_dict_file, true)) {
                continue;
            }
            THashMap<Integer, TIntHashSet> lsh_entity_bins = Utils.loadLSHEntityBinsSimple(lsh_bin_file, 0);
            THashMap<Integer, String> entity_uris = Utils.loadEntityDictionary(entity_dict_file);

            for (int bin_bucket : lsh_entity_bins.keySet()) {
                for (int entity_id : lsh_entity_bins.get(bin_bucket).toArray()) {
                    String entity_uri = entity_uris.get(entity_id);
                    if (!type_index.containsKey(entity_uri)) {
                        continue;
                    }
                    entity_bins.put(entity_uri, bin_bucket);
                }
            }
        }
        return entity_bins;
    }

    /**
     * Returns the entity type index for the retrieved entities from the BM25F model.
     *
     * @param entity_type_index
     * @param entities
     * @return
     * @throws IOException
     */
    private THashMap<String, Integer> loadEntityTypeIndex(String entity_type_index, Set<String> entities) throws IOException {
        THashMap<String, Integer> rst = new THashMap<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(entity_type_index);

        System.out.printf("Starting to load the type index %s\n", entity_type_index);
        while (reader.ready()) {
            String[] tmp = reader.readLine().split("\t");
            if (tmp.length != 3) {
                continue;
            }
            if (!entities.contains(tmp[2].trim())) {
                continue;
            }
            rst.put(tmp[2], Integer.valueOf(tmp[0]));
        }
        reader.close();
        return rst;
    }


    /**
     * Get the Lucene index searcher object.
     *
     * @param index
     * @return
     * @throws IOException
     * @throws ParseException
     */

    public IndexSearcher getLuceneIndex(String index) throws IOException, ParseException {
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(index)));
        IndexSearcher searcher = new IndexSearcher(reader);
        return searcher;
    }

    /**
     * This demonstrates a typical paging search scenario, where the search engine presents
     * pages of size n to the user. The user can then go to the next page if interested in
     * the next hits.
     * <p>
     * When the query is executed for the first time, then only enough results are collected
     * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     * is executed another time and all hits are collected.
     */
    public void bm25Search(IndexSearcher searcher, Query query, int top_k, String out_file, String qid, String type) throws IOException {
        // Collect enough docs to show 5 pages

        Integer fact_id = 1;
        TopDocs results = searcher.search(query, 1000);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = results.totalHits;
        System.out.println(numTotalHits + " total matching documents");

        StringBuffer sb = new StringBuffer();

        Set<String> doc_added = new HashSet<>();
//        System.out.println("hits.length = "+hits.length);

        Document [] docs = new Document[hits.length];
        for (int i = 0; i < hits.length && doc_added.size() <= top_k; i++) {
            docs[i] = searcher.doc(hits[i].doc);
            Document doc = searcher.doc(hits[i].doc);
            List<IndexableField> fields = doc.getFields();
            String resource_uri = doc.get("resource_uri");
            String page_url = doc.get("page_url");

            //filter type
            if(doc.getField("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") == null){
                continue;
            }
            if(!doc.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").contains(type)){
                continue;
            }
//            if(!doc.get("<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>").contains(type)){
//                continue;
//            }
            doc_added.add(resource_uri);
            double bm25_score = hits[i].score;


            for(IndexableField field:fields){
                if(field.name()=="page_url")//||field.name()=="resource_uri")
                    continue;
                String field_content=field.stringValue().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\s+", " ").replaceAll("\t", " ").trim();
                //if(field_content == null) continue;
                sb.append(fact_id).append("\t").append(resource_uri).append("\t").append(field.name()).
                                append("\t").append(field_content).append("\t").append(bm25_score).append("\t").append(page_url).append("\n");
                //append(page_url).append("\t").
                fact_id += 1;
            }

            fields.clear();
        }
        FileUtils.saveText(sb.toString(), out_file, true);
    }
    public static ScoreDoc[] bm25Search(IndexSearcher searcher, Query query, int top_k, String type) throws IOException {

        TopDocs results = searcher.search(query, top_k);
        ScoreDoc[] hits = results.scoreDocs;

        int numTotalHits = results.totalHits;
        System.out.println(numTotalHits + " total matching documents");

        return hits;
    }


    /**
     * This demonstrates a typical paging search scenario, where the search engine presents
     * pages of size n to the user. The user can then go to the next page if interested in
     * the next hits.
     * <p>
     * When the query is executed for the first time, then only enough results are collected
     * to fill 5 result pages. If the user wants to page beyond this limit, then the query
     * is executed another time and all hits are collected.
     */
    public Set<String> bm25SearchMap(IndexSearcher searcher, Query query, int top_k) throws IOException {
        Set<String> query_results = new HashSet<>();
        // Collect enough docs to show 5 pages
        TopDocs results = searcher.search(query, top_k);
        ScoreDoc[] hits = results.scoreDocs;

        for (int i = 0; i < hits.length && query_results.size() <= 10; i++) {
            Document doc = searcher.doc(hits[i].doc);
            String resource_uri = doc.get("resource_uri");
            query_results.add(resource_uri);
        }
        return query_results;
    }
}
