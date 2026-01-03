package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.analyzer.service.model.AnalyzedGeneralResults;

import java.util.List;

public interface AnalyzeStatsService {

    AnalyzedGeneralResults analyzeGeneralStats();

    List<String> analyzeHeatmapStatOfMigrationsAndRelevantCommits(String decisionsubject);

    List<String> analyzeCategoriesOfMigrationsAndCommits(boolean useOnlyRelevantCommits);

    List<String> analyzeCategoriesOfMigrationsAndProjectAge();

    List<String> analyzeProjectCommitAgeAmountOfProjectMap(boolean useOnlyRelevantCommits);

    List<String> analyzeProjectCommitAgeDesignDecisionMap(boolean useOnlyRelevantCommits);

    List<String> retrieveDataListForRepositoryProjectsEDITION1();

    int amountOfAllRelevantRepositoriesAvailableInMongoCrawledStatistic();

    List<String> retrieveOverallRevisionTimeStampList();

    List<String> retrieveOverallMaximumAmountOfSWComponentsPerProjectList();
}
