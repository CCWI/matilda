package edu.hm.ccwi.matilda.korpus.libsim;

import com.mongodb.BasicDBObject;
import edu.hm.ccwi.matilda.base.util.ProgressHandler;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * After noticing that gACategoryTagAddManualTags includes maybe some more information then gACategoryTagTotal leads
 * to this enricher.
 */
public class GACategoryTagManualTagsToTotalEnricher {

    private MongoUtils<BasicDBObject> gACategoryTagTotalMongoUtils;
    private MongoUtils<BasicDBObject> gaCategoryAddManualTagsMongoUtils;
    private String mongodbSourceCollectionName;
    private String mongodbTargetCollectionName;
    private double threshold;
    private LibSimClient lsClient;

    public GACategoryTagManualTagsToTotalEnricher() {
        this.mongodbSourceCollectionName = "gACategoryTagAddManualTags";
        this.mongodbTargetCollectionName = "gACategoryTagTotal";
        this.gaCategoryAddManualTagsMongoUtils = new MongoUtils<>(mongodbSourceCollectionName);
        this.gACategoryTagTotalMongoUtils = new MongoUtils<>(mongodbTargetCollectionName);
        this.threshold = 0.20;
        this.lsClient = new LibSimClient();
    }

    public void enrich() throws IOException, InterruptedException {
        List<BasicDBObject> gaCategoryAddManualTagsBasicDBObjectCollection = gaCategoryAddManualTagsMongoUtils.retrieveCollectionFromDB();
        List<BasicDBObject> gaCategoryTagsTotalBasicDBObjectCollection = gACategoryTagTotalMongoUtils.retrieveCollectionFromDB();

        List<GACategoryTag> gaCategoryAddManualTagsCollection =
                gACategoryTagTotalMongoUtils.mapBasicDBObjectToGACategoryTagObject(gaCategoryAddManualTagsBasicDBObjectCollection);
        List<GACategoryTag> gaCategoryTagsTotalCollection =
                gACategoryTagTotalMongoUtils.mapBasicDBObjectToGACategoryTagObject(gaCategoryTagsTotalBasicDBObjectCollection);

        System.out.println("gaCategoryAddManualTagsCollection size: " + gaCategoryAddManualTagsCollection.size());
        System.out.println("gaCategoryTagsTotalCollection size: " + gaCategoryTagsTotalCollection.size());

        //1) Check if lib from total in addManual and check if there is more information to transfer + save
        updateMissingInformationInTotal(gaCategoryAddManualTagsCollection, gaCategoryTagsTotalCollection);

        //2) Check if lib from addManual is not available in total and add it to total if it is categorized.
        complementTotalCollection(gaCategoryAddManualTagsCollection, gaCategoryTagsTotalCollection);
    }

    /**
     * STEP 1: Check if lib from total in addManual and check if there is more information to transfer + save
     */
    private void updateMissingInformationInTotal(List<GACategoryTag> gaCategoryAddManualTagsCollection, List<GACategoryTag> gaCategoryTagsTotalCollection) {
        System.err.println("### START ENRICH TOTALCOLLECTION BY UPDATING MISSING CATEGORY/TAGS ");
        ProgressHandler progressHandler = new ProgressHandler(gaCategoryTagsTotalCollection.size());
        int updatedGACategoryTagTotalCounter = 0;
        for (GACategoryTag gaCategoryTagTotal : gaCategoryTagsTotalCollection) {
            int progress = progressHandler.incrementProgress();
            if (progress % 50 == 0) {
                System.out.println("Progress enrich gaCategoryTotal: ["+progress + "/" + progressHandler.getMaxAmount() +
                        "] -> Updated: " + updatedGACategoryTagTotalCounter);
            }
            if (StringUtils.isEmpty(gaCategoryTagTotal.getCategory())) {
                Optional<GACategoryTag> gactOptional = gaCategoryAddManualTagsCollection.stream()
                        .filter(gact -> gact.equals(gaCategoryTagTotal)).findFirst();

                if (gactOptional.isPresent()) {
                    GACategoryTag gaCategoryAddManualTag = gactOptional.get();
                    if (StringUtils.isNotEmpty(gaCategoryAddManualTag.getCategory())) {
                        gaCategoryTagTotal.setCategory(gaCategoryAddManualTag.getCategory());
                        gACategoryTagTotalMongoUtils.updateGACategoryTagToMongoDB(gaCategoryTagTotal);
                        updatedGACategoryTagTotalCounter++;
                    } else if (CollectionUtils.isEmpty(gaCategoryTagTotal.getTags()) && CollectionUtils.isNotEmpty(gaCategoryAddManualTag.getTags())) {
                        //CANNOT ACCESS LIBSIM IN DOCKER FROM OUTSIDE WITHOUT EXTRA CONFIG/STEPS
                        /*gaCategoryTagTotal.setTags(gaCategoryAddManualTag.getTags());
                        // CATEGORIZE VIA LIBSIM:
                        try {
                            categorizeViaLibsimAndUpdateCollection(gaCategoryTagTotal, gaCategoryAddManualTag);
                            gACategoryTagTotalMongoUtils.updateGACategoryTagToMongoDB(gaCategoryTagTotal);
                        } catch (Exception e) {
                            continue;
                        }*/
                    } else {
                        continue;
                    }
                } else {
                    //System.out.println("    Not found Lib of total in addManual: " + gaCategoryTagTotal.getId());
                }
            }
        }
    }

    /**
     * STEP 2: Check if lib from addManual is not available in total and add it to total if it is categorized.
     *         If it is only tagged, categorize by libsim
     */
    private void complementTotalCollection(List<GACategoryTag> gaCategoryAddManualTagsCollection, List<GACategoryTag> gaCategoryTagsTotalCollection) {
        System.err.println("### START ANALYZING ");
        ProgressHandler progressHandler = new ProgressHandler(gaCategoryAddManualTagsCollection.size());
        int complementedGACategoryTagTotalCounter = 0;
        for (GACategoryTag gaCategoryAddManualTag : gaCategoryAddManualTagsCollection) {
            int progress = progressHandler.incrementProgress();
            if (progress % 1000 == 0) {
                System.out.println("Progress complement gaCategoryTotal: ["+progress + "/" + progressHandler.getMaxAmount() +
                        "] -> Added Libs: " + complementedGACategoryTagTotalCounter);
            }
            if(!gaCategoryTagsTotalCollection.contains(gaCategoryAddManualTag)) {
                if(StringUtils.isNotEmpty(gaCategoryAddManualTag.getCategory())) { // lib not in total but categorized in AddManual
                    gACategoryTagTotalMongoUtils.saveGACategoryTagToMongoDB(gaCategoryAddManualTag);
                    complementedGACategoryTagTotalCounter++;
                } else if (CollectionUtils.isNotEmpty(gaCategoryAddManualTag.getTags())) { // lib not in Total but tagged -> categorize and save
                    // CATEGORIZE VIA LIBSIM - CANNOT ACCESS INSIDE DOCKER!
                    //categorizeViaLibsimAndUpdateCollection(gaCategoryAddManualTag, gaCategoryAddManualTag);
                } else {
                    continue;
                }
            }
        }
    }

    private void categorizeViaLibsimAndUpdateCollection(GACategoryTag gaCategoryTagTotal, GACategoryTag gaCategoryAddManualTag) throws Exception {
        try {
            LibSimResult libSimResult = lsClient.requestKi(gaCategoryTagTotal);
            LibSimAnalyzedLibrary libSimAnalyzedLibrary = new LibSimAnalyzedLibrary(gaCategoryTagTotal.getId(),
                    gaCategoryTagTotal.getGroup(), gaCategoryTagTotal.getArtifact(), libSimResult);
            if (Double.parseDouble(libSimAnalyzedLibrary.getPercent()) > threshold) {
                gaCategoryTagTotal.setCategory(libSimAnalyzedLibrary.getPrediction());
                gACategoryTagTotalMongoUtils.updateGACategoryTagToMongoDB(gaCategoryTagTotal);
            }
        } catch (Exception e) {
            System.out.println("    Not able to categorize tagged library for total collection. Applied tags: " + gaCategoryAddManualTag.getTags());
            throw new Exception("Tags not categorizable -> ignore tags");
        }
    }
}
