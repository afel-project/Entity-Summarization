package clustering;

import clustering.lsh.LocalitySentitiveHashing;
import gnu.trove.map.hash.THashMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.set.hash.THashSet;
import utils_package.FileUtils;
import utils_package.Utils;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by besnik on 11/23/14.
 */
public class LSHClustering {
    public static void main(String[] args) {
        String operation = "", out_dir = "", data_file = "";

        int sample_size = 100;

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-data_file")) {
                data_file = args[++i];
            } else if (args[i].equals("-sample")) {
                sample_size = Integer.valueOf(args[++i]);
            }
        }

        LSHClustering lsh = new LSHClustering();
        if (operation.equals("lsh")) {
            //compute the LSH of the given entity type
            Set<String> files = new HashSet<>();
            FileUtils.getDirList(data_file, files);

            final String data_file_f = data_file;
            final String out_dir_f = out_dir;
            final int sample_size_f = sample_size;
            files.parallelStream().forEach(type -> {
                if (FileUtils.fileExists(out_dir_f + "/" + type + "_LSH_clusters.csv.gz", false)) {
                    return;
                }
                THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text = (THashMap<Integer, THashMap<Integer, TIntIntHashMap>>) FileUtils.readObject(data_file_f + "/" + type + "/entity_text.obj");
                THashMap<String, THashMap<Integer, THashMap<String, Integer>>> terms_dictionary = (THashMap<String, THashMap<Integer, THashMap<String, Integer>>>) FileUtils.readObject(data_file_f + "/" + type + "/dict_terms.obj");
                //compute the LSH clusters.
                lsh.performLSH(terms_dictionary, entity_text, out_dir_f, type, sample_size_f);
            });
        }
    }

    /**
     * Cluster into buckets entities through Local Sensitive Hashing approach.
     *
     * @param terms_dictionary
     * @param entity_text
     * @param out_dir
     * @param type
     */
    private void performLSH(THashMap<String, THashMap<Integer, THashMap<String, Integer>>> terms_dictionary,
                            THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text,
                            String out_dir, String type, int sample_size) {

        LocalitySentitiveHashing lsh = new LocalitySentitiveHashing(100, 50);
        lsh.out_file = out_dir + "/" + type + "_LSH_clusters.csv.gz";

        THashMap<Integer, String> text_terms = new THashMap<>();

        if (terms_dictionary == null || entity_text == null) {
            return;
        }
        //add all possible terms into one data structure.
        for (int ngram : terms_dictionary.get("text").keySet()) {
            for (String term : terms_dictionary.get("text").get(ngram).keySet()) {
                int term_id = terms_dictionary.get("text").get(ngram).get(term);
                text_terms.put(term_id, term);
            }
        }
        if (sample_size != 100) {
            Set<Integer> entity_indices = Utils.getEntityIndicesSampling(entity_text.keySet(), sample_size);
            performSampleLSH(lsh, entity_indices, entity_text, text_terms, type);
        } else {
            performSampleLSH(lsh, entity_text.keySet(), entity_text, text_terms, type);
        }
    }

    private void performSampleLSH(LocalitySentitiveHashing lsh,
                                  Set<Integer> entity_indices,
                                  THashMap<Integer, THashMap<Integer, TIntIntHashMap>> entity_text,
                                  THashMap<Integer, String> text_terms,
                                  String type) {
        AtomicInteger atm = new AtomicInteger();
        entity_indices.forEach(entity_id -> {
            THashMap<Integer, TIntIntHashMap> sub_entity_text = entity_text.get(entity_id);
            THashSet<String> entity_vector = new THashSet<>();
            for (int ngram : sub_entity_text.keySet()) {
                for (int term_id : sub_entity_text.get(ngram).keys()) {
                    String term = text_terms.get(term_id);
                    entity_vector.add(term);
                }
            }
            String[] terms = new String[entity_vector.size()];
            entity_vector.toArray(terms);
            lsh.index(entity_id, terms);

            atm.incrementAndGet();
        });
        lsh.writeLSHEntities();
        System.out.printf("Finished computing LSH clusters for type %s.\n", type);
    }
}
