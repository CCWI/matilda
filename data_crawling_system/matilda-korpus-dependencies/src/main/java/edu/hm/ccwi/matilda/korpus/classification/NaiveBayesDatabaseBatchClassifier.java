package edu.hm.ccwi.matilda.korpus.classification;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.korpus.DependencyClassificationEvaluator;
import edu.hm.ccwi.matilda.base.util.MapIndexHandler;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.spark.ml.classification.NaiveBayesModel;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import scala.collection.JavaConversions;
import scala.collection.mutable.WrappedArray;

import java.util.*;

public class NaiveBayesDatabaseBatchClassifier {

    // #####################
    // ### Configuration ###
    // #####################
    private static final String[] EXCLUDED_TAGS =
            {"github", "codehaus", "clojure", "apache", "experimental", "starter", "runner", "api", "bom", "library", "framework"};
    private static final String[] EXCLUDED_CATEGORIES = {"androidpackages", "programmablelogiccontroller"};
    private static final boolean EXCLUDE_CATEGORIES = true;
    private static final String GA_CATEGORY_TAG_DATABASE_COLLECTION = "gACategoryTagAddManualTags";

    public static void main(String[] args) throws Exception {
        DependencyClassificationEvaluator dependencyKorpusCreator = new DependencyClassificationEvaluator();

        // load/get all labeled/tagged dependencies (from gACategoryTagAddManualTags)
        List<GACategoryTag> gAManualCategoryTagsList = new MongoUtils<>(GACategoryTag.class, GA_CATEGORY_TAG_DATABASE_COLLECTION)
                .retrieveCollectionFromDB();
        Collections.shuffle(gAManualCategoryTagsList);
        List<GACategoryTag> gaCategoryTagTrainingList = dependencyKorpusCreator
                .filterForCategorizedTaggedDependencies(gAManualCategoryTagsList, EXCLUDE_CATEGORIES, EXCLUDED_CATEGORIES);

        // load/get all only-tagged dependencies (from gACategoryTag)
        List<GACategoryTag> gaTaggedOnlyList = dependencyKorpusCreator
                .filterForTaggedOnlyDependencies(new MongoUtils<>(GACategoryTag.class, GA_CATEGORY_TAG_DATABASE_COLLECTION)
                        .retrieveCollectionFromDB());

        for (String excludedTag : EXCLUDED_TAGS) {
            gaCategoryTagTrainingList.forEach(ga -> ga.getTags().remove(excludedTag));
            gaTaggedOnlyList.forEach(ga -> ga.getTags().remove(excludedTag));
        }
        gaCategoryTagTrainingList.removeIf(gaCategoryTag -> gaCategoryTag.getTags().isEmpty());
        gaCategoryTagTrainingList.removeIf(gaCategoryTag -> gaCategoryTag.getCategory().isEmpty());
        gaTaggedOnlyList.removeIf(gaCategoryTag -> gaCategoryTag.getTags().isEmpty());

        DependencyClassificationEvaluator.mapMvnRepoLabelsToMatildaLabels(gaCategoryTagTrainingList);

        // train model
        // ####### Classification and Evaluation ######
        List<Row> gaCategoryTagListData = new ArrayList<>();
        Map<Integer, String> indexMapForCategories = DependencyClassificationEvaluator.createIndexMapForCategories(gaCategoryTagTrainingList);
        for (GACategoryTag gaCategoryTag : gaCategoryTagTrainingList) {
            gaCategoryTagListData.add(RowFactory.create(MapIndexHandler.getKeyByValue(indexMapForCategories,
                    gaCategoryTag.getCategory()), gaCategoryTag.getTags()));
        }
        String[] gaCategoryTagBoW = dependencyKorpusCreator.createBoWfromTags(gaCategoryTagTrainingList);
        ClassificationProcessor classificationProcessor = new ClassificationProcessor(indexMapForCategories);
        NaiveBayesModel nbModel = classificationProcessor.trainNaiveBayesClassificationModel(gaCategoryTagListData, gaCategoryTagBoW);

        System.out.println("### Number of classes: " + nbModel.numClasses());
        System.out.println("### Number of features: " + nbModel.numFeatures());

        // classify each dependency by model
        String[] gaTagOnlyBoW = dependencyKorpusCreator.createBoWfromTags(gaCategoryTagTrainingList);
        List<Row> data = new ArrayList<>();
        for (GACategoryTag gaCategoryTag : gaTaggedOnlyList) {
            data.addAll(Collections.singletonList(RowFactory.create(gaCategoryTag.getGroup() +
                    ":" + gaCategoryTag.getArtifact(), gaCategoryTag.getTags())));
        }

        Dataset<Row> resultDataset = classificationProcessor.classifyDataByModel(nbModel, gaTagOnlyBoW, data);
        resultDataset.select("features", "ga", "prediction", "probability", "text").show(100, false);

        List<GACategoryTag> gaCategoryTagResultList = new ArrayList<>();
        List<Row> resultList = resultDataset.collectAsList();
        resultList.forEach(row -> {
            GACategoryTag gaCategoryTag = new GACategoryTag(row.get(0).toString());
            gaCategoryTag.setCategory(indexMapForCategories.get(((Double) row.get(5)).intValue()));
            Collection<String> tagCol = JavaConversions.asJavaCollection(((WrappedArray<String>) row.get(1)).toList());
            CollectionUtils.addAll(gaCategoryTag.getTags(), tagCol);
            gaCategoryTagResultList.add(gaCategoryTag);
        });

        gaCategoryTagResultList.forEach(entry -> System.err.println(entry.toString()));

        // save newly classified dependencies to gACategoryTag
        gaCategoryTagResultList.addAll(gaCategoryTagTrainingList);
        new MongoUtils<GACategoryTag>("gACategoryTagTotal").saveGACategoryTagListToMongoDB(gaCategoryTagResultList);
    }
}
