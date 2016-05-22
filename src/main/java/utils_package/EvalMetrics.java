package utils_package;

import java.util.*;

/**
 * Created by besnik on 1/29/15.
 */
public class EvalMetrics {
    public static final double[] ranks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
//    public static final double[] ranks = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
//            21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50};

    public static void measureDiffPrecisionRecall(Set<String> methods, Map<String, Map<String, Map<Integer, Map.Entry<Double, Integer>>>> all_rst, String out_dir) {
        StringBuffer sb = new StringBuffer();
        sb.append("query");
        Map<String, Map<String, Map<Integer, Double>>> rst = new HashMap<>();
        for (String method : methods) {
            Map<String, Map<Integer, Double>> method_p = printPrecision(method, all_rst);

            for (String qid : method_p.keySet()) {
                Map<String, Map<Integer, Double>> qrst = rst.get(qid);
                qrst = qrst == null ? new HashMap<>() : qrst;
                rst.put(qid, qrst);

                Map<Integer, Double> mqrst = qrst.get(method);
                mqrst = mqrst == null ? new HashMap<>() : mqrst;
                qrst.put(method, mqrst);

                mqrst.putAll(method_p.get(qid));
            }

            sb.append("\t").append(method);
        }

        sb.append("\n");
        for (String qid : rst.keySet()) {
            sb.append(qid);
            for (String method : rst.get(qid).keySet()) {
                double avg_p = 0.0;
                for (int rank : rst.get(qid).get(method).keySet()) {
                    avg_p += rst.get(qid).get(method).get(rank);
                }
                double p_k = avg_p / rst.get(qid).get(method).size();
                sb.append("\t").append(p_k);
            }
            sb.append("\n");
        }

        FileUtils.saveText(sb.toString(), out_dir + "/query_method_improvement.txt");
    }


    public static void computeEntityHistograms(String gt_file, String methods_file, String out_dir) {
        Map<String, Map<String, Map<Integer, Integer>>> histogram = new HashMap<>();
        Map<String, Map<String, Double>> gt = loadEntities(gt_file);

        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(methods_file, files);

        Set<String> methods = new HashSet<>();
        Set<String> qids = new HashSet<>();

        for (String file : files) {
            if (!(file.contains("title") || file.contains("body") || file.contains("t_xm_51") || file.contains("b_xm_51") || file.contains("t_sp_51") || file.contains("b_sp_51"))) {
                continue;
            }
            String file_name = file.substring(file.lastIndexOf("/") + 1);
            file_name = file_name.substring(0, file_name.lastIndexOf("."));
            file_name = file_name.replace("_hybrid_ranked_entities", "").replace("_entities_sorted", "");
            Map<String, Map<String, Double>> data = loadApproachEntities(file);
            methods.add(file_name);

            for (String qid : data.keySet()) {
                if (file_name.contains("xm") || file_name.contains("sp")) {
                    qids.add(qid);
                }
                if (!gt.containsKey(qid)) {
                    continue;
                }
                Map<String, Map<Integer, Integer>> sub_hist = histogram.get(qid);
                sub_hist = sub_hist == null ? new HashMap<>() : sub_hist;
                histogram.put(qid, sub_hist);

                Map<Integer, Integer> method_hist = sub_hist.get(file_name);
                method_hist = method_hist == null ? new HashMap<>() : method_hist;
                sub_hist.put(file_name, method_hist);

                for (String entity : data.get(qid).keySet()) {
                    double score = gt.get(qid).containsKey(entity) ? gt.get(qid).get(entity) : 1;

                    if (score < 2) {
                        score = 1;
                    } else if (score >= 2 & score < 3) {
                        score = 2;
                    } else if (score >= 3 & score < 4) {
                        score = 3;
                    } else if (score >= 4 & score < 5) {
                        score = 4;
                    } else {
                        score = 5;
                    }

                    Integer count = method_hist.get((int) score);
                    count = count == null ? 0 : count;
                    count += 1;
                    method_hist.put((int) score, count);
                }
            }
        }

        StringBuffer sb = new StringBuffer();
        sb.append("relevance\t").append(methods.toString().replaceAll("\\[|\\]", "").replaceAll(", ", "\t")).append("\n");

        for (int k = 1; k <= 5; k++) {
            sb.append(k).append("\t");
            for (String method : methods) {
                int sum_k = 0;
                for (String qid : qids) {
                    sum_k += histogram.get(qid).containsKey(method) && histogram.get(qid).get(method).containsKey(k) ? histogram.get(qid).get(method).get(k) : 0;
                }
                sb.append(sum_k).append("\t");
            }
            sb.append("\n");
        }
        FileUtils.saveText(sb.toString(), out_dir + "/entity_histogram.txt");
    }

    public static void main(String[] args) {
        String data_dir = "/Users/besnik/Documents/L3S/iswc2015_aor/data/retrieval_entities/body/";
        String ground_truth = "/Users/besnik/Documents/L3S/iswc2015_aor/data/ground_truth/semsearch_groundtruth.txt";

        String out_dir = "/Users/besnik/Desktop/";


        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(data_dir, files);

        Set<String> method_array = new TreeSet<>();

        //ground truth
        Map<String, Map<String, Double>> gt = loadEntities(ground_truth);
        Map<String, Integer> gt_counts = new HashMap<>();
        for (String qid : gt.keySet()) {
            int total = 0;
            for (String entity : gt.get(qid).keySet()) {
                if (gt.get(qid).get(entity) >= 2) {
                    total++;
                }
            }
            gt_counts.put(qid, total);
        }

        Map<String, Map<String, Map<Integer, Map.Entry<Double, Integer>>>> all_rst = new HashMap<>();
        Map<String, Map<String, Map<String, Double>>> all_approches_data = new HashMap<>();
        for (String file : files) {
            String file_name = file.substring(file.lastIndexOf("/") + 1);
            file_name = file_name.substring(0, file_name.lastIndexOf("."));
            file_name = file_name.replace("_hybrid_ranked_entities", "").replace("_entities_sorted", "");
            file_name = file_name.replace("b_", "");
            file_name = file_name.replace("body_", "");

            Map<String, Map<String, Double>> data = loadApproachEntities(file);
            all_approches_data.put(file_name, data);

            Map<String, Map<Integer, Map.Entry<Double, Integer>>> rst = new HashMap<>();
            for (String qid : data.keySet()) {
                Map<Integer, Map.Entry<Double, Integer>> q_rst = computePrecisionRecall(data.get(qid), gt.get(qid));
                rst.put(qid, q_rst);
            }
            all_rst.put(file_name, rst);
            method_array.add(file_name);
        }

        Map<String, Map<String, Map<Integer, Integer>>> rel_at_k = getRelevantEntitiesAtK(all_rst);
        Map<String, Map<Integer, Double>> recall = printRecall(rel_at_k, gt);
        Map<String, Map<Integer, Double>> precision = printPrecision(all_rst);

        StringBuffer sb = new StringBuffer();
        StringBuffer sb_avg = new StringBuffer();

        sb.append("method\trank\tP@k\tR@k\n");
        sb_avg.append("method\tMAP\tAvgR\n");

        for (String method : precision.keySet()) {
            double avg_recall = 0.0, map = 0.0;
            for (int rank : precision.get(method).keySet()) {
                double recall_k = recall.get(method).get(rank);
                double precision_k = precision.get(method).get(rank);

                sb.append(method).append("\t").append(rank).append("\t").append(precision_k).append("\t").append(recall_k).append("\n");
                avg_recall += recall_k;
                map += precision_k;
            }

            sb_avg.append(method).append("\t").append(map / precision.get(method).size()).append("\t").append(avg_recall / recall.get(method).size()).append("\n");
        }
        FileUtils.saveText(sb.toString(), out_dir + "/precision_recall.txt");
        FileUtils.saveText(sb_avg.toString(), out_dir + "/avg_precision_recall.txt");
        FileUtils.saveText(printNDCGData(rel_at_k, gt_counts), out_dir + "/ndcg_scores.txt");

        System.out.println(sb_avg.toString());
        measureDiffPrecisionRecall(method_array, all_rst, out_dir);
    }

    private static String printNDCGData(Map<String, Map<String, Map<Integer, Integer>>> rel_at_k, Map<String, Integer> qid_count) {
        StringBuffer sb = new StringBuffer();
        Map<String, Map<Integer, List<Double>>> all_ndcg = new TreeMap<>();
        for (String method : rel_at_k.keySet()) {
            Map<Integer, List<Double>> ndcg = new HashMap<>();
            all_ndcg.put(method, ndcg);
            for (String qid : rel_at_k.get(method).keySet()) {
                if (!qid_count.containsKey(qid)) {
                    continue;
                }

                int rel_items = qid_count.get(qid);
                for (int rank : rel_at_k.get(method).get(qid).keySet()) {
                    if (rank > rel_items) {
                        continue;
                    }
                    List<Double> sub_ndcg = ndcg.get(rank);
                    sub_ndcg = sub_ndcg == null ? new ArrayList<>() : sub_ndcg;
                    ndcg.put(rank, sub_ndcg);

                    int rel_at_rank_items = rel_at_k.get(method).get(qid).get(rank);
                    if (rank == 1) {
                        sub_ndcg.add((double) rel_at_rank_items);
                        continue;
                    }

                    double ndcg_score = rel_at_k.get(method).get(qid).get(1);
                    for (int rank_tmp : rel_at_k.get(method).get(qid).keySet()) {
                        if (rank_tmp <= rank && rank_tmp >= 2) {
                            int rel_at_rank_items_tmp = rel_at_k.get(method).get(qid).get(rank_tmp);
                            ndcg_score += rel_at_rank_items_tmp / (Math.log(rank_tmp) / Math.log(2));
                        }
                    }
                    ndcg_score /= idealDCG(rank);
                    sub_ndcg.add(ndcg_score);
                }
            }
        }
        String[] methods = new String[all_ndcg.size()];
        all_ndcg.keySet().toArray(methods);

        sb.append("rank\t");
        for (int i = 0; i < methods.length; i++) {
            sb.append(methods[i]).append("\t");
        }
        sb.append("\n");

        for (double rank_tmp : ranks) {
            int rank = (int) rank_tmp;
            sb.append("NDCG@").append(rank).append("\t");
            for (String method : methods) {
                double ndcg_score = all_ndcg.get(method).containsKey(rank) ? all_ndcg.get(method).get(rank).stream().mapToDouble(x -> x).average().getAsDouble() : 0.0;
                sb.append(ndcg_score).append("\t");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static double idealDCG(int k) {
        double score = 0;
        if (k == 1) {
            score = 1.0;
        } else {
            score += 1;
            for (int i = 2; i <= k; i++) {
                score += i / (Math.log(i) / Math.log(2));
            }
        }
        return score;
    }

    public static Map<String, Map<Integer, Double>> printPrecision(Map<String, Map<String, Map<Integer, Map.Entry<Double, Integer>>>> all_rst) {
        Map<String, Map<Integer, Double>> method_precision = new HashMap<>();

        for (String method : all_rst.keySet()) {
            Map<Integer, Double> rel = new HashMap<>();
            Map<Integer, Integer> rel_qid = new HashMap<>();
            for (String qid : all_rst.get(method).keySet()) {
                for (int rank : all_rst.get(method).get(qid).keySet()) {
                    Double rel_rank = rel.get(rank);
                    rel_rank = rel_rank == null ? 0 : rel_rank;
                    rel_rank += all_rst.get(method).get(qid).get(rank).getKey();
                    rel.put(rank, rel_rank);

                    Integer sub_rel_qid = rel_qid.get(rank);
                    sub_rel_qid = sub_rel_qid == null ? 0 : sub_rel_qid;
                    sub_rel_qid += 1;
                    rel_qid.put(rank, sub_rel_qid);
                }
            }

            Map<Integer, Double> sub_prec = new HashMap<>();
            method_precision.put(method, sub_prec);
            //output the values
            for (int rank : rel.keySet()) {
                double p_k = rel.get(rank) / rel_qid.get(rank);
                sub_prec.put(rank, p_k);
            }
        }
        return method_precision;
    }

    public static Map<String, Map<Integer, Double>> printPrecision(String method, Map<String, Map<String, Map<Integer, Map.Entry<Double, Integer>>>> all_rst) {
        Map<String, Map<Integer, Double>> method_precision = new HashMap<>();

        for (String qid : all_rst.get(method).keySet()) {
            Map<Integer, Double> sub_prec = new HashMap<>();
            method_precision.put(qid, sub_prec);

            for (int rank : all_rst.get(method).get(qid).keySet()) {
                Double rel_rank = all_rst.get(method).get(qid).get(rank).getKey();
                sub_prec.put(rank, rel_rank);
            }
        }
        return method_precision;
    }


    public static Map<String, Map<Integer, Double>> printRecall(Map<String, Map<String, Map<Integer, Integer>>> rel_k, Map<String, Map<String, Double>> gt) {
        Map<String, Map<Integer, List<Double>>> recall = new HashMap<>();
        Map<String, Integer> gt_total_rel = new HashMap<>();
        for (String qid : gt.keySet()) {
            int total = 0;
            for (String entity : gt.get(qid).keySet()) {
                if (gt.get(qid).get(entity) >= 2) {
                    total++;
                }
            }
            gt_total_rel.put(qid, total);
        }


        for (String method : rel_k.keySet()) {
            Map<Integer, List<Double>> sub_recall = new HashMap<>();
            recall.put(method, sub_recall);

            for (String qid : rel_k.get(method).keySet()) {
                if (!gt.containsKey(qid)) {
                    continue;
                }
                for (int rank : rel_k.get(method).get(qid).keySet()) {
                    int rel_entities = rel_k.get(method).get(qid).get(rank);

                    List<Double> rank_recall = sub_recall.get(rank);
                    rank_recall = rank_recall == null ? new ArrayList<>() : rank_recall;
                    sub_recall.put(rank, rank_recall);

                    double recall_at_k = rel_entities / (double) gt_total_rel.get(qid);
                    rank_recall.add(recall_at_k);
                }
            }
        }
        Map<String, Map<Integer, Double>> tmp_recall = new HashMap<>();
        for (String qid : recall.keySet()) {
            Map<Integer, Double> sub_recall = new HashMap<>();
            tmp_recall.put(qid, sub_recall);

            for (int rank : recall.get(qid).keySet()) {
                double avg_recall_at_k = recall.get(qid).get(rank).stream().mapToDouble(x -> x).average().getAsDouble();
                sub_recall.put(rank, avg_recall_at_k);
            }
        }
        return tmp_recall;
    }

    public static Map<String, Map<String, Map<Integer, Integer>>> getRelevantEntitiesAtK(Map<String, Map<String, Map<Integer, Map.Entry<Double, Integer>>>> all_rst) {
        Map<String, Map<String, Map<Integer, Integer>>> rel_at_rank = new TreeMap<>();
        for (String method : all_rst.keySet()) {
            Map<String, Map<Integer, Integer>> qid_rel_at_rank = rel_at_rank.get(method);
            qid_rel_at_rank = qid_rel_at_rank == null ? new HashMap<>() : qid_rel_at_rank;
            rel_at_rank.put(method, qid_rel_at_rank);

            for (String qid : all_rst.get(method).keySet()) {
                Map<Integer, Integer> m_q_rel = qid_rel_at_rank.get(qid);
                m_q_rel = m_q_rel == null ? new HashMap<>() : m_q_rel;
                qid_rel_at_rank.put(qid, m_q_rel);

                for (int rank : all_rst.get(method).get(qid).keySet()) {
                    Integer rel_total = m_q_rel.get(rank);
                    rel_total = rel_total == null ? 0 : rel_total;
                    rel_total += all_rst.get(method).get(qid).get(rank).getValue();
                    m_q_rel.put(rank, rel_total);
                }
            }
        }
        return rel_at_rank;
    }

    public static Map<Integer, Map.Entry<Double, Integer>> computePrecisionRecall(Map<String, Double> data, Map<String, Double> gt) {
        Map<Integer, Map.Entry<Double, Integer>> rst = new HashMap<>();

        for (int i = 0; i < ranks.length; i++) {
            double rank = ranks[i];
            int relevant = 0;
            int total = 0;
            for (String entity : data.keySet()) {
                double score = data.get(entity);
                if (score <= rank) {
                    if (gt != null && gt.containsKey(entity) && gt.get(entity) >= 2) {
                        relevant++;
                    }
                    total++;
                }
            }

            double p_at_k = relevant / (double) total;
            AbstractMap.SimpleEntry<Double, Integer> entry = new AbstractMap.SimpleEntry<>(p_at_k, relevant);
            rst.put(i + 1, entry);
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
            if (tmp.length != 3) {
                continue;
            }
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
    private static Map<String, Map<String, Double>> loadApproachEntities(String data) {
        String[] lines = FileUtils.readText(data).split("\n");
        Map<String, LinkedHashSet<String>> rst = new HashMap<>();

        for (String line : lines) {
            String[] tmp = line.split("\t");
            if (tmp.length != 3) {
                continue;
            }
            String qid = tmp[0];
            String entity = tmp[1];

            LinkedHashSet<String> sub_rst = rst.get(qid);
            sub_rst = sub_rst == null ? new LinkedHashSet<>() : sub_rst;
            rst.put(qid, sub_rst);

            if (sub_rst.size() == 10) {
                continue;
            }

            sub_rst.add(entity);
        }

        Map<String, Map<String, Double>> tmp_rst = new HashMap<>();
        for (String qid : rst.keySet()) {
            Map<String, Double> sub_tmp = new TreeMap<>();
            tmp_rst.put(qid, sub_tmp);
            int i = 0;
            for (String entity : rst.get(qid)) {
                double rank_score = (i + 1);
                sub_tmp.put(entity, rank_score);
                i++;
            }
        }
        return tmp_rst;
    }
}
