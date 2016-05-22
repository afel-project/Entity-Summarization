package utils_package;


import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.util.*;

/**
 * Created by besnik on 12/8/14.
 */
public class Testing {
    public static void parseGT() throws SQLException {
        Set<String> tables = FileUtils.readIntoSet("/Users/besnik/Documents/L3S/iswc2015_aor/data/retrieval_entities/entity_tables.txt", "\n", false);
        for (String table : tables) {
            System.out.println("Processing table " + table);
            PreparedStatement prep = getMySQLConnection().prepareStatement("SELECT qid, ent, score FROM " + table);
            ResultSet rst = prep.executeQuery();

            StringBuffer sb = new StringBuffer();
            while (rst.next()) {
                String qid = rst.getString("qid");
                String ent = rst.getString("ent");
                double score = rst.getDouble("score");

                sb.append(qid).append("\t").append(ent).append("\t").append(score).append("\n");
            }

            FileUtils.saveText(sb.toString(), "/Users/besnik/Documents/L3S/iswc2015_aor/data/retrieval_entities/" + table + ".txt");
        }
    }

    public static Connection getMySQLConnection() {
        Connection conn = null;
        if (conn == null) {
            String dbURL = "jdbc:mysql://localhost/sigir2015_eval?useUnicode=true&characterEncoding=utf-8";

            try {
                Class.forName("com.mysql.jdbc.Driver");
                conn = DriverManager.getConnection(dbURL, "root", "root123");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return conn;
        }
        return conn;
    }

    public static void main(String[] args) throws IOException, ParseException, InterruptedException, SQLException {
        String file = "/Users/besnik/Documents/L3S/iswc2015_aor/data/retrieval/q_imp_title.txt";
        Map<String, String> qtypes = FileUtils.readIntoStringMap("/Users/besnik/Documents/L3S/iswc2015_aor/data/query_types.txt", "\t", false);
        String[] lines = FileUtils.readText(file).split("\n");

        Map<String, Map<String, List<Double>>> query_improv = new HashMap<>();
        for (int i = 1; i < lines.length; i++) {
            String[] tmp = lines[i].split("\t");
            if (tmp.length != 5) {
                continue;
            }
            String qid = tmp[0], qtype = qtypes.get(qid);
            double bm25 = tmp[1].trim().isEmpty() ? 0 : Double.valueOf(tmp[1]), s1 = tmp[2].trim().isEmpty() ? 0 : Double.valueOf(tmp[2]), xm = tmp[3].trim().isEmpty() ? 0 : Double.valueOf(tmp[3]), sc = tmp[4].trim().isEmpty() ? 0 : Double.valueOf(tmp[4]);

            Map<String, List<Double>> qtype_sub = query_improv.get(qtype);
            qtype_sub = qtype_sub == null ? new HashMap<>() : qtype_sub;
            query_improv.put(qtype, qtype_sub);

            List<Double> bm25f_method_sub = qtype_sub.get("bm25f");
            bm25f_method_sub = bm25f_method_sub == null ? new ArrayList<>() : bm25f_method_sub;
            qtype_sub.put("bm25f", bm25f_method_sub);
            bm25f_method_sub.add(bm25);

            List<Double> s1_method_sub = qtype_sub.get("s1");
            s1_method_sub = s1_method_sub == null ? new ArrayList<>() : s1_method_sub;
            qtype_sub.put("s1", s1_method_sub);
            s1_method_sub.add(s1);

            List<Double> xm_method_sub = qtype_sub.get("xm");
            xm_method_sub = xm_method_sub == null ? new ArrayList<>() : xm_method_sub;
            qtype_sub.put("xm", xm_method_sub);
            xm_method_sub.add(xm);

            List<Double> sc_method_sub = qtype_sub.get("sc");
            sc_method_sub = sc_method_sub == null ? new ArrayList<>() : sc_method_sub;
            qtype_sub.put("sc", sc_method_sub);
            sc_method_sub.add(sc);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("qtype\tbm25f\ts1\txm\tsc\n");
        String[] methods = {"bm25f", "s1", "xm", "sc"};

        for (String qtype : query_improv.keySet()) {
            sb.append(qtype).append("\t");
            for (String method : methods) {
                double avg = query_improv.get(qtype).get(method).stream().mapToDouble(x -> x).average().getAsDouble();
                sb.append(avg).append("\t");
            }
            sb.append("\n");
        }
        System.out.println(sb.toString());
    }

    /**
     * Return the entity types.
     *
     * @return
     */
    public static Set<String> getEntityTypes(String entity) {
        try {
            String query = "SELECT DISTINCT ?type WHERE {{<" + entity + "> <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?type} UNION {?type <http://www.w3.org/2000/01/rdf-schema#subClassOf> <" + entity + ">}}";
            QueryEngineHTTP qt = new QueryEngineHTTP("http://dbpedia-live.openlinksw.com/sparql", query);
            Set<String> types = new HashSet<>();
            com.hp.hpl.jena.query.ResultSet rst = qt.execSelect();
            while (rst.hasNext()) {
                types.add(rst.next().get("?type").toString());
            }
            return types;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static Map<String, Integer> readMap(String file) {
        Map<String, Integer> rst = new HashMap<>();
        String[] lines = FileUtils.readText(file).split("\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String[] tmp = line.split("\t");
            rst.put(tmp[1], Integer.valueOf(tmp[0]));
        }
        return rst;
    }

    private static Map<Integer, String> readMap1(String file) {
        Map<Integer, String> rst = new HashMap<>();
        String[] lines = FileUtils.readText(file).split("\n");
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            String[] tmp = line.split("\t");
            rst.put(Integer.valueOf(tmp[0]), tmp[1]);
        }
        return rst;
    }

    private static void queryAnalysis(String query_file, String entity_types) {
        Map<String, Set<String>> entity_types_set = FileUtils.readMapSet(entity_types, "\t");
        String[] lines = FileUtils.readText(query_file).split("\n");

        Map<String, Map<String, Integer>> query_types = new HashMap<>();
        Map<String, String> qids = new HashMap<>();
        for (String line : lines) {
            String[] tmp = line.split("\t");

            String qid = tmp[0];
            String query = tmp[1];
            int rel = Integer.valueOf(tmp[3]);
            String entity = "<" + tmp[2] + ">";

            if (rel == 0 || !entity_types_set.containsKey(entity)) {
                continue;
            }

            Map<String, Integer> types = query_types.get(query);
            types = types == null ? new HashMap<>() : types;
            query_types.put(query, types);

            for (String type : entity_types_set.get(entity)) {
                Integer count = types.get(type);
                count = count == null ? 0 : count;

                count += 1;
                types.put(type, count);
            }


            qids.put(query, qid);
        }

        StringBuffer sb = new StringBuffer();
        StringBuffer sb1 = new StringBuffer();
        for (String query : query_types.keySet()) {
            Set<String> query_ned_spots = new HashSet<>();
            Map<String, Set<String>> query_entities = Utils.disambiguateQueryTerms(query, 0.2, query_ned_spots);

            String qid = qids.get(query);
            for (String type : query_types.get(query).keySet()) {
                int count = query_types.get(query).get(type);
                sb.append(qid).append("\t").append(query).append("\t").append(type).append("\t").append(count).append("\n");
            }

            Set<String> all_types = new HashSet<>();
            for (String entity : query_entities.keySet()) {
                all_types.addAll(query_entities.get(entity));
            }

            Set<String> common = new HashSet<>(all_types);
            common.retainAll(query_types.get(query).keySet());
            double type_overlap_score = common.size() / (double) (all_types.size() + query_types.get(query).size() - common.size());
            sb1.append(qid).append("\t").append(type_overlap_score).append("\t").append(all_types).append("\t").append(query_types.get(query)).append("\n");
        }
        FileUtils.saveText(sb.toString(), "query_types.csv", true, false);
        FileUtils.saveText(sb1.toString(), "query_types_affinity.csv", true, false);
    }
}
