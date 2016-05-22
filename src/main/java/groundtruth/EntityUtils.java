package groundtruth;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import ranking.LanguageModels;
import utils_package.FileUtils;
import utils_package.Utils;

import java.io.*;
import java.text.ParseException;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by besnik on 11/5/14.
 */
public class EntityUtils {
    public static void main(String[] args) throws IOException, ParseException, org.apache.lucene.queryparser.classic.ParseException {
        String gt_data = "", operation = "", out_dir = "", btc_data = "",
                query_file = "", btc_index = "", search_field = "",
                bm25entities = "", stop_words = "";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-gt_data")) {
                gt_data = args[++i];
            } else if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-btc_data")) {
                btc_data = args[++i];
            } else if (args[i].equals("-btc_index")) {
                btc_index = args[++i];
            } else if (args[i].equals("-search_field")) {
                search_field = args[++i];
            } else if (args[i].equals("-query_file")) {
                query_file = args[++i];
            } else if (args[i].equals("-bm25entities")) {
                bm25entities = args[++i];
            } else if (args[i].equals("-stop_words")) {
                stop_words = args[++i];
            }
        }

        EntityUtils eu = new EntityUtils();
        if (operation.equals("load_entities")) {
            System.out.println("Loading entity profiles.");
            eu.loadGroundTruthEntityProfiles(gt_data, out_dir, btc_data);
        } else if (operation.equals("btc_gt_similarity")) {
            Set<String> btc_files = new HashSet<>();
            FileUtils.getFilesList(btc_data, btc_files);
            eu.loadSimilarityData(btc_files, gt_data, bm25entities, out_dir);
        } else if (operation.equals("query_btc")) {
            Map<String, String> queries = FileUtils.readIntoStringMap(query_file, "\t", false);
            IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(btc_index)));
            IndexSearcher searcher = new IndexSearcher(reader);
            // :Post-Release-Update-Version.LUCENE_XY:
            Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

            // :Post-Release-Update-Version.LUCENE_XY:
            QueryParser parser = new QueryParser(Version.LUCENE_CURRENT, search_field, analyzer);

            Map<String, Set<String>> entities = new HashMap<>();
            for (String qid : queries.keySet()) {
                System.out.println("Querying for: " + queries.get(qid));
                Set<String> q_entities = new HashSet<>();
                entities.put(qid, q_entities);

                Query query = parser.parse(queries.get(qid));

                searcher.search(query, null, 100);
                // Collect enough docs to show 5 pages
                TopDocs results = searcher.search(query, 100);
                ScoreDoc[] hits = results.scoreDocs;

                int numTotalHits = results.totalHits;
                System.out.println(numTotalHits + " total matching documents");
                for (int i = 0; i < hits.length; i++) {
                    Document doc = searcher.doc(hits[i].doc);
                    q_entities.add(doc.get("resource_uri"));
                }
            }

            StringBuffer sb = new StringBuffer();
            entities.keySet().forEach(qid -> entities.get(qid).forEach(entity -> sb.append(qid).append("\t").append(entity).append("\n")));
            FileUtils.saveText(sb.toString(), out_dir + "/top_100_entities_2010.csv");
        } else if (operation.equals("query_entity_similarity")) {
            String[] bm25_gt_entities = FileUtils.readText(gt_data).split("\n");

            LanguageModels lm = new LanguageModels(FileUtils.readIntoSet(stop_words, "\n", false));
            //load entities
            Map<String, String> semsearch_queries = FileUtils.readIntoStringMap(query_file, "\t", false);
            for (String qid : semsearch_queries.keySet()) {
                System.out.println("Computing parameters for query: " + qid);
                String query = semsearch_queries.get(qid);

                Map<String, Set<String>> entity_profiles = new HashMap<>();
                Map<String, String> entity_labels = new HashMap<>();
                Map<String, Integer> entity_relevance = new HashMap<>();

                for (String line : bm25_gt_entities) {
                    String[] tmp = line.split("\t");
                    if (tmp.length != 5) {
                        System.out.printf("Incorrect line: [%d]\t%s.\n", tmp.length, line);
                        continue;
                    }
                    if (!tmp[2].contains(qid)) {
                        continue;
                    }

                    Set<String> sub_prof = entity_profiles.get(tmp[2]);
                    sub_prof = sub_prof == null ? new HashSet<>() : sub_prof;
                    entity_profiles.put(tmp[2], sub_prof);
                    sub_prof.add(tmp[3] + "\t" + tmp[4]);

                    //for every entity keep its label
                    if (tmp[3].contains("label")) {
                        entity_labels.put(tmp[2], tmp[4]);
                    }

                    //get the relevance of the entity
                    if (tmp[0].contains("G")) {
                        String rel_str = tmp[0];
                        int start_index = rel_str.indexOf(qid + "=") + (qid + "=").length() + 1;
                        int rel_val = Integer.valueOf(rel_str.substring(start_index, start_index + 1));
                        entity_relevance.put(tmp[2], rel_val);
                    } else {
                        entity_relevance.put(tmp[2], -1);
                    }
                }

                //construct the entity features
                Map<String, Map.Entry<String, String>> entity_features = new HashMap<>();
                for (String entity : entity_profiles.keySet()) {
                    Map<String, String> features = Utils.getEntityFeatures(entity_profiles.get(entity));
                    Map<String, Integer> lm_entity = lm.getLanguageModel(features.get("lm"));

                    Map.Entry<String, String> ef = new AbstractMap.SimpleEntry<String, String>(lm_entity.toString(), features.get("structured"));
                    entity_features.put(entity, ef);
                }

                Map<String, Map<String, Double>> entity_adjacency = Utils.getEntityAdjacencyMatrix(entity_features);
                StringBuffer sb = new StringBuffer();

                entity_adjacency.keySet().forEach(entity_a ->
                        entity_adjacency.get(entity_a).keySet().forEach(entity_b -> {
                            double entity_a_query_jw = StringUtils.getJaroWinklerDistance(getEntityLabel(entity_a, entity_labels), query);
                            double entity_a_query_lw = StringUtils.getLevenshteinDistance(getEntityLabel(entity_a, entity_labels), query);
                            double entity_a_query_ov = Utils.getTermOverlap(getEntityLabel(entity_a, entity_labels), query);

                            double entity_b_query_jw = StringUtils.getJaroWinklerDistance(getEntityLabel(entity_b, entity_labels), query);
                            double entity_b_query_lw = StringUtils.getLevenshteinDistance(getEntityLabel(entity_b, entity_labels), query);
                            double entity_b_query_ov = Utils.getTermOverlap(getEntityLabel(entity_b, entity_labels), query);

                            sb.append(qid).append("\t").
                                    append(entity_a).append("\t").
                                    append(entity_b).append("\t").
                                    append(entity_adjacency.get(entity_a).get(entity_b)).append("\t").
                                    append(entity_a_query_jw).append("\t").
                                    append(entity_b_query_jw).append("\t").
                                    append(entity_a_query_lw).append("\t").
                                    append(entity_b_query_lw).append("\t").
                                    append(entity_a_query_ov).append("\t").
                                    append(entity_b_query_ov).append("\n");
                        }));

                FileUtils.saveText(sb.toString(), out_dir + "/" + qid + "_query_features.csv");
            }
        }
    }

    public static String getEntityLabel(String entity, Map<String, String> labels) {
        if (labels.containsKey(entity)) {
            return labels.get(entity);
        }

        String tmp = entity;
        tmp = tmp.contains("/") ? tmp.substring(tmp.lastIndexOf("/") + 1) : tmp;
        return tmp;
    }

    public void loadSimilarityData(Set<String> btc_files, String gt_data, String bm25_entities, String out_dir) {
        String[] bm25f_entites = FileUtils.readText(bm25_entities).split("\n");
        String[] gt_entities = FileUtils.readText(gt_data).split("\n");

        //load the entities into the corresponding queries
        Map<String, Set<String>> qbm_entities = new HashMap<>();
        Map<String, Map<String, Integer>> qgt_entities = new HashMap<>();

        //load first the top-100 retrieved entities from BM25F
        for (String line : bm25f_entites) {
            String[] tmp = line.split("\t");
            Set<String> qe = qbm_entities.get(tmp[1]);
            qe = qe == null ? new HashSet<>() : qe;
            qbm_entities.put(tmp[1], qe);
            qe.add(tmp[0]);
        }
        //load the data from the ground truth
        for (String line : gt_entities) {
            String[] tmp = line.split("\t");
            Map<String, Integer> qg = qgt_entities.get(tmp[2]);
            qg = qg == null ? new HashMap<>() : qg;
            qgt_entities.put(tmp[2], qg);

            qg.put(tmp[0], Integer.valueOf(tmp[3]));
        }

        //load the data from the BTC dataset into a single file. Indicate by G and B
        btc_files.parallelStream().forEach(file -> {
            StringBuffer sb = new StringBuffer();
            BufferedReader reader = FileUtils.getCompressedFileReader(file);
            try {
                while (reader.ready()) {
                    Node[] triple = NxParser.parseNodes(reader.readLine());
                    if (!qbm_entities.containsKey(triple[0].toString()) && !qgt_entities.containsKey(triple[0].toString())) {
                        continue;
                    }

                    String prefix = "";
                    String queries = "";
                    if (qbm_entities.containsKey(triple[0].toString())) {
                        prefix += "B";
                        queries += qbm_entities.get(triple[0].toString());
                    }
                    if (qgt_entities.containsKey(triple[0].toString())) {
                        if (!prefix.isEmpty())
                            prefix += "|";
                        prefix += "G";
                        if (!queries.isEmpty())
                            queries += "|";

                        queries += qgt_entities.get(triple[0].toString());
                    }

                    sb.append(prefix).append("\t").
                            append(queries).append("\t").
                            append(triple[0].toString()).append("\t").
                            append(triple[1].toString()).append("\t").
                            append(triple[2].toString()).append("\n");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //save the output for further processing
            FileUtils.saveText(sb.toString(), out_dir + "/bm25_gt_2010_entities.csv", true);
            System.out.println("Finished reading file: " + file);
        });
    }

    /**
     * Load the entity profiles of the set of entities from the SemSearch ground truth dataset.
     *
     * @param gt_data
     * @param out_dir
     * @param btc_data
     */
    public void loadGroundTruthEntityProfiles(String gt_data, String out_dir, String btc_data) {
        String[] lines = FileUtils.readText(gt_data).split("\n");
        Set<String> gt_entities = new HashSet<>();
        for (String line : lines) {
            String[] tmp = line.split("\t");
            gt_entities.add(tmp[2]);
        }

        //load the gt-entities
        Map<String, Set<String>> entity_profiles = loadEntityProfiles(btc_data, gt_entities);
        StringBuffer sb = new StringBuffer();
        entity_profiles.keySet().forEach(entity -> {
            entity_profiles.get(entity).forEach(entity_triple -> sb.append(entity_triple).append("\n"));
        });
        FileUtils.saveText(sb.toString(), out_dir + "/ground_truth_entity-profiles.csv");
    }

    /**
     * Given a set of entity URIs retrieves the corresponding entity profiles.
     *
     * @param btc_dir
     * @param entities
     * @return
     */
    private Map<String, Set<String>> loadEntityProfiles(String btc_dir, Set<String> entities) {
        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(btc_dir, files);
        Map<String, Set<String>> entity_profiles = new HashMap<>();
        files.parallelStream().forEach(data_path -> {
            try {
                System.out.println("Reading file: " + data_path);
                InputStream fileStream = new FileInputStream(data_path);
                InputStream gzipStream = new GZIPInputStream(fileStream);
                Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
                BufferedReader reader = new BufferedReader(decoder);

                while (reader.ready()) {
                    String line = reader.readLine();
                    Node[] triple = NxParser.parseNodes(line);
                    if (!entities.contains(triple[0].toString())) {
                        continue;
                    }

                    Set<String> sub_profiles = entity_profiles.get(triple[0].toString());
                    sub_profiles = sub_profiles == null ? new HashSet<String>() : sub_profiles;
                    entity_profiles.put(triple[0].toN3(), sub_profiles);

                    sub_profiles.add(line);
                }
                reader.close();
                System.out.println("Finished processing file: " + data_path);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        });

        return entity_profiles;
    }
}
