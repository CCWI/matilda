package edu.hm.ccwi.matilda.korpus.analyses;

import edu.hm.ccwi.matilda.base.util.StringHandler;
import edu.hm.ccwi.matilda.korpus.util.CsvUtils;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DistributionCalculator {

    /**
     * Calculate amount of tags per category and write to csv.
     * @param args
     */
    public static void main(String[] args) {

        List<GACategoryTag> gACategoryTagsList = new MongoUtils<>(GACategoryTag.class, "gACategoryTag").retrieveCollectionFromDB();

        // Map tags to each category
        Map<String, List<String>> categoryTagMap = new HashMap<>();
        for (GACategoryTag gaCategoryTag : gACategoryTagsList) {
            if(gaCategoryTag.getCategory() == null && StringUtils.isEmpty(gaCategoryTag.getCategory())) {
                continue;
            }
            String stripCategory = StringHandler.stripForCategoryString(gaCategoryTag.getCategory());
            if(categoryTagMap.containsKey(stripCategory)) {
                List<String> tagList = categoryTagMap.get(stripCategory);
                for (String tag : gaCategoryTag.getTags()) {
                    if(!tagList.contains(tag.toLowerCase())) {
                        tagList.add(tag.toLowerCase());
                    }
                }
            } else {
                if(CollectionUtils.isNotEmpty(gaCategoryTag.getTags())) {
                    List<String> lowTagList = new ArrayList<>();
                    for (String tag : gaCategoryTag.getTags()) {
                        lowTagList.add(tag.toLowerCase());
                    }
                    categoryTagMap.put(stripCategory, lowTagList);
                } else {
                    categoryTagMap.put(stripCategory, new ArrayList<>());
                }
            }
        }

        String overlapCsv = "calculated-overlap.csv";
        CsvUtils.appendToCsv(overlapCsv, "###(DISTRIBUTION OF TAGS)###########################", false);

        String columnLabelString = "";
        for (Map.Entry<String, List<String>> categoryTags1 : categoryTagMap.entrySet()) {
            columnLabelString = columnLabelString + categoryTags1.getKey()+ ", ";
        }
        CsvUtils.appendToCsv(overlapCsv, columnLabelString, false);

        String rowLabelString = "";
        for (Map.Entry<String, List<String>> stringListEntry : categoryTagMap.entrySet()) {
            rowLabelString = rowLabelString + stringListEntry.getValue().size() + ", ";
        }

        CsvUtils.appendToCsv(overlapCsv, rowLabelString, false);
        CsvUtils.appendToCsv(overlapCsv, "##############################", true);
    }
}
