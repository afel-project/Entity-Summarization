package ranking;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.PorterStemmer;
import utils_package.FileUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by besnik on 10/09/2014.
 */
public class CosineSimilarity {
    public Map<String, String> documents;
    private Set<String> stop_words;

    public CosineSimilarity(Map<String, String> documents, String stop_words_path) {
        stop_words = FileUtils.readIntoSet(stop_words_path, "\n", false);
        this.documents = documents;
    }

    /**
     * Computes the cosine similarity of the candidate paragraphs for a section and a given entity.
     */
    public double[][][] computeDocumentCosineSimilarity() {
        double[][][] similarity_scores = new double[1][documents.size()][documents.size()];

        //compute the tf-idf scores of the documents
        DoubleMatrix2D tfidf_scores = computeTFIDFDcoumentScores(documents.size());
        //this is computed per entity
        for (int doc_index_a = 0; doc_index_a < documents.size(); doc_index_a++) {
            DoubleMatrix1D vector_a = tfidf_scores.viewRow(doc_index_a);
            for (int doc_index_b = doc_index_a + 1; doc_index_b < documents.size(); doc_index_b++) {
                DoubleMatrix1D vector_b = tfidf_scores.viewRow(doc_index_b);

                double sim_score = vector_a.zDotProduct(vector_b) / Math.sqrt(vector_a.zDotProduct(vector_a) * vector_b.zDotProduct(vector_b));

                similarity_scores[0][doc_index_a][doc_index_b] = sim_score;
                similarity_scores[0][doc_index_b][doc_index_a] = sim_score;
            }
        }
        return similarity_scores;
    }

    /**
     * Compute the similarity between a given pair of documents.
     *
     * @param doc_a
     * @param doc_b
     * @param tfidf_scores
     * @return
     */
    public double getDocumentSimilarity(String doc_a, String doc_b, DoubleMatrix2D tfidf_scores) {
        int index_a = getDocumentIndex(doc_a);
        int index_b = getDocumentIndex(doc_b);

        System.out.printf("Document similarity pairwise: <%s,%s> = <%d,%d>.\n", doc_a, doc_b, index_a, index_b);

        if (index_a == -1 || index_b == -1) {
            System.out.printf("Non-existing similarity pair: <%s, %s> = <%d,%d>.\n", doc_a, doc_b, index_a, index_b);
            return 0;
        }

        DoubleMatrix1D vector_a = tfidf_scores.viewRow(index_a);
        DoubleMatrix1D vector_b = tfidf_scores.viewRow(index_b);

        double sim_score = vector_a.zDotProduct(vector_b) / Math.sqrt(vector_a.zDotProduct(vector_a) * vector_b.zDotProduct(vector_b));

        return sim_score;
    }

    /**
     * Get the document index from the collection.
     *
     * @param doc_id
     * @return
     */
    private int getDocumentIndex(String doc_id) {
        int index = 0;

        for (String doc_id_tmp : documents.keySet()) {
            if (doc_id_tmp.equals(doc_id)) {
                return index;
            }
            index++;
        }
        return -1;
    }


    /**
     * Computes for each document and its corresponding candidate paragraph sections the tfxidf scores.
     */
    public DoubleMatrix2D computeTFIDFDcoumentScores(int doc_size) {
        //compute the idf
        Map<String, Set<String>> idf = new HashMap<String, Set<String>>();
        Map<Integer, Map<String, Integer>> tfall = new HashMap<Integer, Map<String, Integer>>();

        //this is computed per entity
        int doc_id_counter = 0;
        for (String doc_id : documents.keySet()) {
            String doc_text = documents.get(doc_id);

            Map<String, Integer> tf = new HashMap<String, Integer>();
            addTermsToDictionary(doc_id, doc_text.toLowerCase(), tf, idf);

            tfall.put(doc_id_counter, tf);
            doc_id_counter++;
        }

        double[][] tf_idf_scores = new double[documents.size()][idf.size()];
        //compute the tfxidf scores
        for (int doc_id : tfall.keySet()) {
            Map<String, Integer> tf = tfall.get(doc_id);
            tf_idf_scores[doc_id] = computeTfIdfScores(tf, idf, doc_size);
        }

        DoubleMatrix2D tfidf_doc_scores = new DenseDoubleMatrix2D(tf_idf_scores);
        return tfidf_doc_scores;
    }

    public double[] computeTfIdfScores(Map<String, Integer> tf, Map<String, Set<String>> idf, int doc_size) {
        double[] tfidf = new double[idf.size()];

        int counter = 0;
        for (String term : idf.keySet()) {
            double idf_score = Math.log(doc_size / (double) idf.get(term).size());
            int tf_score = !tf.containsKey(term) ? 0 : tf.get(term);

            tfidf[counter] = tf_score * idf_score;
            counter++;
        }

        return tfidf;
    }

    private void addTermsToDictionary(String doc_id, String text, Map<String, Integer> tf, Map<String, Set<String>> idf) {
        String[] tokens = StringUtils.split(text);
        PorterStemmer stem = new PorterStemmer();
        for (String token : tokens) {
            stem.setCurrent(token);
            stem.stem();

            String stemmed_token = stem.getCurrent();
            if (stop_words.contains(stemmed_token) || stop_words.contains(token))
                continue;

            Integer tf_val = tf.get(stemmed_token);
            tf_val = tf_val == null ? 1 : tf_val + 1;
            tf.put(stemmed_token, tf_val);

            Set<String> sub_idf = idf.get(stemmed_token);
            sub_idf = sub_idf == null ? new HashSet<String>() : sub_idf;
            idf.put(stemmed_token, sub_idf);

            sub_idf.add(doc_id);
        }
    }
}
