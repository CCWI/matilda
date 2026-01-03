package edu.hm.ccwi.matilda.dataextractor.korpus;

import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.dataextractor.MatildaDataextractorApplication;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.base.util.LibraryUtilsAdapter;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
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
 */
@SpringBootTest(classes = MatildaDataextractorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class CleanupInconsistentDependencyCategoriesInMongoDB {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupInconsistentDependencyCategoriesInMongoDB.class);

    @Inject
    private CrawledRevisionRepository revRepository;

    @Inject
    private CrawledSoftwareRepository repoRepository;

    @Test
    public void cleanupRunner() {
        List<CrawledRepository> repositories = repoRepository.findAll();
        int fixedRevCounter = 0;
        for (CrawledRepository repository : repositories) {
            LOG.debug("Start cleanup categories in dependencies of crawled repository: {}:{}", repository.getRepositoryName(),
                    repository.getProjectName());
            for (String revisionId : repository.getRevisionCommitIdList()) {
                CrawledRevision revision = revRepository.findById(revisionId).orElse(null);

                if(revision == null) {
                    continue;
                }

                boolean fixedRev = false;
                for (CrawledProject crawledProject : revision.getProjectList()) {
                    for (CrawledDependency crawledDependency : crawledProject.getDependencyList()) {
                        String category = crawledDependency.getCategory();
                        if(category != null) {
                            try {
                                LibCategory libCategory = LibraryUtilsAdapter.resolveLibCategoryByString(category);
                                if (!StringUtils.equals(category, libCategory.getMatildaCategory())) {
                                    crawledDependency.setCategory(libCategory.getMatildaCategory());
                                    fixedRev = true;
                                    fixedRevCounter++;
                                }
                            } catch (MatildaMappingException e) {
                                LOG.error("Failed to get LibCategory for category: {}; exception: {}", category, e.getMessage());
                            }
                        }
                    }
                }
                if(fixedRev) {
                    LOG.info("Would have updated revision no. {}...", fixedRevCounter);
                    //revRepository.save(revision);
                }
            }
        }
    }
}
