package data_structures;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by besnik on 12/12/14.
 */
public class Entity {
    public List<String[]> triples;
    public String entity_uri;
    public int cluster_id;

    public Entity() {
        triples = new ArrayList<>();
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(entity_uri).append("\t<table>");
        for (String[] triple : triples) {
            sb.append("<tr><td>").append(triple[0]).append("</td><td>").append(triple[1]).append("</td></tr>");
        }
        sb.append("</table>\t").append(cluster_id).append("\n");
        return sb.toString();
    }
}
