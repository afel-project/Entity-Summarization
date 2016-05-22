package edu.l3s.algorithm;

/**
 * Created by yuran on 15/12/15.
 */

import edu.l3s.dataStructure.peAbstractor;
import edu.l3s.dataStructure.pePredicate;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

import javafx.util.Pair;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class PrecisionEvaluation {
    private static Map<Pair<String,String>, pePredicate> _gt;

    public PrecisionEvaluation(String path, String type, double confidience) throws IOException {
        _gt = new HashMap<Pair<String, String> , pePredicate>();
        if(path.endsWith(".json")){
            readGTjson(path, type, confidience);
        }
        else if(path.endsWith(".tsv")){
            readGTtsv(path, type, confidience);
        }
    }
    private void readGTtsv(String path, String type, double Conf) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(new File(path)));

        String line = null;
        while( (line = br.readLine() ) != null){
            String [] segs = line.split("\t");
            if(segs.length < 5) continue;
            String pred = segs[2];
            if(pred.contains("http://schema.org/")&&(!pred.contains("http://schema.org/"+type)))
                pred = pred.replaceAll("http://schema.org", "http://schema.org/"+type);

            Double judgement = -1.0;

            if (segs[4].equals("YES")) {
                judgement = 1.0;
            } else if (segs[4].equals("NO")) {
                judgement = 0.0;
            }

            Pair<String, String> key = new Pair<String,String>(segs[0],pred);
            if (_gt.containsKey(key)) {
                pePredicate tmp_pred = _gt.get(key);
                tmp_pred.put_object(segs[3], judgement);
                _gt.put(key, tmp_pred);

            } else {
                pePredicate tmp_pred = new pePredicate(pred, segs[3], judgement);
                _gt.put(key, tmp_pred);

            }
        }
        br.close();
    }
    private void readGTjson(String path, String type, double Conf) throws IOException {
        FileReader fileReader = new FileReader(new File(path));

        BufferedReader br = new BufferedReader(fileReader);

        String line = null;
        int cnt_1 = 0, cnt_2 = 0, cnt_3 = 0, cnt_4 = 0;
//        BufferedWriter output = new BufferedWriter(new FileWriter(new File("Movie.gt"), false));
//        double avg_conf = 0.0;int line_num = 0;

        while ((line = br.readLine()) != null) {
            //  System.out.println(line);
            // reading lines until the end of the file


            Object jobj = JSONValue.parse(line);

            JSONObject jsonObject = (JSONObject) jobj;
            JSONObject data = (JSONObject) jsonObject.get("data");

            String query_id = (String) data.get("id");
//            String query_id = (String) data.get("query_id");

            String pred = (String) data.get("pred");
//            String pred = (String) data.get("predicate");
            if(pred.contains("http://schema.org/")&&(!pred.contains("http://schema.org/"+type)))
                pred = pred.replaceAll("http://schema.org", "http://schema.org/"+type);
            //   System.out.println(pred);

            String obj = (String) data.get("obj");
//            String obj = (String) data.get("object");
            //   System.out.println(obj);

            JSONObject results = (JSONObject) jsonObject.get("results");
            JSONObject question = (JSONObject) results.get("question");
            String agg = (String) question.get("agg");
            Double confidence = Double.valueOf(question.get("confidence").toString());

//            avg_conf+=confidence;
//            line_num+=1;

            //    System.out.println(query_id + "\t" + pred + "\t" + obj + "\t" + agg + "\n");

//            output.write(query_id+"\t"+pred+"\t"+obj+"\t"+agg+"\t"+"conf");
            Double judgement = -1.0;
            if (agg.equals("YES")) {
                if(confidence >= Conf){
                    judgement = 1.0;
                }
                cnt_1 += 1;
            } else if (agg.equals("NO")) {
                if(confidence >= Conf)judgement = 0.0;
                cnt_2 += 1;
            } else if (agg.startsWith("URL")) {
                cnt_3 += 1;
                continue;
            } else if (agg.startsWith("Sufficient")) {
                cnt_4 += 1;
                continue;
            } else {
                System.out.println("Wrong judgement: ***" + agg + "***");
                //System.exit(1);
                continue;
            }

            Pair<String, String> key = new Pair<String, String>(query_id, pred);
            if (_gt.containsKey(key)) {
                pePredicate tmp_pred = _gt.get(key);
                tmp_pred.put_object(obj, judgement);
                _gt.put(key, tmp_pred);

            } else {
                pePredicate tmp_pred = new pePredicate(pred, obj, judgement);
                _gt.put(key, tmp_pred);

            }
        }
//        output.close();
        br.close();
//        System.out.println("Average confidence:\t"+ avg_conf/line_num);
//        System.exit(11);
//        System.out.println(cnt_1+"\tYES");
//        System.out.println(cnt_2+"\tNO");
//        System.out.println(cnt_3+"\tURL(s) not available");
//        System.out.println(cnt_4+"\tSufficient information is not available although the URLs are available");
    }

    public void printGT(){

        for(Map.Entry<Pair<String,String> , pePredicate> predentry: _gt.entrySet()){
            System.out.println(predentry.getKey().getKey() + "\t" + predentry.getKey().getValue());
            for(Map.Entry<String, Double> objentry: predentry.getValue()._objects.entrySet()){
                System.out.println("\t"+objentry.getKey()+"\t"+objentry.getValue());
            }
        }
    }
    public void printGT2(){
        for(Map.Entry<Pair<String,String> , pePredicate> predentry: _gt.entrySet()){
            System.out.println(predentry.getKey().getKey() + "\t" + predentry.getKey().getValue());
            for(Map.Entry<String, Double> objentry: predentry.getValue()._objects.entrySet()){
                System.out.println("\t"+objentry.getKey()+"\t"+objentry.getValue());
            }
        }

    }
    public int judgeFact(String qid, String pred, String obj){
        Pair<String, String> key = new Pair<String, String>(qid, pred);
        if(_gt.containsKey(key)) return _gt.get(key).judgeFact(obj);
        else return -1;
    }
//    private void readGT(String path) throws IOException {
////        File csvData = new File(path);
//        CSVParser parser = CSVParser.parse(path, CSVFormat.RFC4180);
//        for (CSVRecord csvRecord : parser) {
//
//        }
//
//    }
}
