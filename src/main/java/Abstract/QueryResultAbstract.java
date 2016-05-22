package Abstract;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ranyu on 7/15/15.
 */

public class QueryResultAbstract {
    private final String query;
    public List<AbstractPredict> predicts = new ArrayList<AbstractPredict>();

    public QueryResultAbstract(String query) {
        this.query = query;
    }

    public int findPredict(String pre){
        for(int i = 0; i < predicts.size(); i++){
            if(predicts.get(i).predict == pre){
                return i;
            }
        }
        return -1;
    }


    public class AbstractPredict {
        private final String predict;
        public List<AbstractObject> objects;
        public double score;

        public class AbstractObject{
            public String object;
            public double score;

            public AbstractObject(String obj, double s){
                object = obj; score = s;
            }

            public double getScore(){return score;}
            public void updateScore(double new_score){score = new_score;}
        }



        public AbstractPredict(String predict, List<AbstractObject> obj, double s) {
            this.predict = predict;
            this.objects = obj;
            this.score = s;
        }

        public int findObject(String obj){
            for(int i =0; i < objects.size(); i++){
                if(objects.get(i).object == obj){
                    return i;
                }
            }
            return -1;
        }

        public double getScore(){return score;}

        public void updateScore(double new_score){
            score = new_score;
        }

        public void add_score(int pre_pos, int obj_pos, double new_score){

        }
        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
        public StringBuilder toString(StringBuilder sb) {
            return sb;
        }
    }


}
