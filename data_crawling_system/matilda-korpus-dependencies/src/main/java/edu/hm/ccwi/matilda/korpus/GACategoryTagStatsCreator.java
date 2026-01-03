package edu.hm.ccwi.matilda.korpus;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.korpus.util.CsvUtils;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.*;

public class GACategoryTagStatsCreator {

    public static void main(String[] args) {
        CsvUtils.appendToCsv("classificationStats.csv", "######################################", false);
        CsvUtils.appendToCsv("classificationStats.csv", "##### "+ LocalDateTime.now().toString() +" #####", false);
        CsvUtils.appendToCsv("classificationStats.csv", "######################################", false);

        List<GACategoryTag> gaCategoryTagList = new MongoUtils<>(GACategoryTag.class, "gACategoryTagTotal").retrieveCollectionFromDB(); // all
        List<GACategoryTag> categorizedGaCategoryTag = new ArrayList<>();           // categorized
        List<GACategoryTag> taggedOnlyGaCategoryTag = new ArrayList<>();            // tagged

        for(GACategoryTag gaCategoryTag : gaCategoryTagList) {
            if(StringUtils.isNotBlank(gaCategoryTag.getCategory())) {
                categorizedGaCategoryTag.add(gaCategoryTag);
            }
            if(StringUtils.isEmpty(gaCategoryTag.getCategory()) && gaCategoryTag.getTags() != null && !gaCategoryTag.getTags().isEmpty()) {
                taggedOnlyGaCategoryTag.add(gaCategoryTag);
            }
        }


        /*
         * CREATE TAG DISTRIBUTION LIST
         */
        CsvUtils.appendToCsv("classificationStats.csv", "TagDistributionStats of gaCategoryTagList", false);
        createTagDistributionStats(gaCategoryTagList);
        CsvUtils.appendToCsv("classificationStats.csv", "TagDistributionStats of categorizedGaCategoryTag", false);
        createTagDistributionStats(categorizedGaCategoryTag);
        CsvUtils.appendToCsv("classificationStats.csv", "TagDistributionStats of taggedOnlyGaCategoryTag", false);
        createTagDistributionStats(taggedOnlyGaCategoryTag);

        CsvUtils.appendToCsv("classificationStats.csv", "", true);
        CsvUtils.appendToCsv("classificationStats.csv", "", true);


        /*
         * CREATE TAG COMBINATION LIST INCL AMOUNT
         */
        CsvUtils.appendToCsv("classificationStats.csv", "Tag combinations of categorized libs:", false);
        printTagCombinationMap(createTagCombinationAmountList(gaCategoryTagList)); // tag combinations + amount

        CsvUtils.appendToCsv("classificationStats.csv", "", true);
        CsvUtils.appendToCsv("classificationStats.csv", "", true);


        /*
         * CREATE UNKNOWN TAG COMBINATIONS OF LIBRARIES IN TAGONLY-LISTS
         */
        CsvUtils.appendToCsv("classificationStats.csv", "Unknown Tag Combinations of Libraries in tagonly-lists:", false);
        printTagCombinationStringMap(createTagCombinationOfUnknownTagsInTagOnlyLists(gaCategoryTagList, taggedOnlyGaCategoryTag));

        CsvUtils.appendToCsv("classificationStats.csv", "", true);

    }

    // #################################################################################################################
    // ### HELP METHODS ################################################################################################
    // #################################################################################################################

    private static HashMap<String, String> createTagCombinationOfUnknownTagsInTagOnlyLists(List<GACategoryTag> gaCategoryTags,
                                                                                            List<GACategoryTag> gaTaggedOnly) {
        CsvUtils.appendToCsv("classificationStats.csv", "tagcombi | count |", false);

        HashMap<String, Integer> tagCombinationOfCategorizedLibsMap = createTagCombinationAmountList(gaCategoryTags);
        HashMap<String, Integer> tagCombinationOfTaggedOnlyLibsMap = createTagCombinationAmountList(gaTaggedOnly);

        List<String> unknownKeyList = new ArrayList<>();
        Iterator<String> keySetIterator = tagCombinationOfTaggedOnlyLibsMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            String key = keySetIterator.next();
            if(!tagCombinationOfCategorizedLibsMap.containsKey(key)) {
                unknownKeyList.add(key);
            }
        }
        return createTagCombinationLibraryList(unknownKeyList, gaTaggedOnly);
    }

    private static HashMap<String, String> createTagCombinationLibraryList(List<String> keyList, List<GACategoryTag> gaCategoryTags) {
        HashMap<String, String> tagCombinationMap = new HashMap();
        for (GACategoryTag gaCategoryTag : gaCategoryTags) {
            Collections.sort(gaCategoryTag.getTags());
            String tagCombi = String.join(" | ", gaCategoryTag.getTags());
            if(keyList.contains(tagCombi)) {
                tagCombinationMap.put(gaCategoryTag.getId(), tagCombi);
            }
        }
        return tagCombinationMap;
    }

    private static HashMap<String, Integer> createTagCombinationAmountList(List<GACategoryTag> gaCategoryTags) {
        HashMap<String, Integer> tagCombinationMap = new HashMap();
        for (GACategoryTag gaCategoryTag : gaCategoryTags) {
            Collections.sort(gaCategoryTag.getTags());
            String tagCombi = String.join(" | ", gaCategoryTag.getTags());
            if (tagCombinationMap.containsKey(tagCombi)) {
                Integer count = tagCombinationMap.get(tagCombi);
                count++;
                tagCombinationMap.put(tagCombi, count);
            } else {
                tagCombinationMap.put(tagCombi, 1);
            }
        }
        return tagCombinationMap;
    }

    private static void printTagCombinationMap(HashMap<String, Integer> tagCombinationMap) {
        CsvUtils.appendToCsv("classificationStats.csv", "tagcombi | count |", false);
        Iterator<String> keySetIterator = tagCombinationMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            String key = keySetIterator.next();
            System.out.println("Key : " + key + "   Value : " + tagCombinationMap.get(key));
            CsvUtils.appendToCsv("classificationStats.csv", tagCombinationMap.get(key) + " | " + key, false);
        }
    }

    private static void printTagCombinationStringMap(HashMap<String, String> tagCombinationMap) {
        CsvUtils.appendToCsv("classificationStats.csv", "libs | tagcombi |", false);
        Iterator<String> keySetIterator = tagCombinationMap.keySet().iterator();
        while (keySetIterator.hasNext()) {
            String key = keySetIterator.next();
            System.out.println("Key : " + key + "   Value : " + tagCombinationMap.get(key));
            CsvUtils.appendToCsv("classificationStats.csv", tagCombinationMap.get(key) + " | " + key, false);
        }
    }

    private static void createTagDistributionStats(List<GACategoryTag> gaCategoryTags) {
        CsvUtils.appendToCsv("classificationStats.csv", "0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12 | 13 |", false);

        //HashMap<Integer, Integer> distributionMap = new HashMap();
        int[] distribution = new int[14];
        for(GACategoryTag gaCategoryTag : gaCategoryTags) {
            distribution[gaCategoryTag.getTags().size()]++;
        }

        StringBuilder distributionCount = new StringBuilder();
        for(int i = 0; i < 14; i++) {
            distributionCount.append(distribution[i] + " | ");
        }
        CsvUtils.appendToCsv("classificationStats.csv", distributionCount.toString(), false);
    }
}
