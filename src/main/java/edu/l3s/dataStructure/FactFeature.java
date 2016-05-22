package edu.l3s.dataStructure;

import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.DefaultDataset;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ranyu on 2/23/16.
 */
public class FactFeature {
    //PLD level
    private double _pr; //PageRank of PLD
    //subject level
    private double _bm25; //BM25 score from the ER step
    private double _factNum; // number of facts of the subject
    private double _rank; //rank among the ER result
    //predicate level
    private double _pdf; // predicate document frequency, pre calculated
    private double _varience; // varience of the object frequency/ cluster size
    private double _average; //expected value of the object frequency/ cluster size
    //object level
    private double _clusterSize;
    private double _clusterNum;
    private double _frequency;
    private double _length;

    private Integer _isCenter;

    //Dbpedia similarity
    private double _dbsim;
    private Integer _name_exist;
/*
Parameters: @pn: predicate number
*/

    public FactFeature(){
        _clusterSize = 0.0;
        //pterm = new double[pn];
    }
    public FactFeature(double pr, double bm25, double rank, double factNum, double pdf, double len, double dbsim, Integer name_exist){
        _clusterSize = _varience = _average = 0.0;
        _pr = pr;
        _bm25 = bm25;
        _rank = rank;
        _frequency = 1.0;
        _factNum = factNum;
        _pdf = pdf;
        _length = len;
        _dbsim = dbsim;
        _isCenter = 0;
        _name_exist = name_exist;
        //pterm = new double[pn];
    }
    public void updateSubjectFeature(double pr){
        if(pr > _pr) _pr = pr;
        _frequency += 1;
    }
    public void updatePredicateFeature(double variance, double average){
        //_pdf = pdf;
        _varience = variance;
        _average = average;
    }
    public void updateClusterFeature(Integer isCenter, double clusterNum, double clsize, double average, double variance){
        if(_isCenter == 0) _isCenter = isCenter;
        if( clsize > _clusterSize ) _clusterSize = clsize;
        _average = average;
        _varience = variance;
        _clusterNum = clusterNum;
    }
    public Integer isCenter(){
        return _isCenter;
    }
    public String make_string(boolean[] f){
//        String fs = String.format("%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f\t%f",
//                +_pr,_bm25,_factNum,_rank,_pdf,_varience,_average,_clusterSize,_frequency,_length, _dbsim);
        String fs = "";
        Integer len= f.length;

        if(len > 0 && f[0]==true){
            fs = fs + String.format("\t%f\t%f\t%f",_rank,_bm25, _dbsim);
//            fs = fs + String.format("\t%f\t%f\t%f",_rank,_bm25, _dbsim);
        }
//        if(len > 1 && f[1]==true){
//            fs = fs + String.format("\t%f\t%f\t%f",
//                    +_varience,_average,_clusterSize);
//        }
        if(len > 1 && f[1]==true){
            fs = fs + String.format("\t%f\t%f\t%f\t%f",
                    +_varience,_clusterSize/_average,_average, _clusterNum);
        }
        if(len > 2 && f[2]==true){
//            fs = fs + String.format("\t%f\t%f\t%f\t%f\t%f",
//                    +_pr,_factNum,_pdf,_frequency,_length);
            fs = fs + String.format("\t%f\t%f\t%f\t%f",
                    +_pr,_factNum,_pdf,_frequency);
        }
//        if(len > 3 && f[3]==true){
//            fs = fs + String.format("\t%f", _dbsim);
//        }
//        if(len > 4 && f[4]==true){
//            fs = fs + String.format("\t%d", _name_exist);
//        }
        return fs;
    }
    public double[] make_vec(boolean [] f){
        double[] fv = new double[11];
        Integer len= f.length;

        if(len > 0 && f[0]==true){
            fv[0]=_rank;
            fv[1]=_bm25;
            fv[2]=_dbsim;
        }
        if(len > 1 && f[1]==true){
            fv[3]=_varience;
            fv[4]=_clusterSize/_average;
            fv[5]= _average;
            fv[6]=_clusterNum;
        }
        if(len > 2 && f[2]==true){
            fv[7]=_pr;
            fv[8]=_factNum;
            fv[9]=_pdf;
            fv[10]=_frequency;
        }
        return fv;
    }

//    public Dataset buildFeatureSet(){
//        Dataset dataset = new DefaultDataset();
//        return dataset;
//    }
}
