package edu.l3s.utils;

import org.apache.commons.collections.FastHashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ranyu on 4/1/16.
 */
public class StringSimilarity {
    public static Double letterSim(String x, String y){
        Double score;

        String strx = x.toLowerCase();
        String stry = y.toLowerCase();

        char[] arrx = strx.toCharArray();
        char[] arry = stry.toCharArray();

        double [] vx = new double[36];
        double [] vy = new double[36];

        for(char charx : arrx){
            if (charx>47&&charx<58){
                vx[charx-48] += 1;
            }
            else if(charx>96&&charx<123){
                vx[charx-87] += 1;
            }
        }
        for(char chary : arry){
            if (chary>47&&chary<58){
                vx[chary-48] += 1;
            }
            else if(chary>96&&chary<123){
                vx[chary-87] += 1;
            }
        }


        return cosineSim(vx, vy);
    }

    public static Double tokenSim(String x, String y){
        Double score;

        Map<String, Double> dict = new HashMap<String, Double>();

        double [] vx = new double[36];
        double [] vy = new double[36];




        return cosineSim(vx, vy);
    }

    public static boolean isSameMainStr(String x, String y){
        if(x.equals(y) || x.contains(y) || y.contains(x)){
            return true;
        }
        String[] segx = x.split("\\(");
        x = segx[0];

        String[] segy = y.split("\\(");
        y = segy[0];

        x.replaceAll("_"," ");
        y.replaceAll("_"," ");

//        return (x.equals(y) || x.contains(y) || y.contains(x));
        return (x.equals(y) || y.contains(x));
    }

    public static Double cosineSim(double [] x, double [] y){
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < x.length; i++) {
            dotProduct += x[i] * y[i];
            normA += Math.pow(x[i], 2);
            normB += Math.pow(y[i], 2);
        }
        if(Math.sqrt(normA) * Math.sqrt(normB) == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
