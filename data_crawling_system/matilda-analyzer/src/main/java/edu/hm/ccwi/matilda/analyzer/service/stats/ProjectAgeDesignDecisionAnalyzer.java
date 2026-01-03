package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class ProjectAgeDesignDecisionAnalyzer extends AbstractProjectDesignDecisionAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectAgeDesignDecisionAnalyzer.class);

    /**
     * Method is intended to deliver an analysis of each category and all in one about project age
     * (time between first and last commit) and migrations.
     *
     * @param ddList
     * @param allReposFinishedProcessing
     * @param commitIdCommitDates
     * @return
     */
    public List<String> calculateSummaryMigrationsAndProjectAgeOfEachCategory(List<ExtractedDesignDecisionEntity> ddList,
                                                                              List<CrawledRepository> allReposFinishedProcessing,
                                                                              List<CrawledRevision> commitIdCommitDates) {
        Map<String, LocalDateTime> commitIdTimeMap = transformCommitIdTimeListToMap(commitIdCommitDates);
        List<String> summaryList = new ArrayList<>();
        LOG.info("calculates summary of migrations and project age of each category");
        double maxProjectAgeInMonths = calculateDifferenceInMinMaxProjectAge(allReposFinishedProcessing, commitIdTimeMap);
        int counter = 1;
        Set<String> ddSubjects = retrieveAllSubjects(ddList);
        for (String ddSubject : ddSubjects) {
            LOG.info("  calculates summary of migrations and project age of each category: {}/{} category {}", counter,
                    ddSubjects.size(), ddSubject);
            int[][] map = createMapOfProjectAgeAndMigrationsForSubject(ddList, allReposFinishedProcessing, ddSubject, commitIdTimeMap, maxProjectAgeInMonths);
            String summaryResult = analyzeMapOfProjectAgeAndMigrations(map);
            summaryList.add(ddSubject + "," + summaryResult);
            counter++;
            LOG.info("    --- Result: {}", summaryResult);
        }

        return summaryList;
    }

    private int[][] createMapOfProjectAgeAndMigrationsForSubject(List<ExtractedDesignDecisionEntity> ddList,
                                                                 List<CrawledRepository> allReposFinishedProcessing,
                                                                 String subjectToFilter,
                                                                 Map<String, LocalDateTime> commitIdTimeMap,
                                                                 double maxProjectAgeInMonths) {
        int maxProjectAgeInMonthsInt = (int) Math.round(maxProjectAgeInMonths);
        int[][] heatmap = new int[getMaxDDCount(ddList)+1][maxProjectAgeInMonthsInt+1];
        for (CrawledRepository crawledRepo : allReposFinishedProcessing) {
            Set<String> uniqueMigrationDecision = findUniqueMigrationDecisionsForRepository(ddList, subjectToFilter, crawledRepo);
            double projectAgeMonth = calculateDifferenceInMinMaxProjectAge(crawledRepo, commitIdTimeMap);
            if(uniqueMigrationDecision.size() > 0) {
                int projectAgeMonthInt = (int) Math.round(projectAgeMonth);
                heatmap[uniqueMigrationDecision.size()][projectAgeMonthInt] += 1;
            }
        }
        return heatmap;
    }

    private String analyzeMapOfProjectAgeAndMigrations(int[][] mapOfProjectAgesAndMigrations) {
        LOG.info("  - analyze map of project age and migrations for {} to {}", 0, 1);
        int sum_0_1 = getSummaryInRange(mapOfProjectAgesAndMigrations, 0, 1); // bis zu 1,9999 Monate
        LOG.info("  - analyze map of project age and migrations for {} to {}", 2, 11); // zwischen 2 Monate und 12,9999 Monate
        int sum_2_12 = getSummaryInRange(mapOfProjectAgesAndMigrations, 2, 11); //zwischen 13 Monate und Max
        LOG.info("  - analyze map of project age and migrations for {} to {}", 12, mapOfProjectAgesAndMigrations[0].length);
        int sum_13_max = getSummaryInRange(mapOfProjectAgesAndMigrations, 12, mapOfProjectAgesAndMigrations[0].length);

        return "sum_0_1," + sum_0_1 + ",sum_2_12," + sum_2_12 + ",sum_13_max," + sum_13_max;
    }

    double calculateDifferenceInMinMaxProjectAge(CrawledRepository crawledRepository,
                                                 Map<String, LocalDateTime> commitIdTimeMap) {
        LOG.info("start calculating maximum project age");
        return calculateMaxDaysBetweenRepoRevisions(commitIdTimeMap, 0, crawledRepository) / 30;
    }

    double calculateDifferenceInMinMaxProjectAge(List<CrawledRepository> allReposFinishedProcessing,
                                                 Map<String, LocalDateTime> commitIdTimeMap) {
        LOG.info("start calculating maximum project age");
        int maxDays = 0;
        for (CrawledRepository crawledRepository : allReposFinishedProcessing) {
            maxDays = calculateMaxDaysBetweenRepoRevisions(commitIdTimeMap, maxDays, crawledRepository);
        }

        return maxDays / 30;
    }

    public LocalDateTime findDateOfCloneForRepository(CrawledRepository crawledRepository, Map<String, LocalDateTime> idLocalDateTimeMap) {
        if (crawledRepository.getRevisionCommitIdList().size() > 0) {
            // revisions of a project mostly have the same date of clone. Therefore, only one rev is considered.
            return idLocalDateTimeMap.get(crawledRepository.getRevisionCommitIdList().get(0));
        }

        return null;
    }

    int calculateMaxDaysBetweenRepoRevisions(Map<String, LocalDateTime> commitIdTimeMap, int maxDays, CrawledRepository crawledRepository) {
        LocalDateTime minDateTime = LocalDateTime.now();
        LocalDateTime maxDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
        if(crawledRepository.getRevisionCommitIdList().size() > 0) {
            if(containsRepositoryMultipleRevisionsWithCommitDate(crawledRepository, commitIdTimeMap)) {
                for (String revCommitId : crawledRepository.getRevisionCommitIdList()) {
                    LocalDateTime revCommitTime = commitIdTimeMap.get(revCommitId);
                    if(revCommitTime != null) {
                        if (minDateTime.isAfter(revCommitTime)) {
                            minDateTime = revCommitTime;
                        }
                        if (maxDateTime.isBefore(revCommitTime)) {
                            maxDateTime = revCommitTime;
                        }
                    }
                }
                Long daysBetween = ChronoUnit.DAYS.between(minDateTime, maxDateTime);
                if (maxDays < daysBetween.intValue()) {
                    maxDays = daysBetween.intValue();
                }
            }
        }
        return maxDays;
    }

    private boolean containsRepositoryMultipleRevisionsWithCommitDate(CrawledRepository crawledRepository, Map<String, LocalDateTime> commitIdTimeMap) {
        List<String> revisionCommitIdList = crawledRepository.getRevisionCommitIdList();
        int count = 0;

        for (String revCommitId : revisionCommitIdList) {
            LocalDateTime commitDateTime = commitIdTimeMap.get(revCommitId);
            if(commitDateTime != null && commitDateTime instanceof LocalDateTime) {
                count++;
                if(count > 1) {
                    return true;
                }
            }
        }
        return false;
    }

    Map<String, LocalDateTime> transformCommitIdTimeListToMap(List<CrawledRevision> commitIdCommitDates) {
        Map<String, LocalDateTime> commitIdTimeMap = new HashMap<>();
        for (CrawledRevision commitIdCommitDate : commitIdCommitDates) {
            commitIdTimeMap.put(commitIdCommitDate.getCommitId(), commitIdCommitDate.getCommitDate());
        }
        return commitIdTimeMap;
    }

    Map<String, LocalDateTime> transformCommitIdDateOfCloneListToMap(List<CrawledRevision> commitIdCommitDates) {
        Map<String, LocalDateTime> commitIdTimeMap = new HashMap<>();
        for (CrawledRevision commitIdCommitDate : commitIdCommitDates) {
            commitIdTimeMap.put(commitIdCommitDate.getCommitId(), commitIdCommitDate.getDateOfClone());
        }
        return commitIdTimeMap;
    }

}
