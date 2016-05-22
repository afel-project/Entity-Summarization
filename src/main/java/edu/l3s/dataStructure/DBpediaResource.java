package edu.l3s.dataStructure;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import com.sun.org.apache.xpath.internal.operations.Bool;
import edu.l3s.utils.ObjectFormatting;
import edu.l3s.utils.PredicateFormatting;
import edu.l3s.utils.StringSimilarity;

import javax.xml.ws.EndpointReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ranyu on 3/17/16.
 */
public class DBpediaResource {
    private static String _page;
    private static Map<String, List<String>> _predicates;
    private static Map<String, String> _pred_map;
    private static Map<String, Double> _pred_weight;

    public DBpediaResource(String dis_page) throws IOException {
        _page = dis_page;
        _pred_map = new HashMap<String, String>();
        _pred_weight = new HashMap<>();
        _predicates = new HashMap<String, List<String>>();
        load_predMap();
        construct_resource();
    }

    public static void printDBR(){
        for(Map.Entry entry: _predicates.entrySet()){
            System.out.println(entry.getKey());
        }
    }
    private void load_predMap() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("predMap"));
        String line;
        while((line=br.readLine())!=null){
            String [] line_seg = line.split("\t");
            if(line_seg.length != 3){
                continue;
            }
            else{
                _pred_map.put(line_seg[0], line_seg[1]);
                _pred_weight.put(line_seg[1], Double.valueOf(line_seg[2]));
            }
        }
        br.close();
    }
    public static Double objectSim(String tmp_pred, String obj){
        Double score = 0.0;
        String pred = PredicateFormatting.predicateTerm(tmp_pred);
        if(_predicates.containsKey(pred)){
            List<String> objs = _predicates.get(pred);
            for(int i = 0; i < objs.size(); i++){
                if(StringSimilarity.isSameMainStr(objs.get(i), obj))
                    score += 1.0*_pred_weight.get(pred);
//                score += StringSimilarity.letterSim(objs.get(i), obj);
            }
        }
        return score;
    }

    private static boolean isSameString(String strx, String stry){
        String x = strx;
        String y = stry;

        x.toLowerCase();
        y.toLowerCase();

        x.replaceAll("\\s+", "");
        y.replaceAll("\\s+", "");

        x.replaceAll("_", "");
        y.replaceAll("_","");

        String x2 = ObjectFormatting.formatObj(x);
        String y2 = ObjectFormatting.formatObj(y);

//        System.out.println(x+"\tx<-\t->y\t"+y+"\n"+x2+"\tx2<-\t->y2\t"+y2);

        return (x.equals(y))||(x2.equals(y2));
    }

    public Boolean factExist(String pred, String obj){
        if(_predicates.containsKey(pred)){
            List<String> objs = _predicates.get(pred);
            for(int i = 0; i < objs.size(); i++){
                if(isSameString(objs.get(i), obj)){
                    return true;
                }
            }
        }
        return false;
    }

    private static void construct_resource(){
//        String resource_url = _page.replace(" ", "_");
        extractResource(_page);
    }
    public static void extractResource(String seedUri) {

        //load predicate map

        String sparqlEndpoint = "http://dbpedia.org/sparql";
//        String sparqlEndpoint = "http://dbpedia-live.openlinksw.com/sparql";

            String sparqlQuery = "" +
                    "SELECT  distinct ?p ?o WHERE { <" + seedUri + "> ?p ?o . } ";
            Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ) ;
            QuerySolutionMap querySolutionMap = new QuerySolutionMap();

            QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint,query);
            // execute a Select query
            ResultSet results = httpQuery.execSelect();

//            String katz_str = "";
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String pred = solution.get("p").toString();

                String pre_last = PredicateFormatting.predicateTerm(pred);

                String obj = solution.get("o").toString();
                String [] obj_seg = obj.split("/");
                obj = obj_seg[obj_seg.length-1];

//                System.out.println(pre_last + "\t" +obj);

                if(_pred_map.containsKey(pre_last)){
                    pred = _pred_map.get(pre_last);
                }
                else{ pred = pre_last;
                    _pred_weight.put(pred, 1.0);
                }

//                System.out.println(pred + "\t" +obj);

                if(_predicates.containsKey(pred)){
                    List<String> objs = _predicates.get(pred);
                    objs.add(obj);
//                    System.out.println(obj + "\t" + second_obj);
                    _predicates.put(pred, objs);
                }
                else{
                    List<String> objs = new ArrayList<String>();
                    objs.add(obj);
//                    System.out.println(obj + "\t" + second_obj);
                    _predicates.put(pred, objs);
                }
//                String obj = solution.get("o").asLiteral().getLexicalForm();

//                System.out.println(pred + "\t" + obj);
            }
    }

    //Document doc
}
