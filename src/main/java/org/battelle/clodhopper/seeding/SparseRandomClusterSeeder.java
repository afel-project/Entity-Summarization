package org.battelle.clodhopper.seeding;

/**
 * Created by besnik on 1/11/15.
 */
public interface SparseRandomClusterSeeder extends SparseClusterSeeder {

    long getRandomGeneratorSeed();

    void setRandomGeneratorSeed(long seed);
}
