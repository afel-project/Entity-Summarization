package query;

import org.apache.commons.lang3.StringUtils;
import utils_package.FileUtils;
import utils_package.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by besnik on 1/16/15.
 */
public class StructuredRetrieval {
    public static void main(String[] args) throws IOException, InterruptedException {
        String baseline_entity_path = "", out_dir = "", btc_index = "", rdf3x_engine = "", queries = "", tag = "";

        tag = "default";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-baseline_entities")) {
                baseline_entity_path = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-btc")) {
                btc_index = args[++i];
            } else if (args[i].equals("-rdf3x")) {
                rdf3x_engine = args[++i];
            } else if (args[i].equals("-queries")) {
                queries = args[++i];
            } else if (args[i].equals("-tag")) {
                tag = args[++i];
            }
        }

        Utils.tag = tag;

        Map<String, String> query_data = FileUtils.readIntoStringMap(queries, "\t", false);
        Map<String, Map<String, Double>> baseline_entities = loadBaselineEntities(baseline_entity_path, "\t");

        Set<String> all_entities = new HashSet<>();
        baseline_entities.keySet().forEach(qid -> all_entities.addAll(baseline_entities.get(qid).keySet()));

        //load related entities at scope one and two
        System.out.printf("Loading entities at scope one.\n");
        Map<String, Set<String>> related_entities_scope_one = Utils.loadSOTRDF3XEntities(all_entities, btc_index, rdf3x_engine, 1);
        System.out.printf("Finished loading entities at scope one with %d entities.\n", related_entities_scope_one.size());
        System.out.printf("Loading entities at scope two.\n");
        Map<String, Set<String>> related_entities_scope_two = Utils.loadSOTRDF3XEntities(all_entities, btc_index, rdf3x_engine, 2);
        System.out.printf("Finished loading entities at scope two with  %d entities.\n", related_entities_scope_two.size());
        Set<String> entities_scope_one_two = new HashSet<>();
        related_entities_scope_one.keySet().stream().forEach(entity -> entities_scope_one_two.addAll(related_entities_scope_one.get(entity)));
        related_entities_scope_two.keySet().stream().forEach(entity -> entities_scope_one_two.addAll(related_entities_scope_two.get(entity)));

        System.out.printf("Loading triples for entities at scope one and two.\n");
        //load triples for entities
        Map<String, Set<String>> triples = Utils.loadRDF3XEntityTriples(entities_scope_one_two, btc_index, rdf3x_engine);
        System.out.printf("Finished loading entity triples for %d entities.\n", triples.size());

        System.out.printf("Computing entity ranking.\n");
        computeRelatedEntityRanking(baseline_entities, triples, related_entities_scope_one, out_dir + "/s1_", query_data, tag);
        computeRelatedEntityRanking(baseline_entities, triples, related_entities_scope_two, out_dir + "/s2_", query_data, tag);
        System.out.printf("Finished computing entity ranking.\n");
    }

    private static Map<String, Map<String, Double>> loadBaselineEntities(String baseline_entity_path, String s) {
        String[] lines = FileUtils.readText(baseline_entity_path).split("\n");
        Map<String, Map<String, Double>> rst = new HashMap<>();
        for (String line : lines) {
            String[] tmp = line.split(s);

            String qid = tmp[0];
            String entity = tmp[1];
            double bm25f = Double.valueOf(tmp[2]);

            Map<String, Double> sub_rst = rst.get(qid);
            sub_rst = sub_rst == null ? new HashMap<>() : sub_rst;
            rst.put(qid, sub_rst);
            sub_rst.put(entity, bm25f);
        }
        return rst;
    }


    /**
     * Compute the relatedness score between entities retrieved by the baseline approach and the ones that are suggested by the state of the art approach.
     *
     * @param baseline_entities
     * @param triples
     */
    public static void computeRelatedEntityRanking(Map<String, Map<String, Double>> baseline_entities,
                                                   Map<String, Set<String>> triples,
                                                   Map<String, Set<String>> related_entities,
                                                   String out_dir, Map<String, String> queries, String tag) {
        StringBuffer sb = new StringBuffer();
        final double lambda = 0.5;
        for (String qid : baseline_entities.keySet()) {
            String query = queries.get(qid);

            for (String bs_entity : baseline_entities.get(qid).keySet()) {
                if (!related_entities.containsKey(bs_entity)) {
                    continue;
                }

                for (String rt_entity : related_entities.get(bs_entity)) {
                    if (!triples.containsKey(rt_entity)) {
                        continue;
                    }

                    double avg_sim = 0.0;
                    for (String value : triples.get(rt_entity)) {
                        avg_sim += StringUtils.getJaroWinklerDistance(query.toLowerCase().trim(), value.toLowerCase().trim());
                    }
                    avg_sim /= triples.get(rt_entity).size();
                    avg_sim = Double.isInfinite(avg_sim) || Double.isNaN(avg_sim) ? 0.0 : avg_sim;
                    double final_score = lambda * baseline_entities.get(qid).get(bs_entity) + (1 - lambda) * avg_sim;

                    sb.append(qid).append("\t").append(rt_entity).append("\t").append(final_score).append("\n");
                }
            }
        }
        FileUtils.saveText(sb.toString(), out_dir + tag + "_entities.csv");
    }
}
