package edu.l3s.algorithm;

import edu.l3s.algorithm.Abstractor;
import libsvm.LibSVM;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.FactDenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import query.RetrievalModel;
import utils_package.FileUtils;


import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.*;

import edu.l3s.dataStructure.Summary;

/**
 * Created by ranyu on 5/18/16.
 */
public class EntitySummarization {
    private static Map<String, IndexSearcher> _indexes;
    private static RetrievalModel _rm;
    static Classifier _cf;

    public EntitySummarization() throws IOException, ParseException {
        _rm = new RetrievalModel();
        _indexes = new HashMap<String, IndexSearcher>();

        _cf = new LibSVM();
//        _cf = new NaiveBayesClassifier(true,true,false);
        Dataset data = FileHandler.loadDataset(new File("training.data"), 5, "\t");
        _cf.buildClassifier(data);
    }
    public static void load_index(String type) throws IOException, ParseException {
        IndexSearcher is =_rm.getLuceneIndex("./index/"+type);
        _indexes.put(type, is);
    }
    public static String summarize(String query_t, String type_t,  String dis) throws ParseException, IOException, InterruptedException {

        String query_str = query_t;

        String type = type_t;
        String disamb_page = dis;
        Integer topk = 50;

        ScoreDoc[] hits = entity_retrieval(query_str, type, topk);
        System.out.printf("Finished querying for query \"%s\".\n", query_str);

        return sum_div(hits, query_str, type, disamb_page);
    }

    private static String sum_div(ScoreDoc[] hits, String query, String type, String dis_page) throws IOException, InterruptedException {
        String summary="{\n" +
                "\t\"facts\": [\n";

        Abstractor abs = new Abstractor(_indexes.get(type), hits, query, type, dis_page);
        boolean [] feature_index= new boolean[3];
        feature_index[0]=feature_index[1]=feature_index[2]=true;

        List<FactDenseInstance> instances = abs.getTestData(feature_index);

        int cnt = 0;
        for (FactDenseInstance instance : instances) {
            Object prediction = _cf.classify(instance);
            if (prediction.equals("1")){
                if(cnt !=0 ) summary += ",\n";
                else summary+="\n";
                cnt+=1;

                summary += instance.compose_json_str();
            }

            //bw.write(out_text + instance.classValue() + "\t" + prediction.toString() + "\n");
        }
        summary += "\t]\n" +
                "}\n";
        return summary;
    }

    private static ScoreDoc[] entity_retrieval(String query_str, String type, Integer topk)throws ParseException, IOException, InterruptedException {
//        String[] typeFields = _rm.loadFileds(type);

        String [] typeFields = new String[2];
        typeFields[0] = "http://schema.org/name";
        typeFields[1] = "http://schema.org/"+type+"/name";

        Analyzer analyzer = new StandardAnalyzer();
        QueryParser parser = new MultiFieldQueryParser(typeFields, analyzer);
        Query query = parser.parse(query_str);

        //search the Lucene index using the standard approach.

        ScoreDoc[] hits = new ScoreDoc[0];
        if (_indexes.containsKey(type)) {
            return _rm.bm25Search(_indexes.get(type), query, topk, type);

        } else {
            System.out.println("ERROR:\tcan not find index of type " + type);
            System.exit(1);
        }
        return hits;
    }

}
