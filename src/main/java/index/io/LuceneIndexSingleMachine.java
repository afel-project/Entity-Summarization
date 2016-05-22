package index.io;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.parser.ParseException;
import utils_package.FileUtils;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by besnik on 27/08/2014.
 */
public class LuceneIndexSingleMachine {
    public static void main(String[] args) throws IOException, ParseException {
        String data_dir = args[0];
        String out_dir = args[1];
        Set<String> allowed_properties = FileUtils.readIntoSet(args[2], "\n", false);

        Set<String> btc_files = new HashSet<String>();
        FileUtils.getFilesList(data_dir, btc_files);

        System.out.printf("Loaded files: %d\n", btc_files.size());

        for (String btc_file : btc_files) {
            System.out.printf("Processing file: %s\n", btc_file);
            GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(btc_file));
            BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

            int doc_counter = 0;
            Map<String, List<Map.Entry<String, String>>> resources = new HashMap<>();
            while (br.ready()) {
                String triple_line = br.readLine();
                try {
                    Node[] triple = NxParser.parseNodes(triple_line);
                    String resource_uri = triple[0].toString();

                    //check if the object is an Object URI, then do not index.
                    if (triple.length < 3 || triple[2].toString().startsWith("http") || triple[2].toString().startsWith("www")) {
                        continue;
                    }

                    if (!allowed_properties.contains(triple[1].toString())) {
                        continue;
                    }

                    //check based on the analyzed literal length for the different datatype properties whether they should be included in the indexing part
                    if (!resources.containsKey(resource_uri)) {
                        doc_counter++;
                    }

                    List<Map.Entry<String, String>> triples = resources.get(resource_uri);
                    triples = triples == null ? new ArrayList<Map.Entry<String, String>>() : triples;
                    resources.put(resource_uri, triples);

                    //add the triple to the resource
                    AbstractMap.SimpleEntry<String, String> triple_entry = new AbstractMap.SimpleEntry<String, String>(triple[1].toString(), triple[2].toString());
                    triples.add(triple_entry);

                    //after every 10 000 documents write them into the index
                    if (doc_counter != 0 && doc_counter % 10000 == 0) {
                        constructIndex(resources, out_dir);
                        resources.clear();

                        System.out.printf("There are %d documents written into the Lucene index.\n", doc_counter);
                    }
                } catch (Exception e) {
                    System.out.println("Exception at: " + triple_line + "\t" + e.getMessage());
                }
            }

        }
    }

    /**
     * Generate the Lucene Index.
     *
     * @param resources
     */
    private static void constructIndex(Map<String, List<Map.Entry<String, String>>> resources, String out_dir) throws IOException {
        Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);

        IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_CURRENT, analyzer);
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        IndexWriter writer = new IndexWriter(FSDirectory.open(new File(out_dir)), iwc);

        for (String resource_uri : resources.keySet()) {
            Document document = buildDocument(resource_uri, resources.get(resource_uri));
            writer.addDocument(document);
        }

        writer.close();
    }

    /**
     * Given a set of triples builds a lucene index.
     *
     * @param resource_uri
     * @param triples
     * @return
     * @throws IOException
     */
    private static Document buildDocument(String resource_uri, List<Map.Entry<String, String>> triples) throws IOException {
        Document doc = new Document();
        if (resource_uri.contains("\\")) {
            resource_uri = resource_uri.substring(resource_uri.lastIndexOf("\\"));
        }

        Field resource_uri_field = new TextField("resource_uri", resource_uri, Field.Store.YES);
        doc.add(resource_uri_field);

        //label
        StringBuffer sb_label = new StringBuffer();
        StringBuffer sb_body = new StringBuffer();
        for (Map.Entry<String, String> triple : triples) {
            if (triple.getKey().toLowerCase().contains("label") || triple.getKey().toLowerCase().contains("name")) {
                sb_label.append(triple.getValue()).append("\n");
            } else {
                sb_body.append(triple.getValue()).append("\n");
            }
        }
        Field field_title = new TextField("title", sb_label.toString(), Field.Store.YES);
        Field field = new TextField("body", sb_body.toString(), Field.Store.YES);
        doc.add(field);
        doc.add(field_title);
        return doc;
    }
}
