package edu.l3s.dataStructure;

//import org.apache.lucene.search.TopDocs;

import java.io.*;
import java.util.*;

//import org.apache.commons.lang.StringEscapeUtils;
//import org.apache.commons.validator.routines.UrlValidator;

/**
 * Created by ranyu on 9/1/15.
 */
public class peAbstractor {


    private static Map<String, pePredicate> _pePredicates;
    private static String _query;

    public peAbstractor(String query_id, String pred, String obj, Double score) throws IOException {
        _query = query_id;
        _pePredicates = new HashMap<String, pePredicate>();
        _pePredicates.put(pred, new pePredicate(pred, obj, score));
        //build_abstract(result_file, type);
    }

    public peAbstractor(String query_id) throws IOException {
        _query = query_id;
        _pePredicates = new HashMap<String, pePredicate>();
    }

    public int judgeFact(String pred, String obj) {
        if (_pePredicates.containsKey(pred)) return _pePredicates.get(pred).judgeFact(obj);
        else return -1;
    }

    public boolean haspePredicate(String pred) {
        return _pePredicates.containsKey(pred);
    }

    public void addObjGT(String pred, String obj, Double score) {
        if (_pePredicates.containsKey(pred)) {
            pePredicate tmp_pred = _pePredicates.get(pred);
            tmp_pred.put_object(obj, score);
            _pePredicates.put(pred, tmp_pred);
        } else {
            pePredicate new_pred = new pePredicate(pred, obj, score);
            //new_pred.put_object(obj,score);
            _pePredicates.put(pred, new_pred);
        }
    }
    public Map<String,pePredicate> getPredicates(){
        return _pePredicates;
    }

}
