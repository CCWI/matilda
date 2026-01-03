package edu.hm.ccwi.matilda.runner.corpus;

import edu.hm.ccwi.matilda.korpus.libsim.GACategoryTagManualTagsToTotalEnricher;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimToDatabaseEnricher;

import java.io.IOException;

/**
 * Runner for merging 2 gaCategoryTag-Collections, which might both contain important information.
 * Note for usage:
 *  1) Start mongodb-container
 *  2) Start libsim
 *  3) Run
 */
public class GACategoryTagManualTagsToTotalEnricherRunner {

    public static void main(String[] args) throws IOException, InterruptedException {
        GACategoryTagManualTagsToTotalEnricher gACategoryTagManualTagsToTotalEnricher =
                new GACategoryTagManualTagsToTotalEnricher();
        gACategoryTagManualTagsToTotalEnricher.enrich();
    }
}
