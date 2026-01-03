package edu.hm.ccwi.matilda.analyzer.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GenericAnalyzer {
    private static final Logger LOG = LoggerFactory.getLogger(GenericAnalyzer.class);

    final CrawledRevisionRepository mongoRevRepo;

    public GenericAnalyzer(CrawledRevisionRepository mongoRevRepo) {
        this.mongoRevRepo = mongoRevRepo;
    }

    List<CrawledRevision> retrieveRevisionsFromRepository(CrawledRepository crawledRepository) {
        LOG.info("  Start retrieving CrawledRevisions for repository: {}", crawledRepository.getId());
        List<CrawledRevision> crawledRevisions = new ArrayList<>();

        for (String revisionCommitId : crawledRepository.getRevisionCommitIdList()) {
            Optional<CrawledRevision> crawledRevisionOpt = mongoRevRepo.findById(revisionCommitId);
            if(crawledRevisionOpt.isPresent()) {
                crawledRevisions.add(crawledRevisionOpt.get());
            } else {
                LOG.warn("  Revision {} not available in project: {}", revisionCommitId, crawledRepository.getId());
            }
        }

        LOG.info("    --> Found {} CrawledRevisions", crawledRevisions.size());

        return crawledRevisions;
    }
}
