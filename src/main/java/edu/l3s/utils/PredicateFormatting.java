package edu.l3s.utils;

/**
 * Created by ranyu on 4/5/16.
 */
public class PredicateFormatting {
    public static String predicateTerm(String pred){
        String [] pred_seg = pred.split("/");
        String pre_last = pred_seg[pred_seg.length-1];

        String [] pred_seg2 = pred_seg[pred_seg.length-1].split("#");
        String res = pred_seg2[pred_seg2.length-1];

        return res;
    }
    public static String removeBlank(String pred){
        String tmp_pre_str = pred;
        tmp_pre_str = tmp_pre_str.replaceAll("\t", "");
        tmp_pre_str = tmp_pre_str.replaceAll("\n", "");
        tmp_pre_str = tmp_pre_str.replaceAll(" ", "");

        return tmp_pre_str;
    }
    public static boolean skip(String tmp_pre_str){
        if(tmp_pre_str == ""|| tmp_pre_str.equals("resource_uri") || tmp_pre_str.equals("page_url")
                ||tmp_pre_str.endsWith("/url") || tmp_pre_str.endsWith("#type") ){
            //    System.out.println(tmp_pre_str);
            return true;
        }
        else return false;
    }
}
