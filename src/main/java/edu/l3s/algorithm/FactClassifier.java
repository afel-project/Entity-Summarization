package edu.l3s.algorithm;

import edu.l3s.dataStructure.FactFeature;
import jdk.nashorn.internal.ir.WhileNode;
import libsvm.*;
import net.sf.javaml.classification.AbstractClassifier;
import net.sf.javaml.classification.Classifier;
import net.sf.javaml.classification.KNearestNeighbors;
import net.sf.javaml.classification.bayes.AbstractBayesianClassifier;
import net.sf.javaml.classification.bayes.NaiveBayesClassifier;
import net.sf.javaml.classification.evaluation.CrossValidation;
import net.sf.javaml.classification.evaluation.CrossValidationRY;
import net.sf.javaml.classification.evaluation.PerformanceMeasure;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DenseInstance;
import net.sf.javaml.core.FactDenseInstance;
import net.sf.javaml.core.Instance;
import net.sf.javaml.distance.PearsonCorrelationCoefficient;
import net.sf.javaml.featureselection.subset.GreedyForwardSelection;
import net.sf.javaml.tools.data.FileHandler;

import java.io.*;
import java.util.Map;
import java.util.Objects;

/**
 * Created by ranyu on 3/7/16.
 */
public class FactClassifier {
    static Classifier _cf;

    public FactClassifier(String path, String preOut, String type) throws IOException {

        System.out.println("NOTICE:\tStart FactClassifier.");
        Dataset data = FileHandler.loadDataset(new File(path), 5, "\t");
        _cf = new LibSVM();
//        _cf = new NaiveBayesClassifier(true,true,false);
//        _cf = new KNearestNeighbors(3);

//  //Feature selection
//        GreedyForwardSelection ga = new GreedyForwardSelection(10, new PearsonCorrelationCoefficient());
///* Apply the algorithm to the data set */
//        ga.build(data);
///* Print out the attribute that has been selected */
//                System.out.println(ga.selectedAttributes());

     //cross validation
        CrossValidationRY cv = new CrossValidationRY(_cf);
        Map<Object, PerformanceMeasure> p = cv.crossValidation(data,10, type);
        printValidationRes(p, preOut);

//        _cf.buildClassifier(data);
    }
    public FactClassifier(String trainingdata, Integer startq, Integer endq, String out) throws IOException {
        Dataset data = FileHandler.loadDatasetInterval(new File(trainingdata), 5, "\t", startq, endq);

                _cf = new LibSVM();
//        _cf = new KNearestNeighbors(3);
        _cf.buildClassifier(data);

        classify(trainingdata, startq, endq, 5, out);
    }

    private static void classify(String trainingdata, Integer startq, Integer endq, Integer classIndex, String out) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(trainingdata));

        BufferedWriter bw = new BufferedWriter(new FileWriter(out, true));

        String line;


        while((line = br.readLine()) != null){
            String[] arr = line.split("\t");

            String out_text = "";

            if(Integer.valueOf(arr[0]) >= startq && Integer.valueOf(arr[0]) < endq) {
                double[] values = new double[arr.length - classIndex - 1];

                for (int i = 0; i < arr.length; i++) {
                    if(i>classIndex){
                        double val;
                        try {
                            val = Double.parseDouble(arr[i]);
                        } catch (NumberFormatException e) {
                            val = Double.NaN;
                        }
                        values[i - classIndex - 1] = val;
                    }
                    else{
                        out_text = out_text + arr[i] + "\t";
                    }
                }
                Instance ins = new DenseInstance(values);

                String insClass = _cf.classify(ins).toString();

                bw.write(out_text + insClass + "\n");
            }
        }

        br.close();
        bw.close();
    }

    private void printValidationRes(Map<Object, PerformanceMeasure> p, String preOut) throws IOException {
        System.out.println("NOTICE:\tStart cross validation.");

        BufferedWriter bw = new BufferedWriter(new FileWriter(preOut, true));
      //  bw.write("Class" + "\tAcc" + "\tP" + "\tR" + "\tF\n");

        for( Map.Entry<Object, PerformanceMeasure> entry : p.entrySet()){
            String cls = entry.getKey().toString();
            PerformanceMeasure pm = entry.getValue();
            if(cls.equals("1")) {
                bw.write(cls + "\t" + pm.getAccuracy() + "\t" + pm.getPrecision() + "\t" + pm.getRecall() + "\t" + pm.getFMeasure() + "\tclassification\n");
            }
        }
        bw.close();
    }
    public Double testClassifier(String path) throws IOException {
        Double precision = 0.0;
        Double tp= 0.0,fp= 0.0,tn= 0.0, fn = 0.0;

        Dataset dataForClassification = FileHandler.loadDataset(new File(path), 0, "\t");
/* Counters for correct and wrong predictions. */
        int correct = 0, wrong = 0;
/* Classify all instances and check with the correct class values */
        for (Instance inst : dataForClassification) {
            Object predictedClassValue = _cf.classify(inst);
            Object realClassValue = inst.classValue();
            if (predictedClassValue.equals(realClassValue)) {
//                System.out.println("Correct");
                if(predictedClassValue.equals("0")){
                    tn += 1;
                }
                else{
                    tp += 1;
                }
                correct++;
            }
            else {
                if(predictedClassValue.equals("0")){
                    fn += 1;
                }
                else{
                    fp += 1;
                }
//                System.out.println("Incorrect");
                wrong++;
            }
        }

        if(correct+wrong < 0.9) return precision;
        System.out.println("Prcision of summarization = "+(tp)/(tp+fp));
//        System.out.println("Recall of summarization = "+(tp)/(tp+fn));
        return correct / (double)(correct + wrong);
    }
//    public int classify(Instance inst){
//
//    }
}
