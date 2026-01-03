package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledStatistic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ProjectCommitSizeDesignDecisionAnalyzer extends AbstractProjectDesignDecisionAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectCommitSizeDesignDecisionAnalyzer.class);

    public List<String> calculateHeatmapForAmountOfMigrationsByRelevantCommits(List<ExtractedDesignDecisionEntity> ddList,
                                                                               List<CrawledRepository> allReposFinishedProcessing,
                                                                               String subjectToFilter) {
        LOG.info("Start calculateHeatmapForAmountOfMigrationsByCommits for {} Design Decisions", ddList.size());
        int maxRevSize = calculateMaxRevisionSize(allReposFinishedProcessing);
        int[][] heatmap = createMapOfCommitsAndMigrationsForSubject(ddList, allReposFinishedProcessing, subjectToFilter,
                maxRevSize, true, null);

        List<String> flatList = new ArrayList<>();
        for (int[] ddCommits : heatmap) {
            StringJoiner stringJoiner = new StringJoiner(",");
            for (int ddCommit : ddCommits) {
                stringJoiner.add(String.valueOf(ddCommit));
            }
            flatList.add(stringJoiner.toString());
        }

        return flatList;
    }

    /**
     * Method is intended to deliver an analysis of each category and all in one about commits and migrations.
     *
     * @param ddList
     * @param allReposFinishedProcessing
     * @param allCrawledStatistics
     * @param useOnlyRelevantCommits
     * @return
     */
    public List<String> calculateSummaryMigrationsAndCommitsOfEachCategory(List<ExtractedDesignDecisionEntity> ddList,
                                                                           List<CrawledRepository> allReposFinishedProcessing,
                                                                           Map<String, CrawledStatistic> allCrawledStatistics,
                                                                           boolean useOnlyRelevantCommits) {
        List<String> summaryList = new ArrayList<>();
        LOG.info("calculates summary of migrations and commits of each category");
        int maxRevSize = useOnlyRelevantCommits ? calculateMaxRevisionSize(allReposFinishedProcessing) :
                findMaxRevCountFromCrawledStatistics(allCrawledStatistics);
        int counter = 1;
        Set<String> ddSubjects = retrieveAllSubjects(ddList);
        LOG.info("  found dd-subjects to iterate: {}", ddSubjects);
        for (String ddSubject : ddSubjects) {
            LOG.info("  calculates summary of migrations and commits of each category: {}/{} category {}", counter,
                    ddSubjects.size(), ddSubject);
            int[][] map = createMapOfCommitsAndMigrationsForSubject(ddList, allReposFinishedProcessing, ddSubject,
                    maxRevSize, useOnlyRelevantCommits, allCrawledStatistics);
            String summaryResult = recategorizeMapOfCommitsAndMigrationsToSummary(map);
            summaryList.add(ddSubject + "," + summaryResult);
            counter++;
            LOG.info("    --- Result: {}", summaryResult);
        }

        return summaryList;
    }

    private int findMaxRevCountFromCrawledStatistics(Map<String, CrawledStatistic> allCrawledStatistics) {
        LOG.info("start calculating maximum revision size");
        int max = 0;
        for (Map.Entry<String, CrawledStatistic> entry : allCrawledStatistics.entrySet()) {
            if (entry.getValue().getNumberOfRevisions() != null && entry.getValue().getNumberOfRevisions() > max) {
                max = entry.getValue().getNumberOfRevisions();
            }
        }
        LOG.info("found maximum revision size: {}", max);
        return max;
    }

    private int[][] createMapOfCommitsAndMigrationsForSubject(List<ExtractedDesignDecisionEntity> ddList,
                                                              List<CrawledRepository> allReposFinishedProcessing,
                                                              String subjectToFilter, int maxRevSize, boolean useOnlyRelevantCommits,
                                                              Map<String, CrawledStatistic> crawledStatisticMap) {
        int[][] heatmap = new int[getMaxDDCount(ddList)+1][maxRevSize+1];
        for (CrawledRepository crawledRepo : allReposFinishedProcessing) {
            Set<String> uniqueMigrationDecision = findUniqueMigrationDecisionsForRepository(ddList, subjectToFilter, crawledRepo);
            if(uniqueMigrationDecision.size() > 0) {
                if(useOnlyRelevantCommits) {
                    heatmap[uniqueMigrationDecision.size()][crawledRepo.getRevisionCommitIdList().size()] += 1;
                } else {
                    CrawledStatistic crawledStatistic = crawledStatisticMap.get(crawledRepo.getRepositoryName() +
                            "-" + crawledRepo.getProjectName() + "-" + crawledRepo.getSource().name());
                    heatmap[uniqueMigrationDecision.size()][crawledStatistic.getNumberOfRevisions()] += 1;
                }
            }
        }
        return heatmap;
    }

    /**
     * Transform "commitsAndMigrationsMap" to analyzed summary string (comma-separated)
     * >> Divided in <=10 commits, 11-99 commits, >=100 commits
     * @param mapOfCommitsAndMigrations
     * @return
     */
    private String recategorizeMapOfCommitsAndMigrationsToSummary(int[][] mapOfCommitsAndMigrations) {
        LOG.info("  - analyze map of commits and migrations for {} to {}", 0, 10);
        int sum_0_10 = getSummaryInRange(mapOfCommitsAndMigrations, 0, 10);
        LOG.info("  - analyze map of commits and migrations for {} to {}", 11, 99);
        int sum_11_99 = getSummaryInRange(mapOfCommitsAndMigrations, 11, 99);
        LOG.info("  - analyze map of commits and migrations for {} to {}", 100, mapOfCommitsAndMigrations[0].length);
        int sum_100_max = getSummaryInRange(mapOfCommitsAndMigrations, 100, mapOfCommitsAndMigrations[0].length);

        return "sum_0_10," + sum_0_10 + ",sum_11_99," + sum_11_99 + ",sum_100_max," + sum_100_max;
    }

    private int calculateMaxRevisionSize(List<CrawledRepository> allReposFinishedProcessing) {
        LOG.info("start calculating maximum revision size");
        int max = 0;
        for (CrawledRepository cr : allReposFinishedProcessing) {
            if (cr.getRevisionCommitIdList().size() > max) {
                max = cr.getRevisionCommitIdList().size();
            }
        }
        LOG.info("found maximum revision size: {}", max);
        return max;
    }

}
