package edu.hm.ccwi.matilda.analyzer.service.decision;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import javassist.NotFoundException;

import java.util.List;

public interface DecisionDetectionService {

    void analyzeAndSaveDesignDecisions(CrawledRepository crawledRepository,
                                       List<CrawledRevision> revisionListOfRepository) throws NotFoundException;
}
