//package Abstract;
//
//import org.apache.lucene.search.TopDocs;
//
//import java.io.BufferedWriter;
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.util.*;
//
///**
// * Created by ranyu on 9/1/15.
// */
//public class Abstractor {
//
//    static class PredicateComparator implements Comparator<Predicate> {
//
//        @Override
//        public int compare(Predicate item1, Predicate item2) {
//            return item2.get_score().compareTo(item1.get_score());
//        }
//
//    }
//    private static class Predicate{
////        private class Object{
////            private String _content;
////            private double _score;
////
////            public void Object(String content, double score){
////                content = _content;
////                score = _score;
////            }
////
////            public boolean
////        }
//        private Double _score;
//        private String _content;
//        private Map<String, Double> _objects;
//
//        private final Double _alpha = 0.9;
//
//        public Predicate(){
//        }
//        public Predicate(String pre, String obj, int rank){
//            _score = 1.0;
//            _content = pre;
//            _objects = new HashMap<String, Double>();
//            _objects.put(obj, Math.pow(_alpha, rank));
//        }
//
//        public void add_object(String obj, Integer rank){
//            _score+=1.0;
//
//            if(_objects.containsKey(obj)){
//                _objects.put(obj, _objects.get(obj)+Math.pow(_alpha, rank));
//            }
//            else{
//                _objects.put(obj, Math.pow(_alpha, rank));
//            }
//        }
//        public String best_object(){
//            Map.Entry<String, Double> maxEntry = null;
//
//            for (Map.Entry<String, Double> entry : _objects.entrySet())
//            {
//                if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
//                {
//                    maxEntry = entry;
//                }
//            }
//
//            return maxEntry.getKey();
//        }
//        public String get_predicate(){
//            return _content;
//        }
//
//        public Double get_score(){
//            return _score;
//        }
////        public int compareTo(Predicate obj) {
////            int deptComp = _score.compareTo(obj.get_score());
////
////            return deptComp;
////        }
//
//    }
//
//    private static Map<String, Predicate> _predicates;
//    private static String _query;
//
//    public Abstractor(String query, TopDocs result){
//        _query = query;
//        _predicates = new HashMap<String, Predicate>();
//
//        build_abstract(result);
//    }
//    public Abstractor(String query, String result_file){
//        _query = query;
//        _predicates = new HashMap<String, Predicate>();
//        TopDocs result = read_from_file(result_file);
//        build_abstract(result);
//    }
//
//    private TopDocs read_from_file(String path){
//        TopDocs result;
//        re
//    }
//
//
//    public static void build_abstract(TopDocs result){
//
//        List<QueryResultItem> resultItems = result.getResultItems();
//
//
//        for(int i = 0; i < resultItems.size(); i++){
//            QueryResultItem tmp_item = resultItems.get(i);
//
//            List<QueryResultItem.Relation> relations = tmp_item.getRelations();
//
//            for(int j = 0; j < relations.size(); j++){
//                QueryResultItem.Relation tmp_rel = relations.get(j);
//                String tmp_pre_str = tmp_rel.getPredicate();
//                String tmp_obj = tmp_rel.getObject();
//
//                tmp_pre_str = tmp_pre_str.replaceAll("\t", "");
//                tmp_pre_str = tmp_pre_str.replaceAll("\n", "");
//                tmp_pre_str = tmp_pre_str.replaceAll(" ", "");
//
//                if(tmp_pre_str == "") continue;
//                //filter with schema.org
//
//                //use score instead of rank
//
//                if(_predicates.containsKey(tmp_pre_str)){
//                    Predicate tmp_pre = _predicates.get(tmp_pre_str);
//                    tmp_pre.add_object(tmp_obj, i);
//                }
//                else{
//                    _predicates.put(tmp_pre_str, new Predicate(tmp_pre_str, tmp_obj,i));
//                }
//            }
//        }
//
//       // print_predicates(10);
//    }
//    public static void print_predicates(int k){
//        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());
//
//        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
//            queue.add(entry.getValue());
//        }
//
//        int cnt = 0;
//        while(queue.size() != 0 ){
//            String pre = queue.peek().get_predicate();
//            String obj = queue.peek().best_object();
//
//            System.out.println(pre + " \t" + obj + "\t" + queue.peek().get_score());
//
//            queue.remove();
//            cnt++;
//            if(cnt == k){
//                queue.clear();
//                break;
//            }
//        }
//    }
//    public static void print_to_file(String filename, int k) throws IOException {
//        BufferedWriter output = new BufferedWriter(new FileWriter(new File(filename), true));
//
//        PriorityQueue<Predicate> queue = new PriorityQueue<Predicate>(new PredicateComparator());
//
//        for(Map.Entry<String,Predicate> entry: _predicates.entrySet()){
//            queue.add(entry.getValue());
//        }
//
//        output.write("<result>\n");
//        output.write("\t<query> " + _query + " </query>\n");
//        output.write("\t<abstract>\n");
//
//        int cnt = 0;
//        while(queue.size() != 0 ){
//            String pre = queue.peek().get_predicate();
//            String obj = queue.peek().best_object();
//
//            //System.out.println(pre + " \t" + obj + "\t" + queue.peek().get_score());
//            output.write("\t\t<relation>\n");
//            output.write("\t\t\t<predicate> " + pre + " </predicate>\n");
//            output.write("\t\t\t<object> " + obj + " </object>\n");
//            output.write("\t\t\t<score> " + queue.peek().get_score() + " </score>\n");
//            output.write("\t\t</relation>\n");
//            queue.remove();
//            cnt++;
//            if(cnt == k){
//                queue.clear();
//                break;
//            }
//        }
//        output.write("\t</abstract>\n");
//        output.write("</result>\n");
//
//        output.close();
//    }
//}
