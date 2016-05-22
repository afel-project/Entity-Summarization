package edu.l3s.algorithm;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;
import edu.l3s.dataStructure.Predicate;
import edu.l3s.utils.PredicateFormatting;
import edu.l3s.utils.StringSimilarity;
import edu.l3s.utils.skipURL;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by ranyu on 4/19/16.
 */
public class NameChecker {
    private Set<String> _db_names;
//    private String _qid;
//    private String _query;
    public NameChecker(String query_term){
        String db_url = "http://dbpedia.org/resource/"+query_term.replaceAll(" ", "_");
        _db_names = get_db_names(db_url);
        _db_names.add(query_term);
//        _qid = query_id;
//        _query = query_term;
    }

    public Integer nameExist(Set<String> names){
        for(String dbs: _db_names){
            for(String subs: names){
                if(StringSimilarity.isSameMainStr(dbs, subs)){
                    return 1;
                }
            }
        }
        return 0;
    }
    public static void  print_with_label(String type, String queryfile, Integer start_index, Integer end_index, Integer topk) throws IOException, InterruptedException {

        BufferedReader br = new BufferedReader(new FileReader(queryfile));

        String line;

        while((line=br.readLine()) != null){
            String[] tmps = line.split("\t");
            if(tmps.length<2)continue;

            //groundtruth
            if(Integer.valueOf(tmps[0])< start_index) continue;
            if(Integer.valueOf(tmps[0])>end_index) break;

            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
            String output = "filter_name_result/"+type+"/"+type+"_"+tmps[0]+".filtered";
            print_dataset(tmps[0], tmps[1], input, output, type, topk);
        }
        br.close();
    }
    public static void print_dataset(String qid, String query_term, String inpath, String outpath, String type, Integer topk) throws IOException, InterruptedException {

        System.out.println("Start print dataset for: "+inpath);
        BufferedReader br = new BufferedReader(new FileReader(inpath));
        String line;

        BufferedWriter bw  = new BufferedWriter(new FileWriter(new File(outpath), false));

        String pre_sub = ""; //for rank
        Integer rank = 0;
        Double bm25_score = 0.0;

        int fact_num = 0;

        String db_url = "http://dbpedia.org/resource/"+query_term.replaceAll(" ", "_");
        Set<String> db_names = get_db_names(db_url);
        db_names.add(query_term);

        Set<String> sub_names = new HashSet<String>();

        List<String> lines = new ArrayList<String>();

        while((line=br.readLine())!=null){

            String[] frags = line.split("\t");
            if(frags.length < 6 ){
                lines.add(line);
                continue;
            }

            if(!pre_sub.equals(frags[1])){
                String jm = judge(db_names, sub_names);
                print_sub(jm, lines, bw);

                rank+=1;
                pre_sub = frags[1];
                sub_names.clear();
                lines.clear();
            }
            if(rank > topk) break;
            lines.add(line);

            String tmp_pre_str = frags[2];

            String tmp_obj = frags[3];
            for(Integer j=4; j < frags.length-2; j++){
                tmp_obj = tmp_obj + frags[j];
            }

            if(tmp_pre_str.endsWith("name")) sub_names.add(tmp_obj);
        }
        String jm = judge(db_names, sub_names);
        print_sub(jm, lines, bw);

        br.close();
        bw.close();

        System.out.println("NOTICE:\tPrint dataset for type " + type + " finished.");
    }
    public static void print_dataset_class(String qid, String query_term, String inpath, String outpath, String type, Integer topk,String clabel) throws IOException, InterruptedException {

        System.out.println("Start print dataset for: "+inpath);
        BufferedReader br = new BufferedReader(new FileReader(inpath));
        String line;

        BufferedWriter bw  = new BufferedWriter(new FileWriter(new File(outpath), true));

        String pre_sub = ""; //for rank
        Integer rank = 0;
        Double bm25_score = 0.0;

        int fact_num = 0;

        String db_url = "http://dbpedia.org/resource/"+query_term.replaceAll(" ", "_");
        Set<String> db_names = get_db_names(db_url);
        db_names.add(query_term);

        Set<String> sub_names = new HashSet<String>();

        List<String> lines = new ArrayList<String>();

        while((line=br.readLine())!=null){

            String[] frags = line.split("\t");
            if(frags.length < 6 ){
                lines.add(line);
                continue;
            }

            if(!pre_sub.equals(frags[1])){
                String jm = judge(db_names, sub_names);
                if(jm.equals(clabel)) {
                    print_query_sub(type, qid, query_term,jm, lines, bw);
                }
                rank+=1;
                pre_sub = frags[1];
                sub_names.clear();
                lines.clear();
            }
            if(rank > topk) break;
            lines.add(line);

            String tmp_pre_str = frags[2];

            String tmp_obj = frags[3];
            for(Integer j=4; j < frags.length-2; j++){
                tmp_obj = tmp_obj + frags[j];
            }

            if(tmp_pre_str.endsWith("name")) sub_names.add(tmp_obj);
        }
        String jm = judge(db_names, sub_names);
        if(jm.equals(clabel)) {
            print_query_sub(type,qid, query_term,jm, lines, bw);
        }
        br.close();
        bw.close();

        System.out.println("NOTICE:\tPrint dataset for type " + type + " finished.");
    }
    private static void print_sub(String jm, List<String> lines, BufferedWriter bw) throws IOException {
//        if(jm.equals("incorrect")) return;
        for(String line: lines){
//            bw.write(line+"\n");
            bw.write(jm+"\t"+line+"\n");
        }
    }
    private static void print_query_sub(String type, String qid, String query, String jm, List<String> lines, BufferedWriter bw) throws IOException {

        String pred_str = "Irrelevant Entity";
        String obj_str = "Irrelevant Entity";
        String sub_id = "";
        String sub_rank = "";
        String wiki_url = "https://en.wikipedia.org/wiki/"+query.replaceAll(" ", "_");

        for(String line: lines){
//            bw.write(line+"\n");
            String [] segs = line.split("\t");
            if(segs.length != 6) continue;

            if(sub_id.equals("")){
                sub_id = segs[1];
                sub_rank = segs[0];

//                pred_str = segs[2].replaceAll("schema.org/"+type,"schema.org");
//                obj_str = segs[3];
            }

                pred_str = pred_str + "`" + segs[2].replaceAll("schema.org/"+type,"schema.org");
                obj_str = obj_str + "`" + segs[3];

        }
        bw.write(qid+"\t"+query+"\t"+wiki_url+"\t"+sub_rank+ "\t" + sub_id +"\t" + pred_str + "\t" + obj_str+"\n");
    }
    private static Set<String> get_db_names(String db_url){
        Set<String> names = new HashSet<String>();

        return names;
    }
    private static String judge(Set<String> db_name, Set<String> sub_name){
        for(String dbs: db_name){
            for(String subs: sub_name){
                if(StringSimilarity.isSameMainStr(dbs, subs)){
                    return "correct";
                }
            }
        }
        return "incorrect";
    }
    public static void  print_class(String type, String queryfile, Integer start_index, Integer end_index, Integer topk, String clabel) throws IOException, InterruptedException {

        BufferedReader br = new BufferedReader(new FileReader(queryfile));

        String output = "filter_name_" +type+"."+ clabel+".tsv";
        File f = new File(output);
        f.delete();

        String line;

        while((line=br.readLine()) != null){
            String[] tmps = line.split("\t");
            if(tmps.length<2)continue;

            //groundtruth
            if(Integer.valueOf(tmps[0])< start_index) continue;
            if(Integer.valueOf(tmps[0])>end_index) break;

            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
            print_dataset_class(tmps[0], tmps[1], input, output, type, topk, clabel);
        }
        br.close();
    }
}
