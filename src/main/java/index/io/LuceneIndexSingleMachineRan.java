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

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by besnik on 27/08/2014.
 */
public class LuceneIndexSingleMachineRan {
    public static void main(String[] args) throws IOException, ParseException {
        String data_file = args[0];
        String out_dir = args[1];

        System.out.printf("Processing file: %s\n", data_file);
        GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(data_file));
        BufferedReader br = new BufferedReader(new InputStreamReader(gzip));

        int doc_counter = 0;
        Map<String, List<Map.Entry<String, String>>> resources = new HashMap<>();
        while (br.ready()) {
            String triple_line = br.readLine();
            try {
                Node[] triple = NxParser.parseNodes(triple_line);
                if(triple.length < 4 ) continue;
                String resource_uri = triple[0].toString();
               // String page_url = triple[3].toString();
                String field_content=triple[2].toString().replaceAll("\n", "").replaceAll("\r", "").replaceAll("\\s+", " ").trim();

                //check based on the analyzed literal length for the different datatype properties whether they should be included in the indexing part
                if (!resources.containsKey(resource_uri)) {
                    //after every 10 000 documents write them into the index
                    if (doc_counter != 0 && doc_counter % 10000 == 0) {
                        constructIndex(resources, out_dir);
                        resources.clear();
                        System.out.printf("There are %d documents written into the Lucene index.\n", doc_counter);
                    }
                    doc_counter++;
                }
                List<Map.Entry<String, String>> triples = resources.get(resource_uri);
                triples = triples == null ? new ArrayList<>() : triples;
                if(triples.isEmpty()){
                    AbstractMap.SimpleEntry<String, String> url_entry =  new AbstractMap.SimpleEntry<String, String>("page_url", triple[3].toString());
                    triples.add(url_entry);
                }

                //add the triple to the resource
                AbstractMap.SimpleEntry<String, String> triple_entry = new AbstractMap.SimpleEntry<String, String>(triple[1].toString(), field_content);
                triples.add(triple_entry);

                resources.put(resource_uri, triples);

            } catch (Exception e) {
                System.out.println("Exception at: " + triple_line + "\t" + e.getMessage());
            }
        }
        if (!resources.isEmpty()){
            constructIndex(resources, out_dir);
            resources.clear();
            System.out.printf("There are %d documents written into the Lucene index.\n", doc_counter);
        }
    }

    /**
     * Generate the Lucene Index.
     *
     * @param resources
     */
    private static void constructIndex(Map<String, List<Map.Entry<String, String>>> resources, String out_dir) throws IOException {
        Analyzer analyzer = new StandardAnalyzer();

        IndexWriterConfig iwc = new IndexWriterConfig(Version.LATEST, analyzer);
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
//        if (resource_uri.contains("\\")) {
//            resource_uri = resource_uri.substring(resource_uri.lastIndexOf("\\"));
//        }

        Field resource_uri_field = new TextField("resource_uri", resource_uri, Field.Store.YES);
        doc.add(resource_uri_field);

        for (Map.Entry<String, String> triple : triples) {
            Field field = new TextField(triple.getKey(), triple.getValue(), Field.Store.YES);
            doc.add(field);
        }

        return doc;
    }
}
