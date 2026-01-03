package edu.hm.ccwi.matilda.runner.corpus;

import edu.hm.ccwi.matilda.korpus.libsim.LibSimToDatabaseEnricher;

import java.io.IOException;

/**
 * Runner for classifying libraries one-time and persisting these
 * reclassified libraries into a new collection for production use.
 * Note for usage:
 *  1) Start libsim-ki-model in container
 *  2) Prepare target-database (remove if necessary)
 *  3) Run
 */
public class LibSimClassificationRunner {

    public static void main(String[] args) throws IOException, InterruptedException {
        LibSimToDatabaseEnricher libSimToDatabaseEnricher =
                new LibSimToDatabaseEnricher("gACategoryTagAddManualTags", "gACategoryTagTotal", 0.20);
        libSimToDatabaseEnricher.enrich();
    }
}
