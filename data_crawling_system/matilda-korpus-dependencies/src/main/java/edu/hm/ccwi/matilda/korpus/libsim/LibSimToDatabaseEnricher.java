package edu.hm.ccwi.matilda.korpus.libsim;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;

@Deprecated(since = "01.2022 - Still Utilizes the old mongo-tables for library-classification")
public class LibSimToDatabaseEnricher {

    private MongoUtils<GACategoryTag> gaCategoryTagMongoUtils;
    private String mongodbSourceCollectionName;
    private String mongodbTargetCollectionName;
    private double threshold;

    public LibSimToDatabaseEnricher() {
        this.mongodbSourceCollectionName = "gACategoryTagAddManualTags";
        this.mongodbTargetCollectionName = "gACategoryTagTotal";
        this.threshold = 0.20;
        this.gaCategoryTagMongoUtils = new MongoUtils<>(mongodbTargetCollectionName);
    }

    public LibSimToDatabaseEnricher(String source, String target, double threshold) {
        this.mongodbSourceCollectionName = source;
        this.mongodbTargetCollectionName = target;
        this.threshold = threshold;
        this.gaCategoryTagMongoUtils = new MongoUtils<>(mongodbTargetCollectionName);
    }

    public void enrich() throws IOException, InterruptedException {
        LibSimClient lsClient = new LibSimClient();
        List<GACategoryTag> gACategoryTagAddManualTags =
                new MongoUtils<>(GACategoryTag.class, mongodbSourceCollectionName).retrieveCollectionFromDB();
        int counter = 0;
        for (GACategoryTag gACategoryTagAddManualTag : gACategoryTagAddManualTags) {
            if (gACategoryTagAddManualTag != null && StringUtils.isEmpty(gACategoryTagAddManualTag.getCategory())
                    && CollectionUtils.isNotEmpty(gACategoryTagAddManualTag.getTags())) {
                counter++;
                try {
                    LibSimResult libSimResult = lsClient.requestKi(gACategoryTagAddManualTag);
                    LibSimAnalyzedLibrary libSimAnalyzedLibrary = new LibSimAnalyzedLibrary(gACategoryTagAddManualTag.getId(),
                            gACategoryTagAddManualTag.getGroup(), gACategoryTagAddManualTag.getArtifact(), libSimResult);
                    if(Double.parseDouble(libSimAnalyzedLibrary.getPercent()) > threshold) {
                        gACategoryTagAddManualTag.setCategory(libSimAnalyzedLibrary.getPrediction());
                        gaCategoryTagMongoUtils.saveGACategoryTagToMongoDB(gACategoryTagAddManualTag);
                    }
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
