package edu.l3s.dataStructure;

import javafx.util.Pair;
import net.sf.javaml.clustering.*;
import net.sf.javaml.core.*;
import net.sf.javaml.clustering.evaluation.HybridPairwiseSimilarities;
import net.sf.javaml.distance.CosineDistance;
import net.sf.javaml.distance.DistanceMeasure;
import net.sf.javaml.tools.DatasetTools;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Created by ranyu on 11/23/15.
 */
public class Predicate{
    //        private class Object{
//            private String _content;
//            private double _score;
//
//            public void Object(String content, double score){
//                content = _content;
//                score = _score;
//            }
//
//            public boolean
//        }
    private Double _score;
    private String _content;
    public Map<String, Double> _objects;
    public List<String> _objectList;
    private Map<Integer, String> _obj_map;
    private double _sum_len;

    private final Double _alpha = 0.9;

    public Predicate(){
    }
    public Predicate(String pre, String obj, int rank){
        _score = 1.0;
        _content = pre;
        _objects = new HashMap<String, Double>();
        _objectList = new ArrayList<String>();
        _obj_map= new HashMap<Integer, String>();
        _sum_len = 0;
        //_objects.put(obj, 1);
        if(!(obj.trim().equals("null")||obj.trim().equals("Null"))) {
            _objects.put(obj, 1.0);
            _objectList.add(obj);
        }
        //_objects.put(obj, Math.pow(_alpha, rank));

    }
    private static String dateTimeFormatter(String in) {
        //formatting date and time
        //String in = "Tue, 3 Jun 2008 11:05:30 GMT   ";
        LocalDate date;
        try {
            date = LocalDate.parse(in, DateTimeFormatter.BASIC_ISO_DATE);
            //System.out.printf(date.toString());
            return date.toString();
        } catch (DateTimeParseException exc) {
            // System.out.printf("%s is not parsable!%n", in);
            //throw exc;      // Rethrow the exception.
        }
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_ZONED_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_ORDINAL_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_WEEK_DATE);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.ISO_INSTANT);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        try {
            date = LocalDate.parse(in, DateTimeFormatter.RFC_1123_DATE_TIME);
            return date.toString();
        } catch (DateTimeParseException exc) {}
        return in;
    }
    public int judgeFact(String obj){
        if(_objects.containsKey(obj)) return (_objects.get(obj)>0.5)?1:0;
        else return -1;
    }
    public void add_object(String obj, Integer rank){
        if(obj.trim().equals("null")||obj.trim().equals("Null")) return;
        _score+=1.0;

        if(_objects.containsKey(obj)){
            _objects.put(obj, _objects.get(obj)+1);
            //_objects.put(obj, _objects.get(obj)+Math.pow(_alpha, rank));
        }
        else{
            _objects.put(obj, 1.0);
            //_objects.put(obj, Math.pow(_alpha, rank));
        }
        _objectList.add(obj);
        _sum_len+=obj.length();
    }
    public void put_object(String obj, Double score){
        _objects.put(obj, score);
    }
    //build dataset for string editing distance
    private List<Instance> build_dataset_SED(){
        List<Instance> ins = new ArrayList<Instance>();

        Map<Character, Integer> dict = new HashMap<Character, Integer>();
        Integer cnt = 0;
        for(Integer i = 0; i < _objectList.size(); i++) {

            String tmp_obj = _objectList.get(i);
            if (tmp_obj.startsWith("node")) continue;
            tmp_obj = dateTimeFormatter(tmp_obj);

            char [] charlist = tmp_obj.toCharArray();
            for(Integer j = 0; j < charlist.length; j++) {
                char letter = charlist[j];

                if (dict.containsKey(letter) == false) {
                    //   System.out.println(word+"\t"+cnt);
                    dict.put(letter, cnt);
                    cnt++;
                }
            }
        }
        System.out.println("Size of dict: "+dict.size());
        for(Integer i = 0; i < _objectList.size(); i++) {

            String tmp_obj = _objectList.get(i);
            if (tmp_obj.startsWith("node")) continue;
            tmp_obj = dateTimeFormatter(tmp_obj);

            char [] charlist = tmp_obj.toCharArray();

            double[] vec = new double[dict.size()];
            for(Integer j = 0; j < charlist.length; j++) {
                char letter = charlist[j];

                if (dict.containsKey(letter)) {
                    int word_no = dict.get(letter);
                    vec[word_no] = vec[word_no] +1.0;
                }
            }

            Instance tmp_ins = new DenseInstance(vec);
            //System.out.println("Instance length = "+ tmp_ins.size());
            //Instance tmp_ins = new SparseInstance(vec);

            _obj_map.put(tmp_ins.getID(), _objectList.get(i));
            //System.out.println("adding: " + tmp_ins.getID() + "\t" + _objectList.get(j));
            ins.add(tmp_ins);

            //TODO: stemming
        }
//        System.out.println(ins.size() + " instances added for predicate: " + _content);
        return ins;
    }

    public boolean contains_object(String str){
        return _objects.containsKey(str);
    }

    private List<Instance> build_dataset(){
        //build instance for each object, format Double values[]={1,2,1,1,0,1,0}
        //need a dictionary to support
        //stops words, stemming, node**, url, date
        List<Instance> ins = new ArrayList<Instance>();

        Map<String, Integer> dict = new HashMap<String, Integer>();
        Integer cnt = 0;
        String delims = ":; \"\', |/.[]{}()!?>-";
        for(Integer i = 0; i < _objectList.size(); i++) {
            //TODO: tokenize, remove stop words
            String tmp_obj = _objectList.get(i);
            tmp_obj = dateTimeFormatter(tmp_obj);
            StringTokenizer tokens = new StringTokenizer(tmp_obj, delims);

            Instance tmp_ins = new SparseInstance();
            while (tokens.hasMoreTokens()) {
                String word = tokens.nextToken().toLowerCase();
                word.trim();
                if (word.length() < 1) continue;
               // if(word.startsWith("node")) continue;
                if (dict.containsKey(word) == false) {
                    //   System.out.println(word+"\t"+cnt);
                    dict.put(word, cnt);
                    cnt++;
                }
            }
        }
//        System.out.println("Size of dict: "+dict.size());
        for(Integer j = 0; j < _objectList.size(); j++) {
            //TODO: tokenize, remove stop words
            StringTokenizer tokens = new StringTokenizer(_objectList.get(j),delims);

            double[] vec = new double[dict.size()];
            while (tokens.hasMoreTokens()) {
                String word = tokens.nextToken().toLowerCase();
                word.trim();
                if (word.length() < 1 || word.startsWith("node")) continue;

                //Integer word_no = dict.get(word);

                if (dict.containsKey(word)) {
                    int word_no = dict.get(word);
                    vec[word_no] = vec[word_no] +1.0;
                }
            }

            Instance tmp_ins = new DenseInstance(vec);
            //System.out.println("Instance length = "+ tmp_ins.size());
            //Instance tmp_ins = new SparseInstance(vec);

            _obj_map.put(tmp_ins.getID(), _objectList.get(j));
            //System.out.println("adding: " + tmp_ins.getID() + "\t" + _objectList.get(j));
            ins.add(tmp_ins);

            //TODO: stemming
        }
//        System.out.println(ins.size() + " instances added for predicate: " + _content);
        return ins;
    }
    //TODO: how many objects to print?
    public Pair<List<String>, List<Double> > get_objects_from_cluster(double thresh) throws IOException {
        List<String> res=new ArrayList<String>();
        List<Double> cls_sizes = new ArrayList<Double>();

        //choose distance measure based on the average length of objects
        DistanceMeasure dm;
        List<Instance> data_list;
        if(_objectList.size() == 0) return new Pair<List<String>, List<Double> >(res, cls_sizes);
//        if(_sum_len/_objectList.size() <= 20 ){
//            //dm = new StringEditingDistance();
//            data_list = build_dataset_SED();
//        }
//        else {
//           // dm = new CosineDistance();
//            data_list = build_dataset();
//        }
        dm = new CosineDistance();

        Dataset data = new DefaultDataset(build_dataset());

        //clusterer
        int k = data.size();
        if(data.size()>30)k=data.size()/5;
        if(k>30) k = 30;
        KMeans kmeans = new KMeans(5,5);
        HybridPairwiseSimilarities ce = new HybridPairwiseSimilarities();
        IterativeKMeans ik = new IterativeKMeans(1,data.size()>50?50:data.size(), 20, dm, ce);
        // IterativeKMeans ik = new IterativeKMeans(1,10, ce);
        IterativeMultiKMeans imk = new IterativeMultiKMeans(1,10, ce);
        IterativeFarthestFirst iff = new IterativeFarthestFirst(ce);
        FarthestFirst ff = new FarthestFirst();

            /* Perform clustering */
        if(0 == data.size()){
            System.out.println("Empty object set.");
            return new Pair<List<String>, List<Double> >(res, cls_sizes);
        }
//        System.out.println("Start clustering.");
        Dataset[] clusters = ik.cluster(data);
            /* Output results */
//        System.out.println("number of clusters for predicate:" + _content + " is:" + clusters.length);

        //Class classes = clusters.getClass();


        double clusterNum = 0;
        //can be removed, use clusters[0].size() as max_size
        Integer max_size = 0;
        for(Dataset clst : clusters){
            max_size = max_size >= clst.size()?max_size:clst.size();
        }
        Arrays.sort(clusters, new Comparator<Dataset>() {
            public int compare(Dataset o1, Dataset o2) {
                return Integer.valueOf(o2.size()).compareTo(o1.size());
            }
        });

        for(Dataset clst : clusters){
            if(res.isEmpty()==false && clst.size()<=thresh*max_size) continue;

            Instance clusterCentroid = DatasetTools.average(clst);

            double minDistanst=1.1;
            int bestInstanceLabel=-1;

            for (Instance inst: clst){
//                System.out.println("id = " + inst.getID());
                double tmpDistance = dm.measure(clusterCentroid, inst);
                if(tmpDistance < minDistanst){
                    bestInstanceLabel = inst.getID();
                }
            }

//            for(int i = 0; i< clst.noAttributes();i++){
//                System.out.println(i);
//
//                double tmpDistance = dm.measure(clusterCentroid, clst.instance(i));
//                if(tmpDistance < minDistanst){
//                    bestInstanceLabel = i;
//                }
//            }
            String bestObj = _obj_map.get(bestInstanceLabel);
           // System.out.println(bestObj);
            if(bestObj != null){
                res.add(bestObj);
//                cls_sizes.add(Double.valueOf(clst.size())/data.size());
                cls_sizes.add(Double.valueOf(clst.size()));
            }
//            System.out.println("cluster size: " + clst.size() + "\tBest object: "+bestObj);
        }
        return new Pair<List<String>, List<Double> >(res, cls_sizes);
    }
    public Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > > get_all_objects_from_cluster(double thresh) throws IOException {
        List<String> res_obj = new ArrayList<String>();
        List<Integer> res_flag = new ArrayList<Integer>();
        List<Double> cls_sizes = new ArrayList<Double>();
        List<Double> obj_cls_size = new ArrayList<Double>();

        //choose distance measure based on the average length of objects
        DistanceMeasure dm;
        List<Instance> data_list;
        if(_objectList.size() == 0) {
            return new Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > >(new Pair< List<String>,List<Integer> >(res_obj, res_flag),
                    new Pair< List<Double>,List<Double> >(obj_cls_size,cls_sizes));
        }

        dm = new CosineDistance();

        Dataset data = new DefaultDataset(build_dataset());

        //clusterer
        int k = data.size();
        if(data.size()>30)k=data.size()/5;
        if(k>30) k = 30;
        KMeans kmeans = new KMeans(5,5);
        HybridPairwiseSimilarities ce = new HybridPairwiseSimilarities();
        IterativeKMeans ik = new IterativeKMeans(1,data.size()>50?50:data.size(), 20, dm, ce);
        // IterativeKMeans ik = new IterativeKMeans(1,10, ce);
        IterativeMultiKMeans imk = new IterativeMultiKMeans(1,10, ce);
        IterativeFarthestFirst iff = new IterativeFarthestFirst(ce);
        FarthestFirst ff = new FarthestFirst();

            /* Perform clustering */
        if(0 == data.size()){
            System.out.println("Empty object set.");
            return new Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > >(new Pair< List<String>,List<Integer> >(res_obj, res_flag),
                    new Pair< List<Double>,List<Double> >(obj_cls_size,cls_sizes));
        }
//        System.out.println("Start clustering.");
        Dataset[] clusters = ik.cluster(data);
            /* Output results */
//        System.out.println("number of clusters for predicate:" + _content + " is:" + clusters.length);

        //Class classes = clusters.getClass();


        double clusterNum = 0;
        //can be removed, use clusters[0].size() as max_size
        Integer max_size = 0;
        for(Dataset clst : clusters){
            max_size = max_size >= clst.size()?max_size:clst.size();
        }
        Arrays.sort(clusters, new Comparator<Dataset>() {
            public int compare(Dataset o1, Dataset o2) {
                return Integer.valueOf(o2.size()).compareTo(o1.size());
            }
        });

        for(Dataset clst : clusters){
            if(res_obj.isEmpty()==false && clst.size()<=thresh*max_size) continue;

            Instance clusterCentroid = DatasetTools.average(clst);

            double minDistanst=1.1;
            int bestInstanceLabel=-1;
            int bestInstanceIndex=-1;

            for (Instance inst: clst){
//                System.out.println("id = " + inst.getID());
                res_obj.add(_obj_map.get(inst.getID() ) );
                res_flag.add(0);
                obj_cls_size.add(Double.valueOf(clst.size()));

                double tmpDistance = dm.measure(clusterCentroid, inst);
                if(tmpDistance < minDistanst){
                    bestInstanceLabel = inst.getID();
                    bestInstanceIndex= res_obj.size()-1;
                }
            }

            String bestObj = _obj_map.get(bestInstanceLabel);
           // System.out.println(bestObj);
            if(res_flag.size() != res_obj.size()){
                System.out.println("ERROR:\tthe object list size and flag array size do not match!");
                System.exit(5);
            }
            if(bestInstanceIndex >=0 && bestInstanceIndex < res_flag.size()){
                res_flag.set(bestInstanceIndex, 1);
                cls_sizes.add(Double.valueOf(clst.size()));
            }
//            System.out.println("cluster size: " + clst.size() + "\tBest object: "+bestObj);
        }
        return new Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > >(new Pair< List<String>,List<Integer> >(res_obj, res_flag),
                new Pair< List<Double>,List<Double> >(obj_cls_size,cls_sizes));
    }
    public String best_object(){
        Map.Entry<String, Double> maxEntry = null;

        for (Map.Entry<String, Double> entry : _objects.entrySet())
        {
            if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
            {
                maxEntry = entry;
            }
        }

        return maxEntry.getKey();
    }
    public List<String> best_objects(){

        List<String> res = new ArrayList<String>();

        List<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(_objects.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });


        for(Integer i = 0; i < list.size(); i++){
            if(list.get(i).getValue() >= 0.5 * list.get(0).getValue()) res.add(list.get(i).getKey());
        }

        return res;
    }

    public String get_predicate(){
        return _content;
    }

    public Double get_score(){
        return _score;
    }
    public boolean getObjectJudgement(String obj){
        return _objects.get(obj) > 0.5;
    }
//        public int compareTo(Predicate obj) {
//            int deptComp = _score.compareTo(obj.get_score());
//
//            return deptComp;
//        }
    static class PredicateComparator implements Comparator<Predicate> {

    //@Override
    public int compare(Predicate item1, Predicate item2) {
        return item2.get_score().compareTo(item1.get_score());
    }

}
}