package edu.l3s.algorithm;

import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.Node;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ranyu on 10/12/15.
 */
public class LinkSubject {
    Map<String, String> _sub_dict;
    public LinkSubject(String inpath) throws IOException {
        _sub_dict = new HashMap<String, String>();
        load_dict(inpath);
    }
    private void load_dict(String inpath) throws IOException {
        FileInputStream is = new FileInputStream(inpath);
        NxParser nxp = new NxParser(is);

        for(Node[] nxx: nxp){
            if(4 != nxx.length) continue;
            String tmp_str = nxx[0].toString();
            String sub = tmp_str;
            if(tmp_str.startsWith("_:")){
                sub = tmp_str.substring(2);
            }
            if(tmp_str.startsWith("<")){
                sub = tmp_str.substring(1,tmp_str.length()-1);
            }
            if(!_sub_dict.containsKey(sub)){
                _sub_dict.put(sub,nxx[3].toString());
            }
            else if(nxx[1].toString().endsWith("/name")){
                _sub_dict.put(nxx[0].toString(),nxx[2].toString().replace("\n","").replaceAll("\r","").replace("\t"," ").trim());
            }
        }
        is.close();

//        BufferedWriter output = new BufferedWriter(new FileWriter(new File("Movie.subject.name"), false));
//        for(Map.Entry entry: _sub_dict.entrySet()){
//            output.write(entry.getKey()+"\t"+entry.getValue()+"\n");
//        }
//        output.close();
    }
    private void load_dict_new(String inpath) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inpath));
        String line;
        while ((line = br.readLine()) != null) {
            String[] strs = line.split("\t");
            if (2 != strs.length) continue;
            _sub_dict.put(strs[0], strs[1]);
        }

        br.close();
    }
    public String judgeAndReplace(String subject){
        if(_sub_dict.containsKey(subject)) return _sub_dict.get(subject);
        return null;
    }
}
