package edu.hm.ccwi.matilda.analyzer.service.decision;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.jpa.repo.ExtractedDesignDecisionRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import javassist.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import edu.hm.ccwi.matilda.persistence.jpa.service.LibraryService;

import java.util.List;
import java.util.Set;

@Service
public class DecisionDetectionServiceImpl implements DecisionDetectionService {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionDetectionService.class);

    final DesignDecisionExtractor designDecisionExtractor;
    final ExtractedDesignDecisionRepository extractedDesignDecisionRepository;
    final LibraryService libraryService;

    public DecisionDetectionServiceImpl(DesignDecisionExtractor designDecisionExtractor,
                                        ExtractedDesignDecisionRepository extractedDesignDecisionRepository,
                                        LibraryService libraryService) {
        this.designDecisionExtractor = designDecisionExtractor;
        this.extractedDesignDecisionRepository = extractedDesignDecisionRepository;
        this.libraryService = libraryService;
    }

    @Transactional
    public void analyzeAndSaveDesignDecisions(CrawledRepository crawledRepository,
                                              List<CrawledRevision> revisionListOfRepository) throws NotFoundException {
        LOG.info("  Start analyze CrawledRevisions on design decisions");

        Set<ExtractedDesignDecisionEntity> extractedDesignDecisionEntities =
                designDecisionExtractor.analyzeRepositoryOnDD(crawledRepository, revisionListOfRepository);

        LOG.info("  Persist {} design decisions for repository: {}", extractedDesignDecisionEntities.size(), crawledRepository.getId());
        persistDesignDecisions(extractedDesignDecisionEntities);
    }

    private void persistDesignDecisions(Set<ExtractedDesignDecisionEntity> extractedDesignDecisionEntities) {
        if (CollectionUtils.isNotEmpty(extractedDesignDecisionEntities)) {
            if(LOG.isTraceEnabled()) {
                for (ExtractedDesignDecisionEntity extractedDesignDecisionEntity : extractedDesignDecisionEntities) {
                    LOG.trace("  Migrated: {} ---> {}", extractedDesignDecisionEntity.getInitial(), extractedDesignDecisionEntity.getTarget());
                    LOG.trace("            Subject: {} - on {}", extractedDesignDecisionEntity.getDecisionSubject(), extractedDesignDecisionEntity.getDecisionCommitTime());
                }
            }

            int knownExtractedDecision = 0;
            int unknownExtractedDecision = 0;
            for (ExtractedDesignDecisionEntity extractedDesignDecisionEntity : extractedDesignDecisionEntities) {
                if(!extractedDesignDecisionRepository
                        .existsByDecisionCommitTimeAndAndRepositoryAndProjectAndInitialAndTarget(extractedDesignDecisionEntity.getDecisionCommitTime(),
                                extractedDesignDecisionEntity.getRepository(), extractedDesignDecisionEntity.getProject(), extractedDesignDecisionEntity.getInitial(),
                                extractedDesignDecisionEntity.getTarget())) {
                    extractedDesignDecisionRepository.save(extractedDesignDecisionEntity);
                    saveLibrary(extractedDesignDecisionEntity);
                    unknownExtractedDecision += 1;
                } else {
                    LOG.trace("Decision already saved. Skip...");
                    knownExtractedDecision += 1;
                }
            }
            LOG.info("Extracted design decisions which were unknown yet: {}, already known: {}", unknownExtractedDecision, knownExtractedDecision);
        }
    }

    private void saveLibrary(ExtractedDesignDecisionEntity extractedDesignDecisionEntity) {
        String initial = extractedDesignDecisionEntity.getInitial();
        String target = extractedDesignDecisionEntity.getTarget();
        try {
            if (StringUtils.isNotEmpty(initial) && !StringUtils.equals(initial, "NULL")) { //NOT "NULL"
                libraryService.saveOrUpdateLibrary(initial, extractedDesignDecisionEntity.getDecisionSubject(), null);
            }
            if (StringUtils.isNotEmpty(target) && !StringUtils.equals(target, "NULL")) { //NOT "NULL"
                libraryService.saveOrUpdateLibrary(target, extractedDesignDecisionEntity.getDecisionSubject(), null);
            }
        } catch (IllegalArgumentException e) {
            LOG.error("Found Library-GA which has no common ID-Structure: {}/{}, skipped...", initial, target);
        }
    }

}
