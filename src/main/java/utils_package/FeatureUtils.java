package utils_package;

import java.util.*;

/**
 * Created by besnik on 1/24/15.
 */
public class FeatureUtils {
    public static void main(String[] args) {
        String data_dir = args[0];
        String out_dir = args[1];
        String baseline_data = args[2];
        String tag = args[3];
        String queries = args[4];

        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(data_dir, files);

        //load the queries
        Map<String, String> query = FileUtils.readIntoStringMap(queries, "\t", false);
        //load the baseline entities
        Map<String, Map<String, Double>> baseline_entities = loadEntities(baseline_data);

        files.parallelStream().forEach(file -> {
            String file_tmp = file.substring(file.lastIndexOf("/") + 1);
            if (!file_tmp.startsWith(tag)) {
                return;
            }
            try {
                Map<String, Map<String, Double>> hybrid = loadEntities(file);

                StringBuffer sb = new StringBuffer();
                for (String qid : query.keySet()) {
                    //if our approach doesn't contain entities for the current query.
                    Map<Double, List<String>> ranked_entities = new TreeMap<>();
                    if(!hybrid.containsKey(qid) && !baseline_entities.containsKey(qid)){
                        continue;
                    }
                    if (!hybrid.containsKey(qid)) {
                        Map<String, Double> qid_baseline = baseline_entities.get(qid);
                        ranked_entities = rankedEntities(qid_baseline);
                    } else if (hybrid.get(qid).size() < 10) {
                        Map<String, Double> qid_entities = hybrid.get(qid);
                        Map<String, Double> qid_baseline = baseline_entities.get(qid);
                        ranked_entities = mergeAndRankEntities(qid_entities, qid_baseline);
                    } else {
                        Map<String, Double> qid_entities = hybrid.get(qid);

                        //get the ranked list of entities
                        ranked_entities = rankedEntities(qid_entities);
                    }

                    int rank = 1;
                    for (double score : ranked_entities.keySet()) {
                        for (String entity : ranked_entities.get(score)) {
                            double rank_score = 1 / (double)rank;
                            sb.append(qid).append("\t").append(entity).append("\t").append(rank_score).append("\n");
                            rank++;
                        }
                    }
                }
                String file_name = out_dir + "/" + file.substring(file.lastIndexOf("/") + 1);
                FileUtils.saveText(sb.toString(), file_name);
                System.out.printf("Finished processing file %s \n", file_name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static Map<Double, List<String>> mergeAndRankEntities(Map<String, Double> entities_a, Map<String, Double> entities_b) {
        Map<Double, List<String>> rst = new TreeMap<>();

        Set<Double> ranks = new HashSet<>(entities_b.values());
        double max = Collections.max(ranks);
        double min = Collections.min(ranks);


        for (String entity : entities_a.keySet()) {
            double value = entities_a.get(entity);
            List<String> sub_rst = rst.get(value);
            sub_rst = sub_rst == null ? new LinkedList<>() : sub_rst;
            rst.put(value, sub_rst);

            sub_rst.add(entity);
        }

        for (String entity : entities_b.keySet()) {
            double bs_score = 1 - ((entities_b.get(entity) - min) / (max - min));
            List<String> sub_rst = rst.get(bs_score);
            sub_rst = sub_rst == null ? new LinkedList<>() : sub_rst;
            rst.put(bs_score, sub_rst);

            sub_rst.add(entity);
        }

        return rst;
    }

    public static Map<Double, List<String>> rankedEntities(Map<String, Double> entities) {
        Map<Double, List<String>> rst = new TreeMap<>();

        for (String entity : entities.keySet()) {
            double value = entities.get(entity);
            List<String> sub_rst = rst.get(value);
            sub_rst = sub_rst == null ? new LinkedList<>() : sub_rst;
            rst.put(value, sub_rst);

            sub_rst.add(entity);
        }

        return rst;
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
}
