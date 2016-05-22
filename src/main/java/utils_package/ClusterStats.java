package utils_package;

import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by besnik on 1/22/15.
 */
public class ClusterStats {
    public static void main(String[] args) throws IOException {
        String lsh_index = "", cluster_dir = "", out_dir = "";

        boolean is_spectral = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-lsh_index")) {
                lsh_index = args[++i];
            } else if (args[i].equals("-clusters")) {
                cluster_dir = args[++i];
            } else if (args[i].equals("-out")) {
                out_dir = args[++i];
            } else if (args[i].equals("-spectral")) {
                is_spectral = Boolean.valueOf(args[++i]);
            }
        }

        Set<String> lsh_files = new HashSet<>();
        FileUtils.getFilesList(lsh_index, lsh_files);

        Set<String> cluster_files = new HashSet<>();
        FileUtils.getFilesList(cluster_dir, cluster_files);

        List<String> lines = new ArrayList<>();

        if (!FileUtils.fileExists(out_dir + "/lsh_stats.csv.gz", false)) {
            lsh_files.parallelStream().forEach(lsh_file -> {
                try {
                    System.out.printf("Finished processing file %s \n", lsh_file);
                    int start_index = lsh_file.lastIndexOf("/") + 1;
                    String type_name = lsh_file.substring(start_index, lsh_file.indexOf("_", start_index));
                    THashMap<Integer, TIntHashSet> bin = Utils.loadLSHEntityBinsSimple(lsh_file, 0);
                    double entity_count = 0;
                    StringBuffer sb = new StringBuffer();
                    for (int bin_id : bin.keySet()) {
                        entity_count += bin.get(bin_id).size();
                    }
                    sb.append(type_name).append("\t").append(bin.size()).append("\t").append(entity_count).append("\t").append((entity_count) / (double) bin.size()).append("\n");
                    lines.add(sb.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        StringBuffer sb = new StringBuffer();
        for(String line:lines){
            sb.append(line);
        }
        FileUtils.saveText(sb.toString(), out_dir + "/lsh_stats.csv.gz", false, true);

        String key_tag = is_spectral ? "spectral/" : "xmeans/";
        final String tag = is_spectral ? "spectral" : "xmeans";
        final boolean is_spectral_f = is_spectral;
        StringBuffer sb_tmp = new StringBuffer();
        sb_tmp.append("type\tbin\t#clusters\t#entities\t#avg_entities\n");
        cluster_files.forEach(cluster_file -> {
            try {
                int start_type_index = cluster_file.indexOf(key_tag) + key_tag.length();
                String type = cluster_file.substring(start_type_index, cluster_file.indexOf("/", start_type_index));

                int start_index = cluster_file.lastIndexOf("/") + 1;
                int end_index = cluster_file.indexOf("_", start_index);
                String lsh_bin = cluster_file.substring(start_index, end_index);

                THashMap<Integer, TIntHashSet> clusters = Utils.loadEntityCluster(cluster_file, is_spectral_f);
                if (clusters == null) {
                    return;
                }
                double count = 0;
                for (int cluster_id : clusters.keySet()) {
                    count += clusters.get(cluster_id).size();
                }

                double avg = (count) / ((double) clusters.size());
                avg = Double.isInfinite(avg) || Double.isNaN(avg) ? 0.0 : avg;

                sb_tmp.append(type).append("\t").append(lsh_bin).append("\t").append(count).append("\t").append(clusters.size()).append("\t").append(avg).append("\n");
                System.out.printf("Finished processing file %s \n", cluster_file);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        FileUtils.saveText(sb_tmp.toString(), out_dir + "/" + tag + "_stats.csv.gz", false, true);
    }


}
