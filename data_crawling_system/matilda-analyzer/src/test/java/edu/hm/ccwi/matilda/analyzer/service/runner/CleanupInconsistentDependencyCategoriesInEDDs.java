package edu.hm.ccwi.matilda.analyzer.service.runner;

import edu.hm.ccwi.matilda.analyzer.MatildaAnalyzerApplication;
import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.util.LibraryUtilsAdapter;
import edu.hm.ccwi.matilda.persistence.jpa.repo.ExtractedDesignDecisionRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import java.util.List;

/**
 * NO TEST!
 * This Class helps to cleanup the inconsistency in DependencyCategories EDDs from Postgres
 */
@SpringBootTest(classes = MatildaAnalyzerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class CleanupInconsistentDependencyCategoriesInEDDs {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupInconsistentDependencyCategoriesInEDDs.class);

    @Inject
    ExtractedDesignDecisionRepository eddRepository;

    @Test
    void calcMigrationRankForCharacteristicsInCategoryAndType() {
        List<ExtractedDesignDecisionEntity> eddList = eddRepository.findAll();

        int fixedEddCounter = 0;
        for (ExtractedDesignDecisionEntity edd : eddList) {
            boolean fixedEDD = false;
            String category = edd.getDecisionSubject();
            if(category != null) {
                try {
                    LibCategory libCategory = LibraryUtilsAdapter.resolveLibCategoryByString(category);
                    if (!StringUtils.equals(category, libCategory.getMatildaCategory())) {
                        edd.setDecisionSubject(libCategory.getMatildaCategory());
                        fixedEDD = true;
                        fixedEddCounter++;
                    }
                } catch (MatildaMappingException e) {
                    LOG.error("Failed to get LibCategory for category: {}; exception: {}", category, e.getMessage());
                }
            }

            if(fixedEDD) {
                LOG.info("Update inconsistent eDD no. {}...", fixedEddCounter);
                eddRepository.save(edd);
            }
        }
    }
}