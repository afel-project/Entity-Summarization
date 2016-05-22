import edu.l3s.algorithm.EntitySummarization;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;

/**
 * Created by ranyu on 5/20/16.
 */
public class Main {
    public static void main(String[] args) throws ParseException, InterruptedException, IOException {
        EntitySummarization es = new EntitySummarization();
        es.load_index("Movie");
        String res = es.summarize("Forrest Gump", "Movie", "http://dbpedia.org/resource/Forrest_Gump");
        System.out.println(res);
    }
}
