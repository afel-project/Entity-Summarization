package edu.l3s.utils;

/**
 * Created by ranyu on 7/8/15.
 */

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class GooglePR {

    private static Map<String, Double> _domain_pr;

    public GooglePR() throws IOException {
        _domain_pr = new HashMap<String, Double>();
        load_pr();
    }

    private void load_pr() throws IOException {
        BufferedReader br = new BufferedReader(new FileReader("DomainPR"));
        String line;

        while((line = br.readLine()) != null){
            String[] segs = line.split("\t");

            if(segs.length != 2) continue;

            Double score = Double.valueOf(segs[1]);
            if(Math.abs(score) < 1E-13) continue;

            _domain_pr.put(segs[0], score);
        }
        br.close();
    }
    public static void main(String[] args) throws IOException, InterruptedException {

        GooglePR obj = new GooglePR();
//        System.out.println(obj.getUrlPR("http://www.futuresbroker.com.au/directory/wa/eastern-suburbs-perth/"));
    }

    public static Double getUrlPR(String url) throws InterruptedException {
        URI uri = null;
        Double pr;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String domain = uri.getHost();

        if(_domain_pr.containsKey(domain)){
            pr = _domain_pr.get(domain);
//            System.out.println("Got pagerank for: "+domain);
        }
        else{
            Thread.sleep(2000);
            pr = GooglePR.getPR(domain);
            _domain_pr.put(domain, pr);
        }

        return pr;
    }

    public static void save_pr() throws IOException {
        BufferedWriter output = new BufferedWriter(new FileWriter(new File("DomainPR")));

        for(Map.Entry<String, Double> entry: _domain_pr.entrySet()){
            output.write(entry.getKey()+"\t"+entry.getValue()+"\n");
        }
        output.close();
    }

    public static String getUrlDomain(String url){
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        String domain = uri.getHost();
        return domain;
    }

    public static Double getPR(String domain) {

        String result = "";

        JenkinsHash jenkinsHash = new JenkinsHash();
        long hash = jenkinsHash.hash(("info:" + domain).getBytes());

        //Append a 6 in front of the hashing value.
        String url = "http://toolbarqueries.google.com/tbr?client=navclient-auto&hl=en&"
                + "ch=6" + hash + "&ie=UTF-8&oe=UTF-8&features=Rank&q=info:" + domain;

        System.out.println("Sending request to : " + url);

        try {
            URLConnection conn = new URL(url).openConnection();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));

            String input;
            while ((input = br.readLine()) != null) {

                // What Google returned? Example : Rank_1:1:9, PR = 9
                System.out.println(input);

                result = input.substring(input.lastIndexOf(":") + 1);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        if ("".equals(result)) {
            return 0.0;
        } else {
            return Double.valueOf(result);
        }

    }
}
