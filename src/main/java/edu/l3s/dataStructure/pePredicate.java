package edu.l3s.dataStructure;

import net.sf.javaml.clustering.*;
import net.sf.javaml.clustering.evaluation.HybridPairwiseSimilarities;
import net.sf.javaml.core.*;
import net.sf.javaml.distance.CosineDistance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.tools.DatasetTools;

import java.io.IOException;
import java.util.*;

/**
 * Created by ranyu on 11/23/15.
 */
public class pePredicate {

    private Double _score;
    private String _content;
    public Map<String, Double> _objects;

    public pePredicate() {
    }

    public pePredicate(String pre, String obj, Double score) {
        _score = 1.0;
        _content = pre;
        _objects = new HashMap<String, Double>();

        //_objects.put(obj, 1);
        if (!(obj.trim().equals("null") || obj.trim().equals("Null"))) {
            _objects.put(obj, score);
        }
        //_objects.put(obj, Math.pow(_alpha, rank));

    }

    public int judgeFact(String obj) {
        if (_objects.containsKey(obj)) return (_objects.get(obj) > 0.5) ? 1 : 0;
        else return -1;
    }

    public void put_object(String obj, Double score) {
        _objects.put(obj, score);
    }

    public String get_predicate() {
        return _content;
    }
}