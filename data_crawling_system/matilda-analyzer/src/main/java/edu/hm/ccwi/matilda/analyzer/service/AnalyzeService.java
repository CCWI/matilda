package edu.hm.ccwi.matilda.analyzer.service;

import java.util.List;

public interface AnalyzeService {

    String analyzeProject(String crawledRepositoryId);

    List<String> findAndUpdateAllAnalyzableRepoIdsByState(String matildaState);
}
