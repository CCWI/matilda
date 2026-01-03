package edu.hm.ccwi.matilda.korpus;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.util.MapIndexHandler;
import edu.hm.ccwi.matilda.base.util.StringHandler;
import edu.hm.ccwi.matilda.korpus.classification.ClassificationProcessor;
import edu.hm.ccwi.matilda.korpus.classification.EvaluationResult;
import edu.hm.ccwi.matilda.korpus.util.*;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.time.LocalDateTime;
import java.util.*;

public class DependencyClassificationEvaluator {

    public static void main(String[] args) throws Exception {
        DependencyClassificationEvaluator dependencyKorpusCreator = new DependencyClassificationEvaluator();

        // #####################
        // ### Configuration ###
        // #####################
        String dataCollectionName = "gACategoryTag"; //"gACategoryTag" or "gACategoryTagAddManualTags"

        // exclude categories or balance classes
        String[] excludedCategories = {"androidpackages", "programmablelogiccontroller", "null", ""}; //, "mavenplugins", "webapplications", "example"
        String[] excludedTags = {"github", "codehaus", "clojure", "apache", "experimental", "starter", "runner", "api", "bom", "null", ""};
        // übergreifende, unwichtige Tags entfernen (native? interface? eclipse? jboss?)
        boolean excludeCategories = true;
        boolean excludeTags = true;
        boolean mapMvnRepoToMatildaCategories = false; //false for origin
        // sum should be 1; perform k-fold cross-validation, by double[].size = k
        double[] splitOnData = new double[]{0.1, 0.1, 0.1, 0.1, 0.1};
        long jobTimeMillis = System.currentTimeMillis();
 
        // ###############################
        // ### PREPARE LIBRARY-DATASET ###
        // ###############################
        List<GACategoryTag> gaList = new MongoUtils<>(GACategoryTag.class, dataCollectionName).retrieveCollectionFromDB();

        if(!mapMvnRepoToMatildaCategories) { // IF NO MAPPING TO NEW CATEGORIES, STRIP EXISTING CATEGORIES
            for (GACategoryTag gaCategoryTag : gaList) {
                if(gaCategoryTag.getCategory() != null) {
                    gaCategoryTag.setCategory(StringHandler.stripForCategoryString(gaCategoryTag.getCategory()));
                }
            }
        }

        Collections.shuffle(gaList);
        List<GACategoryTag> gaCategoryTagList = dependencyKorpusCreator.filterForCategorizedTaggedDependencies(gaList, excludeCategories, excludedCategories); //get training-dataset
        List<GACategoryTag> gaTaggedOnlyList = dependencyKorpusCreator.filterForTaggedOnlyDependencies(gaList); //get target-dataset

        if(!mapMvnRepoToMatildaCategories) { // IF NO MAPPING TO NEW CATEGORIES, STRIP EXISTING CATEGORIES
            for (GACategoryTag gaCategoryTag : gaCategoryTagList) {
                if(gaCategoryTag.getCategory() != null) { gaCategoryTag.setCategory(StringHandler.stripForCategoryString(gaCategoryTag.getCategory())); }
            }
            for (GACategoryTag gaCategoryTag : gaTaggedOnlyList) {
                if(gaCategoryTag.getCategory() != null) { gaCategoryTag.setCategory(StringHandler.stripForCategoryString(gaCategoryTag.getCategory())); }
            }
        }

        // filter excluded tags if defined and remove all GAs which do not include any Tags
        if(excludeTags) {
            for (String excludedTag : excludedTags) {
                gaList.forEach(ga -> ga.getTags().remove(excludedTag));
                gaCategoryTagList.forEach(ga -> ga.getTags().remove(excludedTag));
                gaTaggedOnlyList.forEach(ga -> ga.getTags().remove(excludedTag));
            }
        }
        for (Iterator<GACategoryTag> iterator = gaList.iterator(); iterator.hasNext(); ) { if (iterator.next().getTags().isEmpty()) { iterator.remove(); } }
        for (Iterator<GACategoryTag> iterator = gaCategoryTagList.iterator(); iterator.hasNext(); ) { if (iterator.next().getTags().isEmpty()) { iterator.remove(); } }
        for (Iterator<GACategoryTag> iterator = gaTaggedOnlyList.iterator(); iterator.hasNext(); ) { if (iterator.next().getTags().isEmpty()) { iterator.remove(); } }

        // Include Category-Mapping
        if(mapMvnRepoToMatildaCategories) {
            mapMvnRepoLabelsToMatildaLabels(gaCategoryTagList);
        }

        // create BoW
        String[] gaCategoryTagBoW = dependencyKorpusCreator.createBoWfromTags(gaCategoryTagList);
        String[] gaTaggedOnlyBoW = dependencyKorpusCreator.createBoWfromTags(gaTaggedOnlyList);

        // Find Tags/Tag-Combinations which are not in CategoryTag
        List<String> gaBoWDiffNotAvailableInCategoryCombi = new ArrayList<>();
        for(String taggedOnlyTag : gaTaggedOnlyBoW) {
            if(!Arrays.stream(gaCategoryTagBoW).anyMatch(taggedOnlyTag::equals)) {
                gaBoWDiffNotAvailableInCategoryCombi.add(taggedOnlyTag);
            }
        }

        // Create Statistic
        String overview = "Total libs: (" + gaList.size() + ") - Categorized libs: (" + gaCategoryTagList.size()+") - " +
                "TaggedOnly libs: (" + gaTaggedOnlyList.size() + ")";
        createStatistics(overview, gaCategoryTagList, gaBoWDiffNotAvailableInCategoryCombi.toArray(String[]::new));

        // ############################################
        // ####### Classification and Evaluation ######
        // ############################################
        List<Row> gaCategoryTagListData = new ArrayList<>();
        Map<Integer, String> indexMapForCategories = createIndexMapForCategories(gaCategoryTagList);
        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            gaCategoryTagListData.add(RowFactory.create(MapIndexHandler.getKeyByValue(indexMapForCategories,
                    gaCategoryTag.getCategory()), gaCategoryTag.getTags()));
        }
        List<EvaluationResult> evaluationResults = new ClassificationProcessor(indexMapForCategories)
                .classificationAndEvaluation(gaCategoryTagListData, gaCategoryTagBoW, splitOnData, jobTimeMillis);
        for (EvaluationResult evaluationResult : evaluationResults) {
            System.err.println("Calculated evaluation results: " + evaluationResult.toString());
        }
    }

    /**
     * Since MvnRepo-Categories are in some cases not suitable for classification and further usages in MATILDA,
     * this method maps all categories to manually defined matilda-categories.
     *
     * @param gaCategoryTagList
     */
    public static void mapMvnRepoLabelsToMatildaLabels(List<GACategoryTag> gaCategoryTagList) throws Exception {
        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            String cat = LibCategory.valueOf(StringHandler.stripForCategoryEnum(gaCategoryTag.getCategory())).getMatildaCategory();
            if(cat == null) {
                throw new Exception("+++ ERROR - Didn't find: " + gaCategoryTag.getCategory());
            } else {
                gaCategoryTag.setCategory(cat);
            }
        }
    }

    /**
     * Create statistics about labeled dataset...
     *
     * @param overview
     * @param gaCategoryTagList
     * @param gaTaggedOnlyBoW
     */
    private static void createStatistics(String overview, List<GACategoryTag> gaCategoryTagList, String[] gaTaggedOnlyBoW) {
        CsvUtils.appendToCsv("classificationStats.csv", "######################################", false);
        CsvUtils.appendToCsv("classificationStats.csv", "##### "+ LocalDateTime.now().toString() +" #####", false);
        CsvUtils.appendToCsv("classificationStats.csv", "######################################", false);
        CsvUtils.appendToCsv("classificationStats.csv", "OVERVIEW: " + overview, false);

    // 1) Label Distribution
        Map<String, Integer> labelDistributionMap = new HashMap<>();
        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            String category = StringHandler.stripForCategoryString(gaCategoryTag.getCategory());
            labelDistributionMap.put(category, labelDistributionMap.getOrDefault(category, 0) + 1);
        }

        CsvUtils.appendToCsv("classificationStats.csv", "", true);
        CsvUtils.appendToCsv("classificationStats.csv", "### labelDistributionMap ###", false);
        labelDistributionMap.entrySet().forEach(entry -> {
            CsvUtils.appendToCsv("classificationStats.csv", entry.getKey() + " : " + entry.getValue(), false);
        });

    // 2) Topic lists on each categories
        CsvUtils.appendToCsv("classificationStats.csv", "", true);
        CsvUtils.appendToCsv("classificationStats.csv", "### topicListsForCategories ###", false);
        Map<String, List<String>> topicListsForCategoriesMap = new HashMap<>();
        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            String category = StringHandler.stripForCategoryString(gaCategoryTag.getCategory());
            if(topicListsForCategoriesMap.containsKey(category)) {
                List<String> topicList = topicListsForCategoriesMap.get(category);
                for(String tag : gaCategoryTag.getTags()) {
                    if(!topicList.contains(tag)) {
                        topicList.add(tag);
                    }
                }
                topicListsForCategoriesMap.put(category, topicList);
            } else {
                topicListsForCategoriesMap.put(category, new ArrayList<>());
            }
        }
        topicListsForCategoriesMap.forEach((cat, list) -> {
            CsvUtils.appendToCsv("classificationStats.csv", cat + " :: " + Arrays.toString(list.toArray(String[]::new)), false);
        });

    // 3) Unknown Tags (Tags which are not used on categorized Libraries
        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "### UnknownTags ###", false);
        CsvUtils.appendToCsv("classificationStats.csv", Arrays.toString(gaTaggedOnlyBoW), true);

    // 4)
        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "", true);

    // END
        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "--------- END ---------", true);
    }

    public static HashMap<Integer, String> createIndexMapForCategories(List<GACategoryTag> gaCategoryTagList) {
        HashMap<Integer, String> categoryLabelIndexMap = new HashMap<>();
        int indexCounter = 0;
        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            if(!categoryLabelIndexMap.containsValue(gaCategoryTag.getCategory())) {
                categoryLabelIndexMap.put(indexCounter, gaCategoryTag.getCategory());
                indexCounter++;
            }
        }
        return categoryLabelIndexMap;
    }

    /**
     * Filter for result-dataset
     *
     * @param gaList
     * @return
     */
    public List<GACategoryTag> filterForTaggedOnlyDependencies(List<GACategoryTag> gaList) {
        List<GACategoryTag> gaTaggedOnlyList = new ArrayList<>();
        for(GACategoryTag gaEntry : gaList) {
            if(StringUtils.isBlank(gaEntry.getCategory()) && gaEntry.getTags() != null && gaEntry.getTags().size() > 0) {
                gaTaggedOnlyList.add(gaEntry);
            }
        }
        return gaTaggedOnlyList;
    }

    /**
     * Filter for training-dataset
     *
     * @param gaList
     * @param excludeCategories
     * @param excludedCategories
     * @return
     */
    public List<GACategoryTag> filterForCategorizedTaggedDependencies(List<GACategoryTag> gaList,
                                                                      boolean excludeCategories, String[] excludedCategories) {
        List<GACategoryTag> gaCategoryTagList = new ArrayList<>();
        for(GACategoryTag gaEntry : gaList) {
            if(StringUtils.isNotBlank(gaEntry.getCategory()) && gaEntry.getTags() != null && gaEntry.getTags().size() > 0) {
                if(!excludeCategories || !gaCategoryIsExcluded(gaEntry.getCategory(), Arrays.asList(excludedCategories))) {
                    gaCategoryTagList.add(gaEntry);
                }
            }
        }
        return gaCategoryTagList;
    }

    private boolean gaCategoryIsExcluded(String category, List<String> excludedCategories) {
        if(excludedCategories != null && excludedCategories.size() > 0 &&
                excludedCategories.contains(StringHandler.stripForCategoryString(category))) {
            return true;
        }
        return false;
    }

    /**
     * Create BoW from Dependency-List
     *
     * @return
     */
    public String[] createBoWfromTags(List<GACategoryTag> gaCategoryTagList) {
        List<String> BoW = new ArrayList<>();
        for(GACategoryTag gaEntry : gaCategoryTagList) {
            if(gaEntry.getTags() != null && gaEntry.getTags().size() > 0) {
                for(String gaEntryTag : gaEntry.getTags()) {
                    BoW.add(gaEntryTag);
                }
            }
        }
        return Lists.newArrayList(Sets.newHashSet(BoW)).toArray(String[]::new);
    }

}
