package edu.hm.ccwi.matilda.base.util;

import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import org.apache.commons.lang3.StringUtils;

import static edu.hm.ccwi.matilda.base.util.StringHandler.stripForCategoryEnum;

public class LibraryUtilsAdapter {

    public static LibCategory resolveLibCategoryByString(String category) throws MatildaMappingException {
        LibCategory lbc;
        if (StringUtils.isNotEmpty(category)) {
            try {
                // get LibCategory if string is already a matilda category
                lbc = LibCategory.getByMatildaCategory(category.trim());

                // get LibCategory if string is a category from MvnRepository
                if (lbc == null) {
                    lbc = LibCategory.valueOf(stripForCategoryEnum(category));
                }
            } catch (IllegalArgumentException e) {
                throw new MatildaMappingException(e.getMessage());
            }

            // throw exception if category is still unknown
            if (lbc == null) {
                throw new MatildaMappingException("Error while retrieving LibCategory by String: " + category);
            }
        } else {
            throw new MatildaMappingException("Provided category string should not be empty");
        }

        return lbc;
    }
}
