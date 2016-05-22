package externalFeature;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by ranyu on 3/17/16.
 */
public class DBpediaResource {
    private static String _query;
    private static Map<String, List<String>> _predicates;

    public DBpediaResource(String query){
        _query = query;
        construct_resource();
    }

    private static void construct_resource(){
        String resource_url = _query.replace(" ", "_");
        resource_url = "http://dbpedia.org/resource/" + resource_url;
//        System.out.println(page_url);
        extractResource(resource_url);
    }
    public static void extractResource(String seedUri) {

        //load predicate map

        String sparqlEndpoint = "http://dbpedia-live.openlinksw.com/sparql";

            String sparqlQuery = "" +
                    "SELECT  distinct ?p ?o WHERE { <" + seedUri + "> ?p ?o . } ";
            Query query = QueryFactory.create(sparqlQuery, Syntax.syntaxARQ) ;
            QuerySolutionMap querySolutionMap = new QuerySolutionMap();

            QueryEngineHTTP httpQuery = new QueryEngineHTTP(sparqlEndpoint,query);
            // execute a Select query
            ResultSet results = httpQuery.execSelect();

            String katz_str = "";
            while (results.hasNext()) {
                QuerySolution solution = results.next();
                String pred = solution.get("p").toString();
                String obj = solution.get("o").toString();
//                String obj = solution.get("o").asLiteral().getLexicalForm();

                System.out.println(pred+"\t" + obj);
            }
    }

    //Document doc
}
