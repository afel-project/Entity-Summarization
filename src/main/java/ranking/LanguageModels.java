package ranking;

import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.SnowballProgram;
import org.tartarus.snowball.ext.EnglishStemmer;

import java.util.*;
import java.util.logging.Logger;

/**
 * Created by besnik on 10/7/14.
 */
public class LanguageModels {
    private static final Logger log = Logger.getLogger(LanguageModels.class.getName());

    private Map<String, String> documents;
    private Map<String, Integer> document_total_terms;
    private Set<String> stop_words;
    private Map<String, Map<String, Integer>> lm;

    public LanguageModels(Map<String, String> documents, Set<String> stop_words) {
        this.documents = documents;
        document_total_terms = new HashMap<>();
        this.stop_words = stop_words;
        lm = new HashMap<>();
        this.stop_words = stop_words;
    }

    public LanguageModels(Set<String> stop_words) {
        document_total_terms = new HashMap<>();
        documents = new HashMap<>();
        lm = new HashMap<>();
        this.stop_words = stop_words;
    }

    public void addDocument(String doc_id, String doc_content) {
        log.info(String.format("Adding document with id: %s and content: %s", doc_id, doc_content));
        documents.put(doc_id, doc_content);
    }

    /**
     * Computes the language model of a document.
     *
     * @param doc_content
     * @return
     */
    public Map<String, Integer> getLanguageModel(String doc_content) {
        Map<String, Integer> doc_lm = new HashMap<>();
        //stem the words
        SnowballProgram stemmer = new EnglishStemmer();
        String[] terms = StringUtils.split(doc_content);

        for (int i = 0; i < terms.length; i++) {
            //apply stemming
            String term = terms[i];
            if (stop_words.contains(term)) {
                continue;
            }
            stemmer.setCurrent(term);
            term = stemmer.getCurrent();

            Integer count = doc_lm.get(term);
            count = count == null ? 0 : count;
            count++;

            doc_lm.put(term, count);
        }
        return doc_lm;
    }

    /**
     * Compute the language models of our corpus.
     *
     * @return
     */
    public void computeLanguageModels() {
        //stem the words
        SnowballProgram stemmer = new EnglishStemmer();

        for (String doc_id : documents.keySet()) {
            Map<String, Integer> doc_lm = new HashMap<>();
            lm.put(doc_id, doc_lm);

            String[] terms = StringUtils.split(documents.get(doc_id));
            document_total_terms.put(doc_id, terms.length);

            for (int i = 0; i < terms.length; i++) {
                //apply stemming
                String term = terms[i];
                if (stop_words.contains(term)) {
                    continue;
                }

                stemmer.setCurrent(term);
                term = stemmer.getCurrent();

                Integer count = doc_lm.get(term);
                count = count == null ? 0 : count;
                count++;

                doc_lm.put(term, count);
            }
        }
    }

    /**
     * Compute the KL-divergence or Cross-enthropy of two given documents based on their language model.
     *
     * @param doc_id_a
     * @param doc_id_b
     * @return
     */
    public double getCrossEnthropy(String doc_id_a, String doc_id_b) {
        if (document_total_terms.get(doc_id_a) == null || document_total_terms.get(doc_id_b) == null) {
            return 1.0;
        }
        int total_terms_a = document_total_terms.get(doc_id_a);
        int total_terms_b = document_total_terms.get(doc_id_b);

        Map<String, Integer> doc_a_lm = lm.get(doc_id_a);
        Map<String, Integer> doc_b_lm = lm.get(doc_id_b);

        //common terms from the language models
        Set<String> common_terms = new HashSet<>(doc_a_lm.keySet());
        common_terms.retainAll(doc_b_lm.keySet());

        return getKLDivergence(doc_a_lm, total_terms_a, doc_b_lm, total_terms_b, common_terms);
    }

    /**
     * Computes the KL Divergence between two topic distributions.
     *
     * @param doc_a_lm
     * @param doc_b_lm
     * @param term_intersection
     * @return
     */
    public static double getKLDivergence(Map<String, Integer> doc_a_lm, int total_a,
                                         Map<String, Integer> doc_b_lm, int total_b,
                                         Set<String> term_intersection) {
        double klDiv = 0.0;

        if (doc_a_lm.isEmpty() || doc_b_lm.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double epsilon_a = Collections.min(doc_a_lm.values());
        double epsilon_b = Collections.min(doc_b_lm.values());

        epsilon_a = epsilon_a != 0 ? epsilon_a - epsilon_a / 10 : epsilon_a + 0.001;
        epsilon_b = epsilon_b != 0 ? epsilon_b - epsilon_b / 10 : epsilon_b + 0.001;

        for (String term : term_intersection) {
            Double prob_a = doc_a_lm.get(term) / (double) total_a;
            Double prob_b = doc_b_lm.get(term) / (double) total_b;

            prob_a = prob_a == null ? epsilon_a : prob_a;
            prob_b = prob_b == null ? epsilon_b : prob_b;

            klDiv += (prob_a - prob_b) * Math.log(prob_a / prob_b);
        }

        double kl_val = (klDiv);
        kl_val = 1 - Math.exp(-kl_val);
        return kl_val;
    }
}
