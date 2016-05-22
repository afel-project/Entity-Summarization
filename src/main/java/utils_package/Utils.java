package utils_package;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;
import ranking.LanguageModels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by besnik on 11/7/14.
 */
public class Utils {
    public static String tag = "default";

    /**
     * Use a NED tool to find the corresponding entities from the query terms.
     *
     * @param query
     * @return
     */
    public static Map<String, Set<String>> disambiguateQueryTerms(String query, double threshold, Set<String> spots) {
        Set<String> entities = performTagMeNERWiki(query, threshold, spots);

        //load the entity types.
        Map<String, Set<String>> entity_types = new HashMap<>();
        for (String entity : entities) {
            String entity_tmp = "http://dbpedia.org/resource/" + entity.replaceAll("\\s{1,}", "_");
            Set<String> types = loadEntityTypes(entity_tmp);
            entity_types.put(entity, types);
        }
        return entity_types;
    }

    /**
     * Loads the types for a given entity of interest from DBpedia.
     *
     * @param entity
     * @return
     */
    private static Set<String> loadEntityTypes(String entity) {
        QueryEngineHTTP qt = new QueryEngineHTTP("http://dbpedia.org/sparql", "SELECT ?type WHERE {<" + entity + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type}");
        ResultSet results = qt.execSelect();

        Set<String> types = new HashSet<>();
        while (results.hasNext()) {
            QuerySolution qs = results.next();
            types.add("<" + qs.get("type").toString() + ">");
        }
        return types;
    }


    /**
     * Performs the NER process on the body of NYT articles using the TagMe NER
     * tool. The named entities are in the form of Wikipedia entity URIs.
     *
     * @param doc
     * @return
     */
    public static Set<String> performTagMeNERWiki(String doc, double threshold, Set<String> spots) {
        String API_URL = "http://tagme.di.unipi.it/tag";
        String API_KEY = "5380a61d2e54f2966986cbada0054e35";
        String epsilon = "0.1";

        //store the annotation in the map data structure.
        Set<String> entities = new HashSet<>();

        try {
            List<Map.Entry<String, String>> urlParameters = new ArrayList<Map.Entry<String, String>>();
            urlParameters.add(new AbstractMap.SimpleEntry<>("key", API_KEY));
            urlParameters.add(new AbstractMap.SimpleEntry<>("epsilon", epsilon));
            urlParameters.add(new AbstractMap.SimpleEntry<>("text", doc));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_abstract", "false"));
            urlParameters.add(new AbstractMap.SimpleEntry<>("include_categories", "false"));

            String response = WebUtils.post(API_URL, urlParameters);

            //parse the json output from TagMe.
            JSONObject resultJSON = new JSONObject(response);
            if (resultJSON.has("annotations")) {
                JSONArray annotations = resultJSON.getJSONArray("annotations");

                //store the annotations
                for (int i = 0; i < annotations.length(); i++) {
                    JSONObject annotation = annotations.getJSONObject(i);

                    if (!annotation.has("title")) {
                        continue;
                    }

                    String title_wiki_page = annotation.getString("title");
                    double rho = annotation.getDouble("rho");
                    String spot = annotation.getString("spot");
                    spots.add(spot);
                    if (rho < threshold) {
                        continue;
                    }
                    entities.add(title_wiki_page);
                }
            }

        } catch (Exception e) {
        }

        return entities;
    }

    /**
     * Measures the similarity between entities as given in the paper.
     *
     * @param entry_a
     * @param entry_b
     * @return
     */
    public static double measureEntitySimilarity(Map.Entry<String, String> entry_a, Map.Entry<String, String> entry_b) {
        Map<String, Integer> lm_a = new HashMap<>();
        Map<String, Integer> lm_b = new HashMap<>();

        String[] tmp_lm_a = entry_a.getKey().replaceAll("\\{|\\}", "").split(",");
        String[] tmp_lm_b = entry_b.getKey().replaceAll("\\{|\\}", "").split(",");
        for (String s : tmp_lm_a) {
            String[] tmp = s.split("=");
            if (tmp.length != 2) {
                continue;
            }
            int lm_val = StringUtils.isNumeric(tmp[1]) ? Integer.valueOf(tmp[1]) : 0;
            lm_a.put(tmp[0], lm_val);
        }
        for (String s : tmp_lm_b) {
            String[] tmp = s.split("=");
            if (tmp.length != 2) {
                continue;
            }
            int lm_val = StringUtils.isNumeric(tmp[1]) ? Integer.valueOf(tmp[1]) : 0;
            lm_b.put(tmp[0], lm_val);
        }

        Set<String> struct_a = new HashSet<>();
        Set<String> struct_b = new HashSet<>();

        String[] tmp_str_a = entry_a.getValue().replaceAll("\\[|\\]", "").split(",");
        String[] tmp_str_b = entry_b.getValue().replaceAll("\\[|\\]", "").split(",");
        for (String s : tmp_str_a) {
            struct_a.add(s);
        }
        for (String s : tmp_str_b) {
            struct_b.add(s);
        }

        Set<String> common = new HashSet<>(struct_a);
        common.retainAll(struct_b);
        double jaccard = common.size() / (double) (struct_a.size() + struct_b.size() - common.size());

        Set<String> term_intersect = new HashSet<>(lm_a.keySet());
        term_intersect.retainAll(lm_b.keySet());
        double lm = LanguageModels.getKLDivergence(lm_a, lm_a.size(), lm_b, lm_b.size(), term_intersect);

        double lambda = term_intersect.size() / (double) (term_intersect.size() + common.size());
        return lambda * lm + (1 - lambda) * jaccard;
    }

    /**
     * Measures the similarity between entities as given in the paper.
     *
     * @param entry_a
     * @param entry_b
     * @return
     */
    public static double measureEntitySimilarity(Map<String, String> entry_a, Map<String, String> entry_b) {
        Map<String, Integer> lm_a = new HashMap<>();
        Map<String, Integer> lm_b = new HashMap<>();

        String[] tmp_lm_a = entry_a.get("lm").replaceAll("\\{|\\}", "").split(",");
        String[] tmp_lm_b = entry_b.get("structured").replaceAll("\\{|\\}", "").split(",");
        for (String s : tmp_lm_a) {
            String[] tmp = s.split("=");
            lm_a.put(tmp[0], Integer.valueOf(tmp[1]));
        }
        for (String s : tmp_lm_b) {
            String[] tmp = s.split("=");
            lm_b.put(tmp[0], Integer.valueOf(tmp[1]));
        }

        Set<String> struct_a = new HashSet<>();
        Set<String> struct_b = new HashSet<>();

        String[] tmp_str_a = entry_a.get("lm").replaceAll("\\[|\\]", "").split(",");
        String[] tmp_str_b = entry_b.get("structured").replaceAll("\\[|\\]", "").split(",");
        for (String s : tmp_str_a) {
            struct_a.add(s);
        }
        for (String s : tmp_str_b) {
            struct_b.add(s);
        }

        Set<String> common = new HashSet<>(struct_a);
        common.retainAll(struct_b);
        double jaccard = common.size() / (double) (struct_a.size() + struct_b.size() - common.size());

        Set<String> term_intersect = new HashSet<>(lm_a.keySet());
        term_intersect.retainAll(lm_b.keySet());
        double lm = LanguageModels.getKLDivergence(lm_a, lm_a.size(), lm_b, lm_b.size(), term_intersect);

        double lambda = term_intersect.size() / (double) (term_intersect.size() + common.size());

        return lambda * lm + (1 - lambda) * jaccard;
    }

    /**
     * Computes the adjacency matrix of the considered entity profiles.
     *
     * @param entity_profiles
     * @return
     */
    public static Map<String, Map<String, Double>> getEntityAdjacencyMatrix(Map<String, Map.Entry<String, String>> entity_profiles) {
        //compute the adjacency matrix of entities.
        Map<String, Map<String, Double>> entity_adjacency = new HashMap<>();
        int entity_index_a = 0;
        for (String entity_a : entity_profiles.keySet()) {
            Map<String, Double> entity_a_sim_values = new HashMap<>();
            entity_adjacency.put(entity_a, entity_a_sim_values);

            Map.Entry<String, String> entry_a = entity_profiles.get(entity_a);
            int entity_index_b = 0;
            for (String entity_b : entity_profiles.keySet()) {
                if (entity_index_b <= entity_index_a) {
                    entity_index_b++;
                    continue;
                }
                Map.Entry<String, String> entry_b = entity_profiles.get(entity_a);
                double sim_ab = measureEntitySimilarity(entry_a, entry_b);
                entity_a_sim_values.put(entity_b, sim_ab);
                entity_index_b++;
            }
            entity_index_a++;
        }
        return entity_adjacency;
    }

    public static Map<String, Set<String>> getEntityProfiles(String[] lines) {
        //generate first the entity feature vectors
        Map<String, Set<String>> entity_profiles = new HashMap<>();
        for (String line : lines) {
            try {
                Node[] triples = NxParser.parseNodes(line);
                Set<String> sub_triples = entity_profiles.get(triples[0].toString());
                sub_triples = sub_triples == null ? new HashSet<>() : sub_triples;
                entity_profiles.put(triples[0].toString(), sub_triples);

                sub_triples.add(triples[1].toString() + "\t" + triples[2].toN3());
            } catch (Exception e) {
                System.out.printf("Error parsing triple: %s\t%s.\n", line, e.getMessage());
            }
        }
        return entity_profiles;
    }

    public static Map<String, String> getEntityFeatures(Set<String> entity_profile) {
        Set<String> structured_profile = new HashSet<>();
        StringBuffer sb = new StringBuffer();
        for (String triple : entity_profile) {
            String[] tmp = triple.split("\t");
            if (tmp[1].startsWith("\"")) {
                sb.append(tmp[1]);
            } else {
                String object = tmp[1];
                if (object.contains("/")) {
                    object = object.substring(object.lastIndexOf("/") + 1);
                }
                object = object.replaceAll("<|>", "");
                structured_profile.add(object);
            }
        }

        Map<String, String> entity_features = new HashMap<>();
        entity_features.put("lm", sb.toString());
        entity_features.put("structured", structured_profile.toString());
        return entity_features;
    }

    public static double getTermOverlap(String str_a, String str_b) {
        String[] terms_a = StringUtils.split(str_a.toLowerCase());
        String[] terms_b = StringUtils.split(str_a.toLowerCase());

        SnowballProgram sb = new EnglishStemmer();
        Set<String> set_a = new HashSet<>();
        Set<String> set_b = new HashSet<>();
        for (String s : terms_a) {
            sb.setCurrent(s);
            set_a.add(sb.getCurrent());
        }
        for (String s : terms_b) {
            sb.setCurrent(s);
            set_b.add(sb.getCurrent());
        }
        Set<String> common = new HashSet<String>(set_a);
        common.retainAll(set_b);
        return common.size() / (double) (set_a.size() + set_b.size() - common.size());
    }

    /**
     * From a given collection picks randomly a set of indices until the relative sample size is reached.
     *
     * @param entity_dictionary
     * @param sample_size
     * @return
     */
    public static Set<Integer> getEntityIndicesSampling(Collection<Integer> entity_dictionary, int sample_size) {
        //entity sample indices
        Set<Integer> entity_indices = new HashSet<>();
        int upper_bound = entity_dictionary.size() / sample_size;
        Random rand = new Random();
        while (entity_indices.size() < upper_bound) {
            int entity_id = rand.nextInt(entity_dictionary.size() - 1);
            entity_indices.add(entity_id);
        }
        return entity_indices;
    }

    /**
     * Computes the Euclidean distance between two feature vectors.
     * http://en.wikipedia.org/wiki/Euclidean_distance
     *
     * @param a
     * @param b
     * @return
     */
    public static double getEucledianDistance(int[][] a, int[][] b) {
        TIntIntHashMap a_map = new TIntIntHashMap();
        TIntIntHashMap b_map = new TIntIntHashMap();

        for (int i = 0; i < a.length; i++) {
            a_map.put(a[i][0], a[i][1]);
        }
        for (int i = 0; i < b.length; i++) {
            b_map.put(b[i][0], b[i][1]);
        }

        return getEucledianDistance(a_map, b_map);
    }

    /**
     * Computes the Euclidean distance between two feature vectors.
     * http://en.wikipedia.org/wiki/Euclidean_distance
     *
     * @param a
     * @param b
     * @return
     */
    public static double getEucledianDistance(int[] a, int[] b, int start_index) {
        double rst = 0.0;

        //if the vectors are not of the same lenght.
        if (a.length != b.length) {
            return 0.0;
        }

        for (int i = start_index; i < a.length; i++) {
            rst += Math.pow(a[i] - b[i], 2);
        }

        return Math.sqrt(rst);
    }

    /**
     * Computes the Euclidean distance between two feature vectors.
     * http://en.wikipedia.org/wiki/Euclidean_distance
     *
     * @param a
     * @param b
     * @return
     */
    private static double getEucledianDistance(TIntIntHashMap a, TIntIntHashMap b) {
        double rst = 0.0;

        for (int key : a.keys()) {
            int a_val = a.get(key);
            int b_val = b.containsKey(key) ? b.get(key) : 0;
            rst += Math.pow(a_val - b_val, 2);
        }

        for (int key : b.keys()) {
            int a_val = a.containsKey(key) ? a.get(key) : 0;
            int b_val = b.get(key);
            rst += Math.pow(a_val - b_val, 2);
        }

        return Math.sqrt(rst);
    }

    /**
     * Loads the entity URI and cluster ID of entities based on the Spectral Clustering approach.
     *
     * @param file
     * @param feature_file
     * @param entity_dict
     * @return
     * @throws java.io.IOException
     */
    public static THashMap<String, Integer> loadSpectralEntityClusters(String file, String feature_file, THashMap<Integer, String> entity_dict) throws IOException {
        THashMap<String, Integer> rst = new THashMap<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(file);

        //load the entity index-indices pairs
        TIntIntHashMap entity_indices = loadEntityIndexIndices(feature_file);
        while (reader.ready()) {
            String[] data = reader.readLine().split("\t");
            int cluster_id = Integer.valueOf(data[0]);

            String[] entities = data[1].replaceAll("\\[|\\]", "").split(", ");
            for (String entity : entities) {
                int entity_indice = Integer.valueOf(entity);
                String entity_uri = entity_dict.get(entity_indices.get(entity_indice));
                rst.put(entity_uri, cluster_id);
            }
        }
        return rst;
    }

    /**
     * Loads the entity URI and cluster ID of entities based on the Spectral Clustering approach.
     *
     * @param file
     * @param entity_dict
     * @return
     * @throws java.io.IOException
     */
    public static THashMap<Integer, THashSet<String>> loadSpectralEntityClustersNested(String file, THashMap<Integer, String> entity_dict, TIntIntHashMap entity_indices) {
        THashMap<Integer, THashSet<String>> rst = new THashMap<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(file);

        try {
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
                    String entity_uri = entity_dict.get(entity_id);
                    sub_rst.add(entity_uri);
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rst;
    }

    /**
     * Load entity cluster data for a given LSH bin and entity type.
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static THashMap<String, Integer> loadEntityClusters(String file, THashMap<Integer, String> entity_dict) throws IOException {
        THashMap<String, Integer> rst = new THashMap();
        BufferedReader reader = FileUtils.getCompressedFileReader(file);
        while (reader.ready()) {
            String[] tmp = reader.readLine().split("\t");
            String entity_uri = entity_dict.get(Integer.valueOf(tmp[1]));
            rst.put(entity_uri, Integer.valueOf(tmp[0]));
        }
        reader.close();
        return rst;
    }

    /**
     * Load entity clusters.
     * @param cluster_file
     * @param is_spectral
     * @return
     * @throws IOException
     */
    public static THashMap<Integer, TIntHashSet> loadEntityCluster(String cluster_file, boolean is_spectral) throws IOException {
        BufferedReader reader = FileUtils.getCompressedFileReader(cluster_file);
        THashMap<Integer, TIntHashSet> entity_clusters = new THashMap<>();

        while (reader.ready()) {
            String[] line = reader.readLine().split("\t");

            int cluster_id = Integer.valueOf(line[0]);
            if (is_spectral) {
                TIntHashSet entities = entity_clusters.get(cluster_id);
                entities = entities == null ? new TIntHashSet() : entities;
                entity_clusters.put(cluster_id, entities);

                String[] tmp = line[1].replaceAll("\\[|\\]", "").trim().split(",");
                for (String s : tmp) {
                    entities.add(Integer.valueOf(s.trim()));
                }
            } else {
                TIntHashSet entities = entity_clusters.get(cluster_id);
                entities = entities == null ? new TIntHashSet() : entities;
                entity_clusters.put(cluster_id, entities);
                entities.add(Integer.valueOf(line[1].trim()));
            }
        }
        reader.close();
        return entity_clusters;
    }

    /**
     * Load entity cluster data for a given LSH bin and entity type.
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static THashMap<Integer, THashSet<String>> loadEntityClustersNested(String file, THashMap<Integer, String> entity_dict) {
        THashMap<Integer, THashSet<String>> rst = new THashMap<>();
        BufferedReader reader = FileUtils.getCompressedFileReader(file);

        try {
            int count = 0;
            while (reader.ready()) {
                String[] tmp = reader.readLine().split("\t");
                String entity_uri = entity_dict.get(Integer.valueOf(tmp[1]));

                int cluster_id = Integer.valueOf(tmp[0]);
                THashSet<String> sub_rst = rst.get(cluster_id);
                sub_rst = sub_rst == null ? new THashSet<>() : sub_rst;
                rst.put(cluster_id, sub_rst);

                sub_rst.add(entity_uri);
                count++;
            }
            reader.close();
            System.out.printf("Loaded %d entities from the cluster file %s. \n", count, file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rst;
    }


    /**
     * Loads the map data structure consisting of the entity indices and their corresponding indexes based on the feature files.
     *
     * @param feature_file
     * @return
     * @throws IOException
     */
    private static TIntIntHashMap loadEntityIndexIndices(String feature_file) throws IOException {
        TIntIntHashMap rst = new TIntIntHashMap();
        BufferedReader reader = FileUtils.getCompressedFileReader(feature_file);

        int line_index = 0;
        while (reader.ready()) {
            String[] data = reader.readLine().split(",");
            rst.put(line_index, Integer.valueOf(data[0]));
        }
        return rst;
    }

    /**
     * Loads the entity dictionary and traverses its key-value pairs.
     *
     * @param file
     * @return
     */
    public static THashMap<Integer, String> loadEntityDictionary(String file) {
        THashMap<String, Integer> tmp = (THashMap<String, Integer>) FileUtils.readObject(file);

        if (tmp == null || tmp.isEmpty()) {
            return null;
        }
        //traverse the map of entity URI-ID dictionary.
        THashMap<Integer, String> rst = new THashMap<>();
        for (String entity_uri : tmp.keySet()) {
            rst.put(tmp.get(entity_uri), entity_uri);
        }
        return rst;
    }


    /**
     * Parses a date.
     *
     * @param date_str
     * @return
     */
    public static String parseDate(String date_str) {
        List<SimpleDateFormat> lst = new ArrayList<>();
        lst.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
        lst.add(new SimpleDateFormat("yyyy/MM/dd"));
        lst.add(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"));
        lst.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        lst.add(new SimpleDateFormat("EEE, MMM d, yyyy"));
        lst.add(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
        lst.add(new SimpleDateFormat("EEE, MMM. dd, yyyy"));
        lst.add(new SimpleDateFormat("EEE, MMM dd, yyyy"));
        lst.add(new SimpleDateFormat("yyyyMMdd"));
        lst.add(new SimpleDateFormat("EEEEE d MMM yyyy"));
        lst.add(new SimpleDateFormat("EEEEE, MMM. dd, yyyy"));
        lst.add(new SimpleDateFormat("dd MMM yyyy"));
        lst.add(new SimpleDateFormat("MMM dd, yyyy, HH:mm a"));
        lst.add(new SimpleDateFormat("MMM dd, yyyy, H:mm a"));
        lst.add(new SimpleDateFormat("dd.MM.yyyy"));
        lst.add(new SimpleDateFormat("MMMMM dd, yyyy, HH:mm a"));
        lst.add(new SimpleDateFormat("MMMMM dd, yyyy, H:mm a"));
        lst.add(new SimpleDateFormat("MMM. dd, yyyy"));
        lst.add(new SimpleDateFormat("MMMMM dd, yyyy"));
        lst.add(new SimpleDateFormat("EEEEE, MMM. dd, yyyy"));
        lst.add(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z"));
        lst.add(new SimpleDateFormat("MMMM dd, yyyy"));
        lst.add(new SimpleDateFormat("MMMM dd, yyyy HH:mm a"));
        lst.add(new SimpleDateFormat("EEE MMMM dd HH:mm:ss z yyyy"));

        SimpleDateFormat df_simple = new SimpleDateFormat("yyyy-MM-dd");
        for (SimpleDateFormat df_tmp : lst) {
            try {
                Date dt = df_tmp.parse(date_str);
                return df_simple.format(dt);
            } catch (Exception e) {
                continue;
            }
        }
        return "";

    }

    /**
     * Queries the BTC index for the types of a given set of entities. The results are saved in a temporary file and read after into the map datastruture.
     *
     * @param entities
     * @param btc_index
     * @param rdf3x_engine
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static Map<String, Set<String>> loadBTCEntityTypes(Set<String> entities, String btc_index, String rdf3x_engine) throws IOException, InterruptedException {
        Map<String, Set<String>> entity_types = new HashMap<>();
        entities.parallelStream().forEach(entity -> {
            try {
                Set<String> types = loadRDF3XEntityTypes(entity, btc_index, rdf3x_engine, false);
                if (types == null) {
                    return;
                }
                entity_types.put(entity, types);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return entity_types;
    }

    public static Set<String> loadRDF3XEntityTypes(String entity, String btc_index, String rdf3x_engine, boolean is_missing) throws IOException, InterruptedException {
        String sparql_query = "";
        if (is_missing) {
            sparql_query = "SELECT DISTINCT ?type WHERE {" + entity + " ?p ?entity. ?entity <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type}";
        } else {
            sparql_query = "SELECT DISTINCT ?type WHERE {<" + entity + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> ?type}";
        }
        String query_file = "query_log/" + Math.abs(entity.hashCode()) + "query_file.txt";
        FileUtils.saveText(sparql_query, query_file);
        FileUtils.saveText(sparql_query + "\n", tag + "_query_log.txt", true);

        //execute the SPARQL query against the RDF3X index.
        String cmd_str = rdf3x_engine + " " + btc_index + " " + query_file;
        Process p = Runtime.getRuntime().exec(cmd_str);

        boolean rst = p.waitFor(20, TimeUnit.SECONDS);
        if (!rst) {
            System.out.printf("Failed loading entity types for entity %s \n", entity);
            return null;
        }

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        Set<String> types = new HashSet<>();
        while (r.ready()) {
            String type = r.readLine();
            type = type.replaceAll("<|>", "").trim();
            if (!type.equals("<empty result>")) {
                types.add(type);
            }
        }
        return types;
    }

    public static Map<String, Set<String>> loadRDF3XEntityTriples(String entity, String btc_index, String rdf3x_engine) {
        FileUtils.checkDir("query_log");
        try {
            String query_file = "query_log/" + Math.abs(entity.hashCode()) + "_query.txt";
            String sparql_query = "SELECT DISTINCT ?p ?o WHERE {<" + entity + "> ?p ?o}";
            FileUtils.saveText(sparql_query, query_file);
            FileUtils.saveText(sparql_query + "\n", tag + "_query_log.txt", true);

            //execute the SPARQL query against the RDF3X index.
            String cmd_str = rdf3x_engine + " " + btc_index + " " + query_file;
            Process p = Runtime.getRuntime().exec(cmd_str);

            boolean rst = p.waitFor(20, TimeUnit.SECONDS);
            if (!rst) {
                p.destroy();
                System.out.printf("Failed loading triples for entity %s \n", entity);
                return null;
            }

            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            Map<String, Set<String>> triples = new HashMap<>();
            while (r.ready()) {
                String triple = r.readLine();
                if (triple.equals("<empty result>")) {
                    continue;
                }
                String predicate = triple.substring(0, triple.indexOf(">") + 1);
                String value = triple.substring(triple.indexOf(">") + 1).trim();

                Set<String> sub_triples = triples.get(predicate);
                sub_triples = sub_triples == null ? new HashSet<>() : sub_triples;
                triples.put(predicate, sub_triples);

                sub_triples.add(value);
            }
            return triples;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void loadRDF3XEntityTriplesBulk(Set<String> entities, String btc_index, String rdf3x_engine, Map<String, Map<String, Set<String>>> triples) {
        try {
            FileUtils.checkDir("query_log");
            for (String entity : entities) {
                if (triples.containsKey(entity)) {
                    continue;
                }
                String sparql_query = "SELECT DISTINCT ?p ?o WHERE {<" + entity + "> ?p ?o}";
                String sparql_query_file = "query_log/" + Math.abs(entity.hashCode()) + "_query.txt";
                FileUtils.saveText(sparql_query, sparql_query_file);
                FileUtils.saveText(sparql_query + "\n", tag + "_query_log.txt", true);

                //execute the SPARQL query against the RDF3X index.
                String cmd_str = rdf3x_engine + " " + btc_index + " " + sparql_query_file;
                Process p = Runtime.getRuntime().exec(cmd_str);

                boolean rst = p.waitFor(30, TimeUnit.SECONDS);
                if (!rst) {
                    System.out.printf("Failed loading triples for entity %s \n", entity);
                    p.destroy();
                    continue;
                }

                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                Map<String, Set<String>> entity_triples = new HashMap<>();
                while (r.ready()) {
                    String triple = r.readLine();
                    if (triple.equals("<empty result>")) {
                        continue;
                    }
                    String predicate = triple.substring(0, triple.indexOf(">") + 1);
                    String value = triple.substring(triple.indexOf(">") + 1).trim();

                    Set<String> sub_triples = entity_triples.get(predicate);
                    sub_triples = sub_triples == null ? new HashSet<>() : sub_triples;
                    entity_triples.put(predicate, sub_triples);

                    sub_triples.add(value);
                }
                triples.put(entity, entity_triples);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * In case there are many entities, we use the following approach where we look into the specific type index files.
     *
     * @param type_index
     * @return
     */
    public static void loadRDF3XEntityTriples(Set<String> entities, String type_index, Map<String, Map<String, Set<String>>> entity_triples) {
        try {
            BufferedReader reader = FileUtils.getCompressedFileReader(type_index);
            while (reader.ready()) {
                String[] triple = reader.readLine().split("\t");
                if (triple.length < 3) {
                    continue;
                }
                if (!entities.contains(triple[0]) || entity_triples.containsKey(triple[0])) {
                    continue;
                }

                Map<String, Set<String>> sub_triple = entity_triples.get(triple[0]);
                sub_triple = sub_triple == null ? new HashMap<>() : sub_triple;
                entity_triples.put(triple[0], sub_triple);

                Set<String> object_values = sub_triple.get(triple[1]);
                object_values = object_values == null ? new HashSet<>() : object_values;
                sub_triple.put(triple[1], object_values);

                object_values.add(triple[2]);
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static Set<String> loadRDF3XEntityTripleValues(String entity, String btc_index, String rdf3x_engine, Set<String> predicates) throws IOException, InterruptedException {
        StringBuffer sb_query_tmp = new StringBuffer();
        int i = 0;
        for (String predicate : predicates) {
            if (i != 0) {
                sb_query_tmp.append(" UNION ");
            }
            i++;
            sb_query_tmp.append(" { ").append(entity).append(" ").append(predicate).append(" ?S } UNION { ").append(entity).append(" ?x ?y. ?y ").append(predicate).append(" ?S }");
        }
        String sparql_query = "SELECT DISTINCT ?S WHERE {" + sb_query_tmp.toString() + "}";
        FileUtils.saveText(sparql_query, "query_tmp.txt");
        FileUtils.saveText(sparql_query + "\n", tag + "_query_log.txt", true);

        //execute the SPARQL query against the RDF3X index.
        String cmd_str = rdf3x_engine + " " + btc_index + " query_tmp.txt";
        Process p = Runtime.getRuntime().exec(cmd_str);
        boolean rst = p.waitFor(60, TimeUnit.SECONDS);

        if (!rst) {
            System.out.printf("Failed loading triples for entity %s \n", entity);
            return null;
        }

        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
        Set<String> triples = new HashSet<>();
        while (r.ready()) {
            String triple = r.readLine();
            if (triple.equals("<empty result>")) {
                continue;
            }
            triples.add(triple);
        }
        if (triples.size() != 0) {
            System.out.printf("Finished loading triples for entity %s with %d triples.\n", entity, triples.size());
        }
        return triples;
    }

    /**
     * Load entity triples.
     *
     * @param entities
     * @param btc_index
     * @param rdf3x_engine
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static Map<String, Set<String>> loadRDF3XEntityTriples(Set<String> entities, String btc_index, String rdf3x_engine) throws IOException, InterruptedException {
        Set<String> literal_predicates = new HashSet<>();
        literal_predicates.add("<http://www.w3.org/2006/03/wn/wn20/schema/lexicalForm>");
        literal_predicates.add("<http://dbpedia.org/property/county>");
        literal_predicates.add("<http://www.daml.org/2003/02/fips55/fips-55-ont#name>");
        literal_predicates.add("<http://www.geonames.org/ontology#name>");
        literal_predicates.add("<http://www.aktors.org/ontology/portal#full-name>");
        literal_predicates.add("<http://dbpedia.org/property/wikiquoteProperty>");
        literal_predicates.add("<http://www.w3.org/2004/02/skos/core#prefLabel>");
        literal_predicates.add("<http://purl.org/dc/elements/1.1/title>");
        literal_predicates.add("<http://sw.opencyc.org/concept/Mx4rwLSVCpwpEbGdrcN5Y29ycA>");
        literal_predicates.add("<http://dbpedia.org/property/officialName>");

        Map<String, Set<String>> rst = new HashMap<>();
        for (String entity : entities) {
            Set<String> entity_triples = loadRDF3XEntityTripleValues(entity, btc_index, rdf3x_engine, literal_predicates);
            if (entity_triples == null) {
                continue;
            }
            rst.put(entity, entity_triples);
        }
        return rst;
    }

    /**
     * Load related entities based on the state of the art approach.
     *
     * @param entities
     * @param btc_index
     * @param rdf3x_engine
     * @param scope
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static Map<String, Set<String>> loadSOTRDF3XEntities(Set<String> entities, String btc_index, String rdf3x_engine, int scope) throws IOException, InterruptedException {
        String sparql_query_one = "SELECT DISTINCT ?x WHERE { " +
                "{ <ENTITY_URI> <http://www.w3.org/2002/07/owl#sameAs> ?x } UNION { ?x <http://www.w3.org/2002/07/owl#sameAs> <ENTITY_URI> } UNION" +
                "{ <ENTITY_URI> <http://dbpedia.org/property/disambiguates> ?x } UNION { ?x <http://dbpedia.org/property/redirect> <ENTITY_URI> } }";

        String sparql_query_two = "SELECT DISTINCT ?y WHERE { " +
                "{{ <ENTITY_URI> <http://dbpedia.org/property/wikilink> ?x . ?x <http://www.w3.org/2004/02/skos/core#subject> ?y } UNION {?x <http://dbpedia.org/property/wikilink> <ENTITY_URI>. ?x <http://www.w3.org/2004/02/skos/core#subject> ?y }} UNION" +
                "{{ <ENTITY_URI> <http://dbpedia.org/property/wikilink> ?w . ?w <http://www.w3.org/2002/07/owl#sameAs> ?y } UNION { ?w <http://dbpedia.org/property/wikilink> <ENTITY_URI> . ?w <http://www.w3.org/2002/07/owl#sameAs> ?y }}}";

        Map<String, Set<String>> related_entities = new HashMap<>();

        entities.parallelStream().forEach(entity -> {
            try {
                Set<String> triples = new HashSet<>();
                related_entities.put(entity, triples);

                String sparql_tmp = "";
                if (scope == 1) {
                    sparql_tmp = sparql_query_one.replaceAll("ENTITY_URI", entity);
                } else {
                    sparql_tmp = sparql_query_two.replaceAll("ENTITY_URI", entity);
                }
                String query_file = "query_log/" + Math.abs(entity.hashCode()) + "_qf.txt";
                FileUtils.saveText(sparql_tmp, query_file);
                FileUtils.saveText(sparql_tmp + "\n", tag + "_query_log.txt", true);
                //execute the SPARQL query against the RDF3X index.
                String cmd_str = rdf3x_engine + " " + btc_index + " " + query_file;
                Process p = Runtime.getRuntime().exec(cmd_str);

                boolean rst = p.waitFor(30, TimeUnit.SECONDS);
                if (!rst) {
                    p.destroy();
                    System.out.printf("Failed loading data for entity %s \n", entity);
                    return;
                }

                BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                while (r.ready()) {
                    String triple = r.readLine();
                    if (triple.equals("<empty result>")) {
                        continue;
                    }
                    triples.add(triple);
                }
                r.close();
                if (triples.size() != 0) {
                    System.out.printf("For entity %s loaded a set of %d entities at scope %d \n", entity, triples.size(), scope);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return related_entities;
    }

    /**
     * Add n-grams from a given array of textual content from an entity.
     *
     * @param label_terms
     * @param terms
     * @param l_t_counts
     * @param last_unigram_index
     * @param terms_dictionary
     * @param stop_words
     * @return
     */
    public static int addNGrams(String[] label_terms, THashMap<Integer, TIntIntHashMap> terms,
                                THashMap<Integer, TIntIntHashMap> l_t_counts, int last_unigram_index,
                                THashMap<Integer, THashMap<String, Integer>> terms_dictionary,
                                Set<String> stop_words, int ngrams) {
        for (int k = 1; k <= ngrams; k++) {
            TIntIntHashMap ngram_terms = terms.get(k);
            ngram_terms = ngram_terms == null ? new TIntIntHashMap() : ngram_terms;
            terms.put(k, ngram_terms);

            TIntIntHashMap counts = l_t_counts.get(k);
            THashMap<String, Integer> ngram_term_dictionary = terms_dictionary.get(k);
            for (int i = 0; i < label_terms.length; i += 1) {
                String term = getTerm(label_terms, i, i + k);
                if (k == 1 && stop_words.contains(term)) {
                    continue;
                }

                if (!ngram_term_dictionary.containsKey(term)) {
                    last_unigram_index++;
                    ngram_term_dictionary.put(term, last_unigram_index);
                }

                Integer unigram_count = counts.get(ngram_term_dictionary.get(term));
                unigram_count = unigram_count == null ? 0 : unigram_count;
                unigram_count += 1;
                counts.put(ngram_term_dictionary.get(term), unigram_count);

                //keep the current count of the unigram in the entity
                Integer entity_unigram_count = ngram_terms.get(ngram_term_dictionary.get(term));
                entity_unigram_count = entity_unigram_count == null ? 0 : entity_unigram_count;
                entity_unigram_count += 1;
                ngram_terms.put(ngram_term_dictionary.get(term), entity_unigram_count);
            }
        }
        return last_unigram_index;
    }


    /**
     * Get the string between a range of indices within a string array.
     *
     * @param terms
     * @param start_offset
     * @param end_offset
     * @return
     */
    private static String getTerm(String[] terms, int start_offset, int end_offset) {
        String term = "";
        if (start_offset < terms.length) {
            for (int i = start_offset; i < end_offset; i++) {
                if (i < terms.length) {
                    term = term + " " + terms[i];
                }
            }
        }
        return term.trim();
    }

    /**
     * Load the set of entities that belong to the same bins in the LSH clustering.
     *
     * @param lsh_bins
     * @return
     */
    public static THashMap<Integer, TIntHashSet> loadLSHEntityBinsSimple(String lsh_bins, int bin_filter) {
        BufferedReader reader = FileUtils.getCompressedFileReader(lsh_bins);

        //first read the entities along with their buckets for the given bin range
        THashMap<Integer, TIntHashSet> bin = new THashMap<>();
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    continue;
                }
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
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bin;
    }
}
