package edu.l3s.utils;

/**
 * Created by ranyu on 4/5/16.
 */
public class ObjectFormatting {
    public static String removeBlank(String obj){
        String tmp_obj = obj;

        tmp_obj =tmp_obj.replaceAll("^\\n+", "");
        tmp_obj =tmp_obj.replaceAll("^\\r+", "");
        tmp_obj =tmp_obj.replaceAll("\\r+$", "");
        tmp_obj =tmp_obj.replaceAll("\\n+$", "");
        tmp_obj = tmp_obj.replaceAll("^\\s+", "");
        tmp_obj =tmp_obj.replaceAll("\\s+$", "");
        tmp_obj =tmp_obj.replaceAll("^\\t+", "");
        tmp_obj =tmp_obj.replaceAll("\\t+$", "");
        tmp_obj =tmp_obj.replaceAll("\t", " ");

        return tmp_obj;
    }
    public static String formatObj(String obj){
        String tmp_obj = obj;

//        if(obj.startsWith("\"")){
//            String [] obj_seg2 = tmp_obj.split("\"");
//            if(obj_seg2.length >= 3) tmp_obj = obj_seg2[1];
//        }
        tmp_obj =tmp_obj.replaceAll("\"", "");
        tmp_obj =tmp_obj.replaceAll("-", "");
        tmp_obj =tmp_obj.replaceAll("@en", "");


        if(tmp_obj.contains("/")){
            String [] obj_seg = tmp_obj.split("/");
            tmp_obj = obj_seg[obj_seg.length-1];
        }
        if(tmp_obj.contains("(")){
            tmp_obj = tmp_obj.substring(0,tmp_obj.indexOf("("));
        }
        tmp_obj = tmp_obj.toLowerCase();
        tmp_obj = tmp_obj.replaceAll("_", " ");
        tmp_obj = tmp_obj.replaceAll(",", "");
        tmp_obj = tmp_obj.replaceAll("\\s+", "");


        tmp_obj = removeBlank(tmp_obj);

        return tmp_obj;
    }
}
