package edu.l3s.utils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by ranyu on 2/18/16.
 */

public class skipURL {
    public static boolean isURL(String str){

        if(str.contains("schema.org")) return false;
        URL u = null;

        try {
            u = new URL(str);
        } catch (MalformedURLException e) {
            return false;
        }

        try {
            u.toURI();
        } catch (URISyntaxException e) {
            return false;
        }

        return true;
    }
    public boolean urlAvailable(String str){
        if(isURL(str)){
            try {
                URL url = new URL(str);
                URLConnection conn = url.openConnection();
                conn.connect();
            } catch (MalformedURLException e) {
                return false;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }
}
