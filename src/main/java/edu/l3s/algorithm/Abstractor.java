package edu.l3s.algorithm;

//import org.apache.lucene.search.TopDocs;

import edu.l3s.dataStructure.DBpediaResource;
import edu.l3s.dataStructure.Predicate;
import edu.l3s.dataStructure.FactFeature;
import edu.l3s.utils.GooglePR;
import edu.l3s.utils.ObjectFormatting;
import edu.l3s.utils.PredicateFormatting;
import edu.l3s.utils.skipURL;
import javafx.util.Pair;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;
import net.sf.javaml.core.FactDenseInstance;
import net.sf.javaml.core.Instance;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

//import org.apache.commons.lang.StringEscapeUtils;
//import org.apache.commons.validator.routines.UrlValidator;

import java.io.*;
import java.util.*;

import static edu.l3s.utils.GooglePR.getUrlPR;

/**
 * Created by ranyu on 9/1/15.
 */
public class Abstractor {

    static class PredicateComparator implements Comparator<Predicate> {

        //@Override
        public int compare(Predicate item1, Predicate item2) {
            return item2.get_score().compareTo(item1.get_score());
        }

    }


    private static Map<String, Predicate> _predicates;
    private static String _query;
    private static String _type;
    private static String _query_term;
    private static Map<String, Double> _subject_freq;
    private static Map<String, Double> _subject_dbsim;
    private static Map<String, Integer> _subject_nameExist;
    private static Map<String, Double> _pred_freq;
    private static Map<String, Double> _pred_length;
    private static DBpediaResource _dbr;

    private static Map<Pair<String, String>, FactFeature> _factFeatureSet;


    public Abstractor(IndexSearcher searcher, ScoreDoc[] hits,String query_term,  String type, String dis_page) throws IOException, InterruptedException {
        _predicates = new HashMap<String, Predicate>();
        _type = type;
        _query_term = query_term;
        _factFeatureSet = new HashMap<Pair<String, String>, FactFeature>();
        _subject_freq = new HashMap<String, Double>();
        _pred_freq = new HashMap<String, Double>();
        _pred_length = new HashMap<String, Double>();
        _subject_dbsim = new HashMap<String, Double>();
        _subject_nameExist = new HashMap<>();
        _dbr = new DBpediaResource(dis_page);
        cal_pred_feature(searcher, hits, query_term, type);
        build_abstract(searcher, hits, type);
    }
    public Abstractor(String query_id, String query_term, String result_file, String type, int topk, String out_path, String out_gt_path) throws IOException, InterruptedException {
        _query = query_id;
        _predicates = new HashMap<String, Predicate>();
        _type = type;
        _query_term = query_term;
//        _dbr = new DBpediaResource(query_term);
//        System.exit(4);

        print_dataset(result_file, type, out_path, out_gt_path, topk);
    }
    public Abstractor(String query_id) throws IOException {
        _query = query_id;
        _predicates = new HashMap<String, Predicate>();
    }
    public int judgeFact(String pred, String obj){
        if(_predicates.containsKey(pred)) return _predicates.get(pred).judgeFact(obj);
        else return -1;
    }

    public boolean hasPredicate(String pred){
        return _predicates.containsKey(pred);
    }
    public void addObjGT(String pred, String obj, Double score){
        if(_predicates.containsKey(pred)){
            Predicate tmp_pred = _predicates.get(pred);
            tmp_pred.put_object(obj, score);
            _predicates.put(pred, tmp_pred);
        }
        else{
            Predicate new_pred = new Predicate(pred,obj,1);
            new_pred.put_object(obj,score);
            _predicates.put(pred, new_pred);
        }
    }

    private void cal_pred_feature(IndexSearcher searcher, ScoreDoc[] hits, String query, String type) throws IOException {
        System.out.println("Start extracting predicate features");

        NameChecker nc = new NameChecker(query);
        Set<String> sub_names = new HashSet<>();
        for (int i = 0; i < hits.length; i++) {
            Document doc = searcher.doc(hits[i].doc);
            List<IndexableField> fields = doc.getFields();
            String resource_uri = doc.get("resource_uri");
            String page_url = doc.get("page_url");
            if(doc.getField("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") == null){
                continue;
            }
            if(!doc.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").contains(type)){
                continue;
            }
            _subject_freq.put(resource_uri, Double.valueOf(fields.size()));
            for(IndexableField field:fields){
                if(field.name()=="page_url")//||field.name()=="resource_uri")
                    continue;
                String field_content=field.stringValue().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\s+", " ").replaceAll("\t", " ").trim();
                if(field.name().endsWith("name")) sub_names.add(field_content);
                Double sim_sum = 0.0;
                if(_subject_dbsim.containsKey(resource_uri)){
                    _subject_dbsim.get(resource_uri);
                }

               // _dbr.printDBR();
                sim_sum += _dbr.objectSim(field.name(),field_content);
                _subject_dbsim.put(resource_uri, sim_sum);

                if(_pred_freq.containsKey(field.name())){
                    _pred_freq.put(field.name(), _pred_freq.get(field.name())+1.0);
                    _pred_length.put(field.name(), _pred_length.get(field.name())+field_content.length());
                }
                else{
                    _pred_freq.put(field.name(), 1.0);
                    _pred_length.put(field.name(), (double)field_content.length() );
                }
            }

            fields.clear();
            if(!sub_names.isEmpty()){
                _subject_nameExist.put(resource_uri, nc.nameExist(sub_names));
            }
            else{
                _subject_nameExist.put(resource_uri, 0);
            }
            sub_names.clear();
        }
    }

    public static void build_abstract(IndexSearcher searcher, ScoreDoc[] hits, String type) throws IOException, InterruptedException {

        GooglePR gp = new GooglePR();

        //feature extraction
        double bm25_score = 0.0; //for bm25 score
        double pr = 0.0;
        double rank = 0.0;

        int fact_num = 0;
//        BufferedWriter fact_cnt_file = new BufferedWriter(new FileWriter("fact.cnt",true));
        for (int i = 0; i < hits.length; i++) {
            Document doc = searcher.doc(hits[i].doc);
            List<IndexableField> fields = doc.getFields();
            String resource_uri = doc.get("resource_uri");
            String page_url = doc.get("page_url");
            if(doc.getField("http://www.w3.org/1999/02/22-rdf-syntax-ns#type") == null){
                continue;
            }
            if(!doc.get("http://www.w3.org/1999/02/22-rdf-syntax-ns#type").contains(type)){
                continue;
            }

            rank = i;
            bm25_score = hits[i].score;
            pr = gp.getUrlPR(page_url);

            for(IndexableField field:fields){
                if(field.name()=="page_url" || field.name() == "resource_uri")//||field.name()=="resource_uri")
                    continue;
                String field_content=field.stringValue().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\s+", " ").replaceAll("\t", " ").trim();
                String tmp_pre_str = field.name();

                String tmp_obj = field_content;
               // String replace_str = ls.judgeAndReplace(tmp_obj); // for replace the object property
                tmp_pre_str = PredicateFormatting.removeBlank(tmp_pre_str);
                if(PredicateFormatting.skip(tmp_pre_str)){
                    //    System.out.println(tmp_pre_str);
                    continue;
                }
                //filter urls
                if(skipURL.isURL(tmp_obj) || tmp_obj.contains("node")|| tmp_obj.equals("Null")||tmp_obj.equals("null")){
                    continue;
                }
                Pair<String, String> fact_pair = new Pair<String, String>(tmp_pre_str, tmp_obj);
                if (!_factFeatureSet.containsKey(fact_pair)){
                    double sum_len = _pred_freq.get(field.name());
                    double tmp_len = 0;
                    if(sum_len > 0)
                        tmp_len = (tmp_obj.length()*_pred_freq.get(field.name()) )/sum_len;
//                tmp_len = tmp_obj.length();
                    FactFeature tmp_fact = new FactFeature(pr, bm25_score, rank,
                            _subject_freq.get(resource_uri), _pred_freq.get(field.name()), tmp_len, _subject_dbsim.get(resource_uri),
                            _subject_nameExist.get(resource_uri));
                    _factFeatureSet.put(fact_pair,tmp_fact);
                }else{
                    FactFeature tmp_fact = _factFeatureSet.get(fact_pair);
                    tmp_fact.updateSubjectFeature(pr);
                    _factFeatureSet.put(fact_pair, tmp_fact);
                }

                //use score instead of rank

                /****************************************/
                if(_predicates.containsKey(tmp_pre_str)){
                    Predicate tmp_pre = _predicates.get(tmp_pre_str);
                    if(!tmp_pre.contains_object(tmp_obj))
                        fact_num += 1;
                    tmp_pre.add_object(tmp_obj, 1);
                }
                else{
                    _predicates.put(tmp_pre_str, new Predicate(tmp_pre_str, tmp_obj,1));
                    fact_num += 1;
                }
            }

            fields.clear();
        }

        gp.save_pr();
       // print_predicates(10);

        System.out.println("Build success for " + fact_num + "\t" + _factFeatureSet.size() + " facts.");
//        fact_cnt_file.write(_factFeatureSet.size()+"\n");
//        fact_cnt_file.close();
    }
    public static void print_dataset( String inpath, String type, String out_path, String out_gt, int topk) throws IOException, InterruptedException {

        System.out.println("Start print dataset for: "+inpath);
        BufferedReader br = new BufferedReader(new FileReader(inpath));
        String line;

        BufferedWriter bw  = new BufferedWriter(new FileWriter(new File(out_path), false));
        BufferedWriter bw_gt  = new BufferedWriter(new FileWriter(new File(out_gt), true));

        LinkSubject ls = new LinkSubject(type+".name");

        String pre_sub = ""; //for rank
        Integer rank = 0;
        double bm25_score = 0.0; //for bm25 score

        int fact_num = 0;

        String wiki_url = "https://en.wikipedia.org/wiki/"+_query_term.replaceAll(" ", "_");

        while((line=br.readLine())!=null){

            String[] frags = line.split("\t");
            if(frags.length < 6 )
                continue;

            if(!pre_sub.equals(frags[1])){
                rank+=1;
                pre_sub = frags[1];
                bm25_score = Double.valueOf(frags[frags.length - 2]);

            }
//            if(fact_num == 0 && bm25_score <= 3.25)break;
            //groundtruth
            if(rank > topk) break;
            //1^Inode72c7aed1fff756757de715ca83e60^Iresource_uri^Inode72c7aed1fff756757de715ca83e60^I3.546473979949951^Ihttp://www.imdb.com/title/tt0043071/$
            String tmp_pre_str = frags[2];

            String tmp_obj = frags[3];
            for(Integer j=4; j < frags.length-2; j++){
                tmp_obj = tmp_obj + frags[j];
            }

            String replace_str = ls.judgeAndReplace(tmp_obj);
            if(null != replace_str) tmp_obj = replace_str;

//            tmp_obj = ObjectFormatting.removeBlank(tmp_obj);

            tmp_pre_str = PredicateFormatting.removeBlank(tmp_pre_str);

            if( PredicateFormatting.skip(tmp_pre_str)){
                continue;
            }
            //filter urls
         //   if(skipURL.isURL(tmp_obj)||tmp_obj.startsWith("node")|| tmp_obj.equals("Null")){
            if(skipURL.isURL(tmp_obj) || tmp_obj.contains("node")|| tmp_obj.equals("Null")||tmp_obj.equals("null")){
                continue;
            }
//            tmp_pre_str = PredicateFormatting.predicateTerm(tmp_pre_str);

            String db_exist = "NO";

//            if(_dbr.factExist(PredicateFormatting.predicateTerm(tmp_pre_str), tmp_obj) || tmp_pre_str.endsWith("type")){
//                db_exist = "YES";
//            }

            if(_predicates.containsKey(tmp_pre_str)){
                Predicate tmp_pre = _predicates.get(tmp_pre_str);
                if(!tmp_pre.contains_object(tmp_obj))
                    bw_gt.write(_query+"\t"+_query_term+"\t"+wiki_url+ "\t"+ rank +"\t"+tmp_pre_str+"\t"+tmp_obj+"\n");//+"\t"+db_exist+"\n");
//                    bw_gt.write(_query+"\t"+_query_term+"\t"+tmp_pre_str+"\t"+tmp_obj+"\n");
                tmp_pre.add_object(tmp_obj, 1);
            }
            else{
                bw_gt.write(_query + "\t" + _query_term + "\t" + wiki_url+ "\t"+ rank + "\t" + tmp_pre_str + "\t" + tmp_obj +"\n");// + "\t" + db_exist + "\n");
//                bw_gt.write(_query+"\t"+_query_term+"\t"+tmp_pre_str+"\t"+tmp_obj+"\n");
                _predicates.put(tmp_pre_str, new Predicate(tmp_pre_str, tmp_obj,1));
            }

            fact_num++;
            bw.write(rank+"\t"+frags[1]+"\t"+tmp_pre_str+"\t"+tmp_obj+"\t"+bm25_score+"\t"+frags[frags.length - 1]+"\n");

        }

        br.close();
        bw.close();
        bw_gt.close();

        System.out.println("NOTICE:\tPrint dataset for type " + type + " finished.");
    }

    public Map<String,Predicate> getPredicates(){
        return _predicates;
    }
    public static void print_predicates(int k){
        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        int cnt = 0;
        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            String obj = queue.peek().best_object();

            System.out.println(pre + " \t" + obj + "\t" + queue.peek().get_score());

            queue.remove();
            cnt++;
            if(cnt == k){
                queue.clear();
                break;
            }
        }
    }
//    public static void print_all(String filename) throws IOException {
//        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), false));
//
//        output.write("<result>\n");
//        output.write("\t<query> " + _query + " </query>\n");
//        output.write("\t<facts>\n");
//        for(Map.Entry<String, Predicate> entryp:_predicates.entrySet()){
//            output.write("\t\t<fact>\n");
//            output.write("\t\t\t<predicate> "+entryp.getKey()+" </predicate>\n");
//            output.write("\t\t\t<objects>\n");
//            for (Map.Entry entryo:entryp.getValue()._objects.entrySet()){
//                //String escapedXml = StringEscapeUtils.escapeXml(entryo.getKey().toString());
//                output.write("\t\t\t\t<object> "+escapedXml+" </object>\n");
//                output.write("\t\t\t\t<object> "+escapedXml+" </object>\n");
//            }
//
//            output.write("\t\t\t</objects>\n");
//            output.write("\t\t</fact>\n");
//        }
//
//        output.write("\t</facts>\n");
//        output.write("</result>\n");
//
//        output.close();
//    }
//    public static void print_to_xml(String filename, int k, String method) throws IOException {
//        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), false));
//
//        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());
//
//        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
//            queue.add(entry.getValue());
//        }
//
//        output.write("<result>\n");
//        output.write("\t<query> " + _query + " </query>\n");
//        output.write("\t<facts>\n");
//
//        int cnt = 0;
//        while(queue.size() != 0 ){
//            String pre = queue.peek().get_predicate();
//            List<String> objs = new ArrayList<String>();
//            if(method.equals("c")){
//                objs = queue.peek().get_objects_from_cluster();
//            }
//            else if(method.equals("h")){
//                objs = queue.peek().best_objects();
//            }
//            else if(method.equals("all")){
//                for (Map.Entry entryo:queue.peek()._objects.entrySet()){
//                    objs.add(entryo.getKey().toString());
//                }
//            }
//
//            output.write("\t\t<fact>\n");
//            output.write("\t\t\t<predicate> "+pre+" </predicate>\n");
//            output.write("\t\t\t<objects>\n");
//            for(Integer i =0; i < objs.size() && i < k; i++){
//                String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
//                output.write("\t\t\t\t<object> "+escapedXml+" </object>\n");
//            }
//
//            output.write("\t\t\t</objects>\n");
//            output.write("\t\t</fact>\n");
//
//            queue.remove();
//            cnt++;
//            if(cnt == k){
//                queue.clear();
//                break;
//            }
//        }
//        output.write("\t</facts>\n");
//        output.write("</result>\n");
//
//        output.close();
//    }
    public static void print_to_tsv(String filename, int k, String method, String query_id,
                                     String gtfile, String precision_output) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), false));
        BufferedWriter eva_output = new BufferedWriter(new FileWriter(new File(precision_output), true));
        BufferedWriter output_correct = new BufferedWriter(new FileWriter(new File(_type+".correct.tsv"), true));

        PrecisionEvaluation pe = new PrecisionEvaluation(gtfile, _type, 0.0);
//        if(query_id.equals("303"))pe.printGT();
        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        int cnt = 0;
        Double positive = 0.0;
        Double negtive =0.0;

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            if(method.equals("c")){
                Pair<List<String>, List<Double>> tmp_pair = queue.peek().get_objects_from_cluster(0.5);
                objs = tmp_pair.getKey();

                //feature extraction
                List<Double> clst_sizes = tmp_pair.getValue();
                if(objs.size() != clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(1);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();
                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < clst_sizes.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(1, clst_sizes.size(), clst_sizes.get(i), average, variance);
                        //private double _varience; // varience of the object frequency/ cluster size
//            private double _average;
                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }

            }
            else if(method.equals("h")){
                objs = queue.peek().best_objects();
            }
            else if(method.equals("bm25")){
                for(Map.Entry<String, Double> objentry: queue.peek()._objects.entrySet()){
                    objs.add(objentry.getKey().toString());
                }
            }
            else if(method.equals("all")){
                for (Map.Entry entryo:queue.peek()._objects.entrySet()){
                    objs.add(entryo.getKey().toString());
                }
            }

            for(Integer i =0; i < objs.size() && i < k; i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                output.write(_query + "\t" + pre + "\t" + objs.get(i).toString() + "\n ");

                int judgeValue=pe.judgeFact(query_id, pre, objs.get(i).toString());
               // System.out.println(query_id+"\t"+pre+"\t" + objs.get(i).toString()+"\t"+judgeValue+"\n ");

                if( judgeValue == 1 ){
                    positive += 1;
                    output_correct.write(_query +"\t"+_query_term+ "\t" + pre + "\t" + objs.get(i).toString() + "\n");
                }
                else if(judgeValue==0) negtive +=1;
            }

            queue.remove();
            cnt++;
            if(cnt == k){
                queue.clear();
                break;
            }
        }

        Double precision=0.0;
        if(positive+negtive!=0)precision = (Double)positive/(positive+negtive);
        eva_output.write(query_id+"\t"+_query_term+"\t"+positive+"\t"+negtive+"\t"+precision+"\t"+method+"\n");
        output.close();
        output_correct.close();
        eva_output.close();
    }
    public static void print_with_feature_precision(String filename, int k, String method, String query_id,
                                    String precision_output, boolean[] f) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), false));
        BufferedWriter eva_output = new BufferedWriter(new FileWriter(new File(precision_output), true));

        PrecisionEvaluationSingle pe = new PrecisionEvaluationSingle("Results_"+_type+".json",query_id, _type, 0);
//        if(query_id.equals("303"))pe.printGT();
        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        int cnt = 0;
        Double positive = 0.0;
        Double negtive =0.0;

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            if(method.equals("c")){
                Pair<List<String>, List<Double>> tmp_pair = queue.peek().get_objects_from_cluster(0);
                objs = tmp_pair.getKey();

                //feature extraction
                List<Double> clst_sizes = tmp_pair.getValue();
                if(objs.size() != clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(1);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();
                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < clst_sizes.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(1, clst_sizes.size(),clst_sizes.get(i), average, variance);
                        //private double _varience; // varience of the object frequency/ cluster size
//            private double _average;
                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }
            }
            for(Integer i =0; i < objs.size() && i < k; i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                String feature_str = _factFeatureSet.get(new Pair<String, String>(pre, objs.get(i).toString())).make_string(f);
                output.write(_query + "\t" + pre + "\t" + objs.get(i).toString() + "\t"+feature_str+"\t"+"\n ");

                int judgeValue=pe.judgeFact(query_id, pre, objs.get(i).toString());
                // System.out.println(query_id+"\t"+pre+"\t" + objs.get(i).toString()+"\t"+judgeValue+"\n ");

                if( judgeValue == 1 ){
                    positive += 1;

                }
                else if(judgeValue==0) negtive +=1;
            }

            queue.remove();
            cnt++;
            if(cnt == k){
                queue.clear();
                break;
            }
        }
        Double precision=0.0;
        if(positive+negtive!=0)precision = (Double)positive/(positive+negtive);
        eva_output.write(query_id + "\t" + _query + "\t" + positive + "\t" + negtive + "\t" + precision + "\t" + method + "\n");
        output.close();
        eva_output.close();
    }
    public static void print_with_feature(String filename, int k, String method, String query_id, boolean[] f) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), false));

        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        int cnt = 0;

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            if(method.equals("c")){
                Pair<List<String>, List<Double>> tmp_pair = queue.peek().get_objects_from_cluster(0);
                objs = tmp_pair.getKey();

                //feature extraction
                List<Double> clst_sizes = tmp_pair.getValue();
                if(objs.size() != clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(1);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();
                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < clst_sizes.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(1, clst_sizes.size(),clst_sizes.get(i), average, variance);
                        //private double _varience; // varience of the object frequency/ cluster size
//            private double _average;
                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }
            }
            for(Integer i =0; i < objs.size() && i < k; i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                String feature_str = _factFeatureSet.get(new Pair<String, String>(pre, objs.get(i).toString())).make_string(f);
                output.write(_query + "\t" + pre + "\t" + objs.get(i).toString() + feature_str +"\n ");
            }

            queue.remove();
            cnt++;
            if(cnt == k){
                queue.clear();
                break;
            }
        }

        output.close();
    }
    public static void printTrainTestDataCentroid(String filename, int k, String method, String query_id, String gt, boolean[] f) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), true));

        PrecisionEvaluationSingle pe = new PrecisionEvaluationSingle(gt,query_id, _type, 0);
//        if(query_id.equals("303"))pe.printGT();
        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            if(method.equals("c")){
                Pair<List<String>, List<Double>> tmp_pair = queue.peek().get_objects_from_cluster(0);
                objs = tmp_pair.getKey();

                //feature extraction
                List<Double> clst_sizes = tmp_pair.getValue();
                if(objs.size() != clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(1);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();
                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < clst_sizes.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(1, clst_sizes.size(),clst_sizes.get(i), average, variance);
                        //private double _varience; // varience of the object frequency/ cluster size
//            private double _average;
                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }
            }

          //  Set<String> centers = new HashSet<String>(objs);

            for(Integer i =0; i < objs.size() && i < k; i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                String obj = objs.get(i).toString();
                String feature_str = _factFeatureSet.get(new Pair<String, String>(pre, obj)).make_string(f);
                int judgeValue=pe.judgeFact(query_id, pre, obj);
                if(judgeValue<0)continue;

                output.write(_query+"\t"+_query_term+"\t"+pre+"\t"+obj+"\t"+String.valueOf(judgeValue) + "\t"+feature_str+"\n");
                // System.out.println(query_id+"\t"+pre+"\t" + objs.get(i).toString()+"\t"+judgeValue+"\n ");
            }
            queue.remove();
        }
        output.close();
    }

    public static void printTrainTestDataAll(String filename, int k, String method, String query_id, String gt, boolean[] f) throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), true));

        PrecisionEvaluation pe = new PrecisionEvaluation(gt, _type, 0);
//        if(query_id.equals("303"))pe.printGT();
        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            List<Integer> isCenters = new ArrayList<Integer>();
            List<Double> clst_sizes = new ArrayList<Double>();
            List<Double> obj_clst_sizes = new ArrayList<Double>();

            if(method.equals("c")){
                Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > >  tmp_pair = queue.peek().get_all_objects_from_cluster(0);
                objs = tmp_pair.getKey().getKey();
                isCenters = tmp_pair.getKey().getValue();
                obj_clst_sizes = tmp_pair.getValue().getKey();
                clst_sizes = tmp_pair.getValue().getValue();

                if(objs.size() != obj_clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(5);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();

                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < objs.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(isCenters.get(i), clst_sizes.size(),obj_clst_sizes.get(i), average, variance);

                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }
            }

          //  Set<String> centers = new HashSet<String>(objs);

            for(Integer i =0; i < objs.size() && i < k; i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                String obj = objs.get(i).toString();
                Pair<String, String> tmp_pair = new Pair<String, String>(pre, obj );
                if(!_factFeatureSet.containsKey(tmp_pair)){
                    continue;
                }
                String feature_str = _factFeatureSet.get(tmp_pair).make_string(f);
                Integer isCenter = _factFeatureSet.get(tmp_pair).isCenter();
                _factFeatureSet.remove(tmp_pair);
                int judgeValue=pe.judgeFact(query_id, pre, objs.get(i).toString());
                if(judgeValue<0)continue;

//                if(judgeValue == 1)judgeValue = _subject_nameExist.get();
                //output.write(String.valueOf(judgeValue) + "\t"+feature_str+"\n");
                output.write(_query+"\t"+_query_term+"\t"+pre+"\t"+obj+"\t"+ isCenter
                        +"\t"+String.valueOf(judgeValue)
                        + feature_str+"\n");
                // System.out.println(query_id+"\t"+pre+"\t" + objs.get(i).toString()+"\t"+judgeValue+"\n ");
            }
            queue.remove();
        }
        output.close();
    }
    public static List<FactDenseInstance> getTestData( boolean []f) throws IOException {

        List<FactDenseInstance> out = new ArrayList<>();

        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());

        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
            queue.add(entry.getValue());
        }

        while(queue.size() != 0 ){
            String pre = queue.peek().get_predicate();
            List<String> objs = new ArrayList<String>();
            List<Integer> isCenters = new ArrayList<Integer>();
            List<Double> clst_sizes = new ArrayList<Double>();
            List<Double> obj_clst_sizes = new ArrayList<Double>();


                Pair<Pair<List<String>,List<Integer> >, Pair<List<Double>, List<Double> > >  tmp_pair = queue.peek().get_all_objects_from_cluster(0);
                objs = tmp_pair.getKey().getKey();
                isCenters = tmp_pair.getKey().getValue();
                obj_clst_sizes = tmp_pair.getValue().getKey();
                clst_sizes = tmp_pair.getValue().getValue();

                if(objs.size() != obj_clst_sizes.size()){
                    System.out.println("ERROR:\tThe size of cluster number and objects are different.");
                    System.exit(5);
                }
                double variance = 0.0;
                double average = 0.0;
                if(clst_sizes.size()>0) average = queue.peek()._objects.size()/clst_sizes.size();

                for(int i = 0; i < clst_sizes.size(); i++){
                    variance = variance + Math.pow(clst_sizes.get(i) - average, 2);
                }
                variance = Math.sqrt(variance);

                for(int i = 0; i < objs.size(); i++){
                    Pair<String, String> tmp_fact = new Pair<String,String>(pre, objs.get(i));

                    if(_factFeatureSet.containsKey(tmp_fact)){
                        FactFeature tmp_feature = _factFeatureSet.get(tmp_fact);
                        tmp_feature.updateClusterFeature(isCenters.get(i), clst_sizes.size(),obj_clst_sizes.get(i), average, variance);

                        _factFeatureSet.put(tmp_fact,tmp_feature);
                    }
                }

            //  Set<String> centers = new HashSet<String>(objs);

            for(Integer i =0; i < objs.size(); i++){
                //String escapedXml = StringEscapeUtils.escapeXml(objs.get(i));
                String obj = objs.get(i).toString();
                Pair<String, String> t_pair = new Pair<String, String>(pre, obj );
                if(!_factFeatureSet.containsKey(t_pair)){
                    continue;
                }
                double[] feature_vec = _factFeatureSet.get(t_pair).make_vec(f).clone();
                Integer isCenter = _factFeatureSet.get(t_pair).isCenter();
                _factFeatureSet.remove(t_pair);

                String[] properties = new String[5];
                properties[0] = _query;
                properties[1] = _query_term;
                properties[2] = pre;
                properties[3] = obj;
                properties[4] = isCenter.toString();

                if(isCenter == 1) {
                    out.add(new FactDenseInstance(properties, feature_vec, 0));
                }
            }
            queue.remove();
        }
        return out;
    }
}
