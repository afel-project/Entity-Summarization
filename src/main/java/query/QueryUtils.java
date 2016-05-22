package query;

import utils_package.FileUtils;

import java.util.*;

/**
 * Created by besnik on 1/13/15.
 */
public class QueryUtils {
    public static void main(String[] args) {
        String query_results = "/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/query_results.csv";
        String query_mappings = "/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/query_type_mapping.csv";

        String[] tmp_lines = FileUtils.readText(query_mappings).split("\n");
        Map<String, Set<String>> query_map = new HashMap<>();
        for (String line : tmp_lines) {
            String[] tmp = line.split("\t");
            String type = tmp[0];
            String[] queries = tmp[1].split(", ");
            Set<String> sub_query_map = new HashSet<>();
            for (int i = 0; i < queries.length; i++) {
                sub_query_map.add(queries[i]);
            }
            query_map.put(type, sub_query_map);
        }

        Map<String, Set<String>> filtered_types = FileUtils.readMapSet("/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/filtered_types.csv", "\t");

        Set<String> all_types = new HashSet<>();
        String[] lines = FileUtils.readText(query_results).split("\n");

        Map<String, Map<String, Integer>> results = new HashMap<>();
        Map<String, Integer> type_counts = new HashMap<>();
        for (String line : lines) {
            String[] tmp = line.split(",");
            if (tmp.length != 4) {
                continue;
            }
            String qid = tmp[0];
            String type = tmp[2];
            int count = Integer.valueOf(tmp[3]);

            all_types.add(type);

            Integer tmp_type_count = type_counts.get(type);
            tmp_type_count = tmp_type_count == null ? 0 : tmp_type_count;
            tmp_type_count += count;
            type_counts.put(type, tmp_type_count);

            //get the type of the query
            String query_tmp_id = null;
            for (String type_1 : query_map.keySet()) {
                if (query_map.get(type_1).contains(qid)) {
                    query_tmp_id = type_1;
                    break;
                }
            }
            if (query_tmp_id == null) {
                continue;
            }
            Map<String, Integer> sub_rst = results.get(query_tmp_id);
            sub_rst = sub_rst == null ? new HashMap<>() : sub_rst;
            results.put(query_tmp_id, sub_rst);

            Integer tmp_count = sub_rst.get(type);
            tmp_count = tmp_count == null ? 0 : tmp_count;
            tmp_count += count;
            sub_rst.put(type, tmp_count);
        }

        // output as a matrix the result
        StringBuffer sb = new StringBuffer();
        int line_counter = 0;
        for (String query_id : results.keySet()) {
            int counter = 0;
            if (line_counter != 0) {
                sb.append("\n");
            }

            for(String type_aggr:filtered_types.keySet()){
                if(counter != 0){
                    sb.append("\t");
                }

                int type_count_sum = 0;
                for(String type:filtered_types.get(type_aggr)){
                    type_count_sum += results.get(query_id).containsKey(type) ? results.get(query_id).get(type) : 0;
                }
                double avg = type_count_sum / (double)filtered_types.get(type_aggr).size();
                sb.append(avg);
                counter ++;
            }
            line_counter++;
        }
        FileUtils.saveText(sb.toString(), "/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/query_affinity_map.csv");

        StringBuffer x_axis = new StringBuffer();
        int counter = 1;
        for (String type : filtered_types.keySet()) {
            x_axis.append("\"").append(type).append("\" ").append(counter).append(",");
            counter++;
        }
        FileUtils.saveText(x_axis.toString(), "/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/x_axis.txt");

        StringBuffer y_axis = new StringBuffer();
        counter = 1;
        for (String qid : results.keySet()) {
            y_axis.append("\"").append(qid).append("\" ").append(counter).append(",");
            counter++;
        }
        FileUtils.saveText(y_axis.toString(), "/Users/besnik/Documents/L3S/www2015_aor/data/query_affinity/y_axis.txt");
    }
}
