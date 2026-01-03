package edu.hm.ccwi.matilda.base.util;

import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LibraryUtilsAdapterTest {

    @Test
    void getLibCategoryByString_ByMatildaCategory() throws MatildaMappingException {
        String matildaCategory = "Testing / Mocking / Evaluating";
        LibCategory libCategoryByString = LibraryUtilsAdapter.resolveLibCategoryByString(matildaCategory);

        assertThat(libCategoryByString).isNotNull();
        assertThat(libCategoryByString.getMatildaCategory()).isEqualTo(matildaCategory);
    }

    @Test
    void getLibCategoryByString_ByMvnCategory() throws MatildaMappingException {
        String mvnCategory = "Validation Frameworks";
        LibCategory libCategoryByString = LibraryUtilsAdapter.resolveLibCategoryByString(mvnCategory);
        assertThat(libCategoryByString).isNotNull();
        assertThat(libCategoryByString.getName()).isEqualTo(mvnCategory);
    }

    @Test
    void getLibCategoryByString_ByUnformatedMvnCategory() throws MatildaMappingException {
        String formatedMvnCategory = "Validation Frameworks";
        String unformatedMvnCategory = "VALIDATION-FRAMEWORKS";
        LibCategory libCategoryByString = LibraryUtilsAdapter.resolveLibCategoryByString(unformatedMvnCategory);
        assertThat(libCategoryByString).isNotNull();
        assertThat(libCategoryByString.getName()).isEqualTo(formatedMvnCategory);
    }

    @Test
    void getLibCategoryByString_ByUnknownCategory() {

        MatildaMappingException exception =
                assertThrows(MatildaMappingException.class, () -> LibraryUtilsAdapter.resolveLibCategoryByString("New Exiting Category"));

        assertThat(exception.getMessage()).isEqualTo("No enum constant edu.hm.ccwi.matilda.base.model.enumeration.LibCategory.NEW_EXITING_CATEGORY");
    }
}