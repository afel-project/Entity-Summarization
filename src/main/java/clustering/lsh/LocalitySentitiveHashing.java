package clustering.lsh;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import utils_package.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements a simple Locality Sensitive Hashing (LSH) strategy,
 * relying on min-hash for measuring similarity between instances
 */
public class LocalitySentitiveHashing {
    // The hash functions for generating the min-hash signatures
    private MinHash.HashFunction function[];

    // The bands from the LSH index
    public Map<Integer, Set<Integer>> index[];

    // The bands from the LSH index (a temporary index that is periodically flushed to non-volatile storage)
    private Map<Integer, Set<Integer>> indexTemp[];

    // The min-hash representions for each example in the database
    public Map<Integer, int[]> representation;

    // The class assigned to each example in the database
    public Map<Integer, String> value;

    // The validity score for each training example
    public Map<Integer, Double> validity;

    //store the number of items that are hashed
    private AtomicInteger entity_no = new AtomicInteger();

    //the output directory which is used to write the hashed entities
    public String out_file;

    // A constructor that initializes the LSH index with a given number of hash functions for the min-hash signatures, and with a given number of bands
    public LocalitySentitiveHashing(File file, int numFunctions, int numBands) {
        if (numFunctions % numBands != 0)
            throw new Error("Number of hash functions is not divisible by the number of bands.");
        try {
            DB db = DBMaker.newFileDB(file).transactionDisable().asyncWriteFlushDelay(100).closeOnJvmShutdown().make();

            this.function = MinHash.createHashFunctions(MinHash.HashType.POLYNOMIAL, numFunctions);
            this.representation = db.getTreeMap("representation");
            this.value = db.getTreeMap("value");
            this.validity = db.getTreeMap("kvalue");
            this.index = (Map[]) Array.newInstance(db.getTreeMap("index").getClass(), numBands);
            this.indexTemp = (Map[]) Array.newInstance(new HashMap<Integer, Set<Integer>>().getClass(), numBands);

            for (int i = 0; i < numBands; i++) {
                this.indexTemp[i] = new HashMap<>();
                this.index[i] = db.getTreeMap("index-" + i);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // A constructor that initializes the LSH index with a given number of hash functions for the min-hash signatures, and with a given number of bands
    public LocalitySentitiveHashing(int numFunctions, int numBands) {
        if (numFunctions % numBands != 0)
            throw new Error("Number of hash functions is not divisible by the number of bands.");
        try {
            this.function = MinHash.createHashFunctions(MinHash.HashType.POLYNOMIAL, numFunctions);
            this.indexTemp = (Map[]) Array.newInstance(new HashMap<Integer, Set<Integer>>().getClass(), numBands);

            for (int i = 0; i < numBands; i++) {
                this.indexTemp[i] = new HashMap<>();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Adds a new example to the index
    public void index(Integer id, String[] data) {
        entity_no.incrementAndGet();
        int size = function.length / indexTemp.length;
        int[] minhash = MinHash.minHashFromSet(data, function);
        for (int i = 0; i < indexTemp.length; i++) {
            try {
                int code = function[0].hash(integersToBytes(minhash, i * size, size));
                Set<Integer> auxSet = indexTemp[i].get(code);
                if (auxSet == null) {
                    auxSet = new HashSet<>();
                }
                auxSet.add(id);
                indexTemp[i].put(code, auxSet);
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
        }

        if (entity_no.get() != 0 && entity_no.get() % 1000 == 0) {
            writeLSHEntities();
            for (int i = 0; i < indexTemp.length; i++) {
                indexTemp[i].clear();
            }
        }
        //representation.put(id, minhash);
        //value.put(id, result);
    }

    public void writeLSHEntities() {
        //write the actual entity mappings.
        for (int i = 0; i < indexTemp.length; i++) {
            Map<Integer, Set<Integer>> index = indexTemp[i];
            StringBuffer sb = new StringBuffer();
            for (int code : index.keySet()) {
                for (int entity_id : index.get(code)) {
                    sb.append(i).append("\t").append(code).append("\t").append(entity_id).append("\n");
                }
            }
            FileUtils.saveText(sb.toString(), out_file, true, false);
        }
    }

    public void commitChanges() {
        for (int i = 0; i < indexTemp.length; i++)
            try {
                for (Integer code : indexTemp[i].keySet()) {
                    Set<Integer> auxSet = new HashSet<>(indexTemp[i].get(code));
                    if (index[i].containsKey(code)) {
                        auxSet.addAll(index[i].get(code));
                    }
                    index[i].put(code, Collections.unmodifiableSet(auxSet));
                }
                indexTemp[i].clear();
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
            }
    }

    // Computes the validity score for each example in the database, through a leave-one-out methodology
    public void computeValidity(int k) {
        for (Integer example : value.keySet()) {
            String cl = value.get(example);
            int minhash[] = representation.get(example);
            TopN<String> result = new TopN<>(k);
            int size = function.length / index.length;
            for (int i = 0; i < index.length; i++) {
                int code = function[0].hash(integersToBytes(minhash, i * size, size));
                Set<Integer> auxSet = index[i].get(code);
                if (auxSet != null) {
                    for (Integer candidate : auxSet)
                        if (candidate != example) {
                            String val = value.get(candidate);
                            int rep[] = representation.get(candidate);
                            result.add(val, MinHash.jaccardSimilarity(minhash, rep));
                        }
                }
            }
            double cnt = 0;
            SortedSet<Pair<String, Double>> mySet = result.get();
            for (Pair<String, Double> newCl : mySet) {
                if (cl.equals(newCl.getFirst())) {
                    cnt++;
                }
            }
            if (mySet.size() != 0) {
                cnt = cnt / ((double) (mySet.size()));
            }
            validity.put(example, cnt);
        }
    }

    // Returns the top-k most similar examples in the database
    public TopN<String> queryNearest(String[] data, int k) {
        final int size = function.length / index.length;
        final int[] minhash = MinHash.minHashFromSet(data, function);
        final TopN<String> result = new TopN<>(k);
        for (int i = 0; i < index.length; i++) {
            int code = function[0].hash(integersToBytes(minhash, i * size, size));
            Set<Integer> auxSet = index[i].get(code);
            if (auxSet != null) for (Integer candidate : auxSet) {
                String valueS = value.get(candidate);
                int rep[] = representation.get(candidate);
                double score = MinHash.jaccardSimilarity(minhash, rep);
                if (validity.containsKey(candidate)) {
                    score = score * validity.get(candidate);
                    result.add(valueS, score);
                }
            }
        }
        return result;
    }

    // Utility method for converting between an a subset of the values in an array of integers, and an array of bytes
    private byte[] integersToBytes(int[] values, int position, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for (int i = position; i < position + length && i < values.length; ++i)
            try {
                dos.writeInt(values[i]);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        return baos.toByteArray();
    }
}