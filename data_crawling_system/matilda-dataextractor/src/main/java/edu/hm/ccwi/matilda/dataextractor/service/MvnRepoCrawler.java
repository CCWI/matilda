package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import org.springframework.stereotype.Service;

@Service
public interface MvnRepoCrawler {

    MvnRepoPage crawlMvnRepo(CrawledDependency cDependency);

    boolean isCrawledMvnRepoDependencyUncategorizedAndUntagged(MvnRepoPage mvnRepoPage);
}
