package edu.hm.ccwi.matilda.base.util;

public class StringHandler {

    public static String stripForCategoryEnum(String tag) {
        return tag.trim()
                .replace(" / ", "_")
                .replace(" - ", "_")
                .replace(" _ ", "_")
                .replace(", ", "_")
                .replace("   ", "_")
                .replace("  ", "_")
                .replace(" ", "_")
                .replace(",", "_")
                .replace(".", "_")
                .replace("/", "_")
                .replace("-", "_")
                .replace(" ", "_")
                .toUpperCase().trim();
    }

    /**
     * Convert to lowercase concatenated category string
     * @param category
     * @return
     */
    public static String stripForCategoryString(String category) {
        return category.replace("-", "")
                .replace("_", "")
                .replace(",", "")
                .replace(".","")
                .replace("/", "")
                .replace("  ", "")
                .replace(" ", "")
                .replace(" ", "")
                .trim().toLowerCase();
    }
}
