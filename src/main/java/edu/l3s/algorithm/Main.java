//package edu.l3s.algorithm;
//
//import edu.l3s.dataStructure.DBpediaResource;
//import net.sf.javaml.core.Dataset;
//
//import java.io.*;
//import java.nio.Buffer;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//public class Main {
//
//    public static void main(String[] args) throws IOException, InterruptedException {
//	// write your code here
//        if(args.length !=3 ){
//            System.out.println("Pleas input the Type, input_query path and:\n\t c - for clustering approach\n\t h - for heuristic approach");
//            System.exit(1);
//        }
//        if(!(args[2].equals("c")||args[2].equals("h"))){
//            System.out.println("Pleas input c|h for the approach identity.");
//            System.exit(1);
//        }
//
//        long startTime = System.currentTimeMillis();
//
//        String gtfile_path = "Results_"+args[0]+"1.json";
////        String gtfile_path = "Results_"+args[0]+".json";
//
//        boolean[] f = new boolean[5];
//        f[0]=f[1]=f[2]=true;  f[3]=f[4] = false;
//
////        run_feature_evalluation();
//       // run_baseline_evaluation();
////        factSelection(args[0],args[1], 1, 10, gtfile_path, 50);
////        print_gt_data(args[0], args[1],11, 30, 50 );
//
//        run_bm25_evaluation(args[0], args[1], 30, 50, gtfile_path);
////        run_cbfs_evaluation(args[0], args[1], 30, 50, gtfile_path);
////        run_classification_evaluation(args[0], args[1], 4, gtfile_path, 20);
////        run_candc_evaluation(args[0], args[1], 1, 30, gtfile_path, 20, f);
////        evaluateApp(args[0] + ".app.out", args[0] + ".precision", args[0], 1, 30);
////        print_training_data(args[0], args[1], 4, gtfile_path, 20);
////        FactClassifier cf = new FactClassifier(args[0] +"_training.data", "tmp");
//
////        NameChecker.print_with_label(args[0], args[1], 1, 10, 50);
////        NameChecker.print_class(args[0], args[1], 11, 30, 50, "correct");
//
//        long endTime   = System.currentTimeMillis();
//        long totalTime = endTime - startTime;
//        System.out.println(totalTime / 1000 + "s");
//
//        System.exit(2);
//
//        BufferedReader br = new BufferedReader(new FileReader(args[1]));
//
//        String precision_output= args[0]+".precision";
//        String line;
//
//        while((line=br.readLine()) != null){
//            //System.out.println(line);
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//            //groundtruth
//            if(Integer.valueOf(tmps[0])>30) break;
//
//            String input="filter_result_new/"+args[0]+"/"+args[0]+"_"+tmps[0]+".filtered";
//                        String output1 = "abs_cluster/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs";
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, args[0], 100000);
////            if(args[2].equals("c")) {
////                output1 = "abs_cluster/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs";
////            }
////            else if(args[2].equals("h")) {
////                output1 = "abs_heuristic/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs";
////            }
//
//
//
////            abs.print_with_feature("feature_out/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs", 1000, "c", tmps[0]);
////            abs.print_with_feature_precision("feature_out/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".fea", 1000, "c", tmps[0], precision_output);
//
////            abs.printTrainTestData(args[0] +"_training.data", 10000, "c", tmps[0]);
//
////            String output2 = "abs_heuristic/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs";
////            abs.print_to_tsv(output2, 1000, "h", tmps[0],  precision_output);
//
//        }
//
//
//        br.close();
//    }
//
//    private static void run_feature_evalluation( )throws IOException, InterruptedException{
//        String type; String query_file; Integer start_index,end_index,topk; String gt;
//////
////        type = "Book";
////        query_file = "Book.query";
////        start_index = 1; end_index = 30; topk = 50;
////        gt = "Results_Book30.tsv";
////
////        run_all_feature_combination(type, query_file, start_index, end_index, gt, topk);
////
////        type = "Product";
////        query_file = "Product.query";
////        start_index = 1; end_index = 30; topk = 50;
////        gt = "Results_Product.tsv";
////
////        run_all_feature_combination(type, query_file, start_index, end_index, gt, topk);
////
//        type = "Movie";
//        query_file = "Movie.query";
//        start_index = 1;
//        end_index = 30;
//        gt = "Results_Movie30.tsv";
//        topk=50;
//
//        run_all_feature_combination(type, query_file, start_index, end_index, gt, topk);
////
////        type = "Movie";
////        query_file = "Movie.query.filtered";
////        start_index = 1;
////        end_index = 7000;
////        gt = "Results_Movie1.json";
////        topk=50;
//////
////        run_all_feature_combination(type, query_file, start_index, end_index, gt, topk);
//
//
//    }
//
//    private static void run_all_feature_combination(String type, String queryfile, Integer start_index, Integer end_index, String gt,
//                                                    Integer topk) throws IOException, InterruptedException {
//
//        String precision_output= type+".precision";
//        BufferedWriter bw = new BufferedWriter(new FileWriter(precision_output, true));
//        bw.write(type+"\t"+ start_index +"\t"+end_index+"\t"+topk+"\n");
//        bw.close();
//        boolean[] f= new boolean[4];
//
//        f[0]= true; f[1]=f[2]=f[3]=false;
//        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//       // System.exit(1);
////        f[1]= true; f[0]=f[2]=f[3]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[2]= true; f[0]=f[1]=f[3]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[3]= true; f[0]=f[1]=f[2]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//
//        f[0]= f[1] = true; f[2]=f[3]=false;
//        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//        f[0]= f[2] = true; f[1]=f[3]=false;
//        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[0]= f[3] = true; f[1]=f[2]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[1]= f[2] = true; f[0]=f[3]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[1]= f[3] = true; f[0]=f[2]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[2]= f[3] = true; f[1]=f[0]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//
//        f[0]= f[1] =f[2] = true; f[3]=false;
//        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[0]= f[1] =f[3] = true; f[2]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
////        f[0]= f[2] =f[3] = true; f[1]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//////        f[1]= f[2] =f[3] = true; f[0]=false;
////        run_candc_evaluation(type, queryfile, start_index,end_index, gt, topk, f);
//
////        f[0]= f[1] =f[2] = f[3] = true;
////        run_candc_evaluation(type, queryfile, start_index, end_index, gt, topk, f);
////        System.exit(8);
//    }
//
//    private static void factSelection(String type, String queryfile, Integer start_index, Integer end_index, String gt, Integer topk) throws IOException {
//        Integer folds = 10;
//
//        Integer interval = (end_index - start_index + 1) / folds ;
//        File f = new File(type + ".app.out");
//        f.delete();
//        BufferedWriter bw = new BufferedWriter(new FileWriter(f));
//        bw.write("q_id\tquery\tpredicate\tobject\tisCenter\tlabel\tclass\n");
//
//        bw.close();
//
//        for(Integer i = 1; i <= folds ; i++){
//            Integer startq = start_index + (i-1)*interval;
//            Integer endq;
//            if(i<folds)  endq = start_index + i * interval;
//            else {
//                endq = end_index+1;
//            }
//
//            FactClassifier fc = new FactClassifier(type+"_training.data", startq, endq, type + ".app.out");
//        }
//
//        evaluateApp(type + ".app.out", type + ".precision", type, start_index, end_index);
//    }
//
//    private static void evaluateApp(String in, String out, String type, Integer s, Integer e) throws IOException{
//        BufferedReader br = new BufferedReader(new FileReader(in));
//        BufferedWriter bw = new BufferedWriter(new FileWriter(out, true));
//
//        Integer tp, fp, tn, fn ;
//        tp = fp = tn = fn = 0;
//
//        Integer tpo = 0, fpo = 0;
//
//        String line = br.readLine();
//
//        while( (line = br.readLine()) != null ){
//            String[] arr = line.split("\t");
//
////            System.out.println(line);
//            Integer isCenter = Integer.valueOf(arr[4]);
//            Integer label = Integer.valueOf(arr[5]);
//            Integer appClass = Integer.valueOf(arr[6]);
//
//            if(appClass == 1){
//                if(label == 1){
//                    tp+=1;
//                    if(isCenter == 1) tpo += 1;
//                }
//                else{
//                    fp += 1;
//                    if(isCenter == 1) fpo += 1;
//                }
//            }
//            else{
//                if(label == 1){
//                    fn += 1;
//                }
//                else{
//                    tn += 1;
//                }
//            }
//        }
//
//        bw.write("Accuracy\tprecision\trecall\tF1\toverall_precision\n");
//
//        double acc = (tp+tn)/(double)(tp+tn+fp+fn);
//        double precision = (tp)/(double)(tp+fp);
//        double recall = (tp)/(double)(tp+fn);
//        double f1 = 0.0;
//        if(precision + recall != 0) f1 = 2*precision*recall/(precision + recall);
//        double po = tpo/(double)(tpo+fpo);
//
//        bw.write(acc+"\t"+precision+"\t"+recall+"\t"+f1+"\t"+po +"\t"+ type +"\t"+s+"\t"+e+"\tiswcApp\n");
//
//        br.close();
//        bw.close();
//    }
//    private static void finsl_summary(String in, String out, String type, Integer s, Integer e) throws IOException{
//        BufferedReader br = new BufferedReader(new FileReader(in));
//        BufferedWriter bw = new BufferedWriter(new FileWriter(out, true));
//
//        String line = br.readLine();
//
//        while( (line = br.readLine()) != null ){
//            String[] arr = line.split("\t");
//
//        }
//
//        bw.write("Accuracy\tprecision\trecall\tF1\toverall_precision\n");
//
//        br.close();
//        bw.close();
//    }
//    private static void run_classification_evaluation(String type, String queryfile, Integer q_num,
//                                                      String gt, Integer topk, boolean[] feature_index) throws IOException, InterruptedException {
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        //groundtruth
//
//
//        String precision_output= type+".precision";
//        String line;
//
//        File f = new File(type +"_training.data");
//        f.delete();
//
//        while((line=br.readLine()) != null){
//            //System.out.println(line);
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//            //groundtruth
//            if(Integer.valueOf(tmps[0])>q_num) break;
//
//            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, type, topk);
////            abs.print_with_feature("feature_out/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".abs", 1000, "c", tmps[0]);
////            abs.print_with_feature_precision("feature_out/" + args[0] + "/" + args[0] + "_" + tmps[0] + ".fea", 1000, "c", tmps[0], precision_output);
//
//            abs.printTrainTestDataCentroid(type + "_training.data", 10000, "c", tmps[0], gt,feature_index);
//        }
//
//        run_cross_validation(type, precision_output);
//
//        br.close();
//    }
//    private static void run_candc_evaluation(String type, String queryfile, Integer start_index, Integer end_index, String gt, Integer topk, boolean[] feature_index) throws IOException, InterruptedException {
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        String precision_output= type+".precision";
//        String line;
//
//        File f = new File(type +"_training.data");
//        f.delete();
//
//        while((line=br.readLine()) != null){
//            //System.out.println(line);
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//
//            if(Integer.valueOf(tmps[0])< start_index) continue;
//            if(Integer.valueOf(tmps[0])>end_index) break;
//
//            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
////            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, type, topk);
//
//            abs.printTrainTestDataAll(type + "_training.data", 10000, "c", tmps[0], gt, feature_index);
//        }
//
//        run_cross_validation(type, precision_output);
//
////        print_final_summary_precision(type);
//
//        br.close();
//    }
//    private static void run_bm25_evaluation(String type, String queryfile, Integer q_num, Integer at_k, String gtfile) throws IOException, InterruptedException {
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        //groundtruth
//
//
//        String precision_output= type+".precision";
//        //TODO add a line here for the column name
//        String line;
//
//        while((line=br.readLine()) != null){
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
////            System.out.println(tmps[0]+"\t"+tmps[1]);
//
//            //groundtruth
//            if(Integer.valueOf(tmps[0])>q_num) break;
//
//            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, type, at_k);
//            String output_bm25 = "tmp_bm25out";
//            abs.print_to_tsv(output_bm25, 1000, "bm25", tmps[0],  gtfile, precision_output);
//        }
//
//
//        br.close();
//
//    }
//    private static void run_cbfs_evaluation(String type, String queryfile, Integer q_num, Integer topk, String gtfile) throws IOException, InterruptedException {
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        //groundtruth
//
//
//        String precision_output= type+".precision";
//        String line;
//
//        while((line=br.readLine()) != null){
//            //System.out.println(line);
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//            //groundtruth
//            if(Integer.valueOf(tmps[0])>q_num) break;
//
//            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//            String output1 = "abs_cluster/" + type + "/" + type + "_" + tmps[0] + ".abs";
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, type, topk);
//            abs.print_to_tsv(output1, 10000, "c", tmps[0], gtfile, precision_output);
//        }
//        br.close();
//
//    }
//    private static void  run_cross_validation(String type,String preOut) throws IOException {
//        FactClassifier cf = new FactClassifier(type +"_training.data", preOut, type);
//    }
//    private static void  print_training_data(String type, String queryfile, Integer start_index, Integer end_index, String gt, Integer topk, boolean[] feature_index) throws IOException, InterruptedException {
//        File trainf = new File(type +"_training.data");
//        trainf.delete();
//
//        String line;
//
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        while((line=br.readLine()) != null){
//            //System.out.println(line);
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//
//            if(Integer.valueOf(tmps[0])< start_index) continue;
//            if(Integer.valueOf(tmps[0])>end_index) break;
//
//            String input="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//
//            Abstractor abs = new Abstractor(tmps[0], tmps[1], input, type, topk);
//
//            abs.printTrainTestDataAll(type + "_training.data", 10000, "c", tmps[0], gt, feature_index);
//        }
//    }
//    private static void  print_gt_data(String type, String queryfile, Integer start_index, Integer end_index, Integer topk) throws IOException, InterruptedException {
//        String out3 = "groundtruth/"+type+".tsv";
//        File f = new File(out3);
//        f.delete();
//        BufferedWriter bwgt = new BufferedWriter(new FileWriter(f));
//        bwgt.write("query_id\tquery_term\twikipediaPage\trank\tpredicate\tobject\tinDBpedia\n");
//        bwgt.close();
//        BufferedReader br = new BufferedReader(new FileReader(queryfile));
//
//        String line;
//
//        while((line=br.readLine()) != null){
//            String[] tmps = line.split("\t");
//            if(tmps.length<2)continue;
//
//            //groundtruth
//            if(Integer.valueOf(tmps[0])< start_index) continue;
//            if(Integer.valueOf(tmps[0])>end_index) break;
//
//            String input_plain="query_result/"+type+"/"+type+"_"+tmps[0]+"_bm25_query.csv";//Person_11_bm25_query.csv
//            String out4="filter_result_new/"+type+"/"+type+"_"+tmps[0]+".filtered";
//            Abstractor abs_gt = new Abstractor(tmps[0], tmps[1], input_plain, type, topk, out4, out3);
//        }
//        br.close();
//    }
//}
