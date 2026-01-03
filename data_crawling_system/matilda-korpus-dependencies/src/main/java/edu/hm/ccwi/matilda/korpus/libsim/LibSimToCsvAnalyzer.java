package edu.hm.ccwi.matilda.korpus.libsim;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.korpus.util.CsvUtils;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.List;

public class LibSimToCsvAnalyzer {

    private static final String MONGODB_SOURCE_COLLECTION_NAME = "gACategoryTagAddManualTags";
    private static final String RESULT_CSV_NAME = "libsim-classified-gACategoryTagAddManualTag.csv";

    public static void main(String[] args) throws IOException, InterruptedException {
        LibSimClient lsClient = new LibSimClient();
        List<GACategoryTag> gACategoryTagAddManualTags =
                new MongoUtils<>(GACategoryTag.class, MONGODB_SOURCE_COLLECTION_NAME).retrieveCollectionFromDB();

        int counter = 0;
        for (GACategoryTag gACategoryTagAddManualTag : gACategoryTagAddManualTags) {
            if (gACategoryTagAddManualTag != null && StringUtils.isEmpty(gACategoryTagAddManualTag.getCategory())
                    && CollectionUtils.isNotEmpty(gACategoryTagAddManualTag.getTags())) {
                counter++;
                try {
                    LibSimResult libSimResult = lsClient.requestKi(gACategoryTagAddManualTag);
                    LibSimAnalyzedLibrary libSimAnalyzedLibrary = new LibSimAnalyzedLibrary(gACategoryTagAddManualTag.getId(),
                            gACategoryTagAddManualTag.getGroup(), gACategoryTagAddManualTag.getArtifact(), libSimResult);
                    CsvUtils.appendToCsv(RESULT_CSV_NAME, libSimAnalyzedLibrary.toString(), false);
                    if(counter % 10 == 0) {
                        System.out.println("LibSim-Batch-Progress: [" + counter++ + "/" + gACategoryTagAddManualTags.size() + "]");
                    }
                } catch (LibSimException e) {
                    System.err.println("Request not processed: [" + counter++ + "/" + gACategoryTagAddManualTags.size() + "] -> " + e.getMessage());
                }
            }
        }
    }
}
