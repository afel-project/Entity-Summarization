package query;

import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import utils_package.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by besnik on 11/24/14.
 */
public class QueryAnalysis {
    public static void main(String[] args) throws Exception {
        String operation = "", btc_data = "", out_dir = "", data_file = "";
        Map<String, String> queries = null;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-btc_data")) {
                btc_data = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-data_file")) {
                data_file = args[++i];
            } else if (args[i].equals("-queries")) {
                queries = FileUtils.readIntoStringMap(args[++i], "\t", false);
            }
        }

        QueryAnalysis qa = new QueryAnalysis();
        if (operation.equals("query_analysis")) {
            String[] lines = FileUtils.readText(data_file).split("\n");
            Map<String, Map<String, Integer>> entity_gt = new HashMap<>();

            Map<String, String> entities = new HashMap<>();
            for (String line : lines) {
                String[] tmp = line.split("\t");
                String qid = tmp[0];
                String entity = tmp[2].trim();
                int relevance = Integer.valueOf(tmp[3]);

                Map<String, Integer> sub_entity_gt = entity_gt.get(qid);
                sub_entity_gt = sub_entity_gt == null ? new HashMap<>() : sub_entity_gt;
                entity_gt.put(qid, sub_entity_gt);
                sub_entity_gt.put(entity, relevance);
                entities.put(entity, "N/A");
            }

            entities = qa.loadEntityTypes(btc_data, entities);
            //output the different entity types for the different queries.
            StringBuffer sb = new StringBuffer();
            for (String qid : entity_gt.keySet()) {
                for (String entity : entity_gt.get(qid).keySet()) {
                    int relevance = entity_gt.get(qid).get(entity);
                    String type = entities.get(entity);
                    sb.append(qid).append("\t").append(queries.get(qid)).append("\t").append(entity).append("\t").append(type).append("\t").append(relevance).append("\n");
                }
            }
            FileUtils.saveText(sb.toString(), out_dir + "/entity_ground_truth.csv", false, false);
        }
    }

    /**
     * Load the entity types for the query result set from the SemSearch 2010 dataset and the BTC2010.
     *
     * @param btc_data
     * @param entities
     * @return
     * @throws java.io.IOException
     */
    private Map<String, String> loadEntityTypes(String btc_data, Map<String, String> entities) throws IOException {
        //extract the entity types for the different queries.
        Set<String> btc_files = new HashSet<>();
        FileUtils.getFilesList(btc_data, btc_files);
        int entities_found = 0;
        for (String btc_file : btc_files) {
            try {
                System.out.printf("Processing file: %s.\n", btc_file);

                BufferedReader reader = FileUtils.getCompressedFileReader(btc_file);
                while (reader.ready()) {
                    if (entities_found >= entities.size()) {
                        break;
                    }

                    Node[] triple = NxParser.parseNodes(reader.readLine());
                    String entity = triple[0].toString().trim();
                    String predicate = triple[1].toString().trim();
                    String type = triple[2].toString().trim();
                    if (!entities.containsKey(entity) || !predicate.equals("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                        continue;
                    }

                    entities.put(entity, type);
                    entities_found++;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return entities;
    }
}
