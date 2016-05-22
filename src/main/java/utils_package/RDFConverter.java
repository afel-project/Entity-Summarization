package utils_package;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * Created by besnik on 11/27/14.
 */
public class RDFConverter {
    public static void main(String[] args) throws IOException {
        String data_dir = "", out_dir = "", operation = "";
        boolean compressed = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-data_dir")) {
                data_dir = args[++i];
            } else if (args[i].equals("-out_dir")) {
                out_dir = args[++i];
            } else if (args[i].equals("-operation")) {
                operation = args[++i];
            } else if (args[i].equals("-compressed")) {
                compressed = Boolean.valueOf(args[++i]);
            }
        }

        //convert to N3 the NQuad files.
        Set<String> files = new HashSet<>();
        FileUtils.getFilesList(data_dir, files);

        for (String file : files) {
            String out_file = out_dir + file.substring(file.lastIndexOf("/") + 1, file.lastIndexOf("."));
            try {
                BufferedReader reader = null;
                if (compressed) {
                    reader = FileUtils.getCompressedFileReader(file);
                } else {
                    reader = FileUtils.getFileReader(file);
                }
                if (operation.equals("n3")) {
                    StringBuffer sb = new StringBuffer();
                    while (reader.ready()) {
                        try {
                            Node[] triple = NxParser.parseNodes(reader.readLine());
                            sb.append(triple[0].toN3()).append(" ").append(triple[1].toN3()).append(" ").append(triple[2].toN3()).append(" .\n");

                            if (sb.length() % 1000 == 0 && sb.length() != 0) {
                                FileUtils.saveText(sb.toString(), out_file + ".n3.gz", true, true);
                                sb.delete(0, sb.length());
                            }
                        } catch (Exception e) {
                            System.out.printf("Skipping triple with error: %s.\n", e.getMessage());
                        }
                    }
                    FileUtils.saveText(sb.toString(), out_file + ".n3.gz", true, true);
                } else if (operation.equals("ttl")) {
                    // creates a new, empty in-memory model
                    Model model = FileManager.get().loadModel(file, "N3");
                    System.out.printf("[%s] Model has %d triples.\n", out_file, model.size());

                    RDFDataMgr.write(new GZIPOutputStream(new FileOutputStream(new File(out_file + ".ttl.gz"))), model, Lang.TTL);
                }
            } catch (Exception e) {
                System.out.printf("Skipping file: %s with error: %s.\n", file, e.getMessage());
            }
        }
    }
}
