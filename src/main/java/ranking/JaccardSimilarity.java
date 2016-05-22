package ranking;

import org.apache.commons.lang3.StringUtils;
import org.tartarus.snowball.ext.PorterStemmer;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by besnik on 23/09/2014.
 */
public class JaccardSimilarity {
    private String doc_a;
    private String doc_b;

    public JaccardSimilarity(String doc_a, String doc_b) {
        this.doc_a = doc_a;
        this.doc_b = doc_b;
    }

    /**
     * Compute the jaccard similarity.
     *
     * @return
     */
    public double computeJaccardSimilarity() {
        String[] tokens_a = StringUtils.split(doc_a);
        String[] tokens_b = StringUtils.split(doc_b);

        Set<String> token_set_a = new HashSet<String>();
        Set<String> token_set_b = new HashSet<String>();

        PorterStemmer stem = new PorterStemmer();
        for (String token_a : tokens_a) {
            stem.setCurrent(token_a);
            stem.stem();
            token_set_a.add(stem.getCurrent());
        }

        for (String token_b : tokens_b) {
            stem.setCurrent(token_b);
            stem.stem();
            token_set_b.add(stem.getCurrent());
        }

        //compute the set intersection
        Set<String> common_terms = new HashSet<String>(token_set_a);
        common_terms.retainAll(token_set_b);

        double jaccard = common_terms.size() / (double) (token_set_a.size() + token_set_b.size() - common_terms.size());
        jaccard = Double.isNaN(jaccard) ? 0 : jaccard;
        return jaccard;
    }
}
