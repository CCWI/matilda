package edu.hm.ccwi.matilda.dataextractor.service.cleaner;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InconsistencyMigrationBatchCleanerImpl implements InconsistencyMigrationBatchCleaner {

    CrawledRevisionRepository crawledRevisionRepository;

    public InconsistencyMigrationBatchCleanerImpl(CrawledRevisionRepository crawledRevisionRepository) {
        this.crawledRevisionRepository = crawledRevisionRepository;
    }

    public Boolean startBatchProcessingByImplementedRuleset() {

        List<CrawledRevision> allRevisions = crawledRevisionRepository.findAll();

        for (CrawledRevision revision : allRevisions) {
            // 1) Iterate each Project/Dependency + Known Replace inconsistent libraryCategories (unknown categories should be printed for next run)
            for (CrawledProject crawledProject : revision.getProjectList()) {
                for (CrawledDependency crawledDependency : crawledProject.getDependencyList()) {
                    if(StringUtils.isNotEmpty(crawledDependency.getCategory())) {
                        // Nachkategorisieren? -> Bringt nichts, wenn nochmal recrawling angedacht ist.
                        throw new NotImplementedException();
                    }
                }
            }
        }

        return Boolean.FALSE;
    }

}
