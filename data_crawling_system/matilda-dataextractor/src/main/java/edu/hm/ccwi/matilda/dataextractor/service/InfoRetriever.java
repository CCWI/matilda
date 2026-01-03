package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDocumentation;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;

import java.util.List;

public interface InfoRetriever {

    CrawledDocumentation extractDocumentation(String commitId, String revisionPath) throws Exception;

    boolean isMDInProject(String projectUri);

    List<CrawledRevision> filterFirstPartyProjectDependencies(List<CrawledRevision> revList);

    List<CrawledRevision> filterUnresolvedProjectDependencies(List<CrawledRevision> revList);
}
