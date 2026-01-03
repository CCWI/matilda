package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectCommitAgeDesignDecisionMapAnalyzer extends AbstractProjectDesignDecisionAnalyzer {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectCommitSizeDesignDecisionAnalyzer.class);

    private ProjectAgeDesignDecisionAnalyzer projectAgeDesignDecisionAnalyzer;

    public ProjectCommitAgeDesignDecisionMapAnalyzer() {
        this.projectAgeDesignDecisionAnalyzer = new ProjectAgeDesignDecisionAnalyzer();
    }

    /**
     * Creates a heatmap (represented as a csv - comma separated strings in a list) for the amount of projects by
     * commits (in categories) and age (in categories).
     *
     * @param allReposFinishedProcessing
     * @param commitIdCommitDates
     * @param useOnlyRelevantCommits
     * @return
     */
    public List<String> createHeatmapForAmountOfProjectsByCommitCatAndAgeCat(List<CrawledRepository> allReposFinishedProcessing,
                                                                             List<CrawledRevision> commitIdCommitDates,
                                                                             boolean useOnlyRelevantCommits) {
        LOG.info("Start creating heatmap for overall amount of projects by commitCat and ageCat");
        List<String> keyValuePairsForHeatmapList = new ArrayList<>();
        Map<String, Integer> mapOfCommitsAndAge = createMapOfCommitsAndAge(allReposFinishedProcessing, commitIdCommitDates);

        for (Map.Entry<String, Integer> mapEntry : mapOfCommitsAndAge.entrySet()) {
            keyValuePairsForHeatmapList.add(mapEntry.getKey() + "|" + mapEntry.getValue());
        }

        return keyValuePairsForHeatmapList;
    }

    public List<String> createHeatmapForDesignDecisionsByProjectsCommitCatAndProjectsAgeCat(List<ExtractedDesignDecisionEntity> ddList,
                                                                                            List<CrawledRepository> allReposFinishedProcessing,
                                                                                            List<CrawledRevision> commitIdCommitDates,
                                                                                            boolean useOnlyRelevantCommits) {
        LOG.info("Start creating heatmap for overall design decisions of projects by commitCat and ageCat");
        List<String> keyValuePairsForHeatmapList = new ArrayList<>();
        Map<String, Integer> mapOfCommitsAndAge = createMapOfCommitsAndAge(ddList, allReposFinishedProcessing, commitIdCommitDates, useOnlyRelevantCommits);

        for (Map.Entry<String, Integer> mapEntry : mapOfCommitsAndAge.entrySet()) {
            keyValuePairsForHeatmapList.add(mapEntry.getKey() + "|" + mapEntry.getValue());
        }

        return keyValuePairsForHeatmapList;
    }

    private Map<String, Integer> createMapOfCommitsAndAge(List<ExtractedDesignDecisionEntity> ddList,
                                                          List<CrawledRepository> allReposFinishedProcessing,
                                                          List<CrawledRevision> commitIdCommitDates,
                                                          boolean useOnlyRelevantCommits) {
        Map<String, LocalDateTime> commitIdTimeMap = this.projectAgeDesignDecisionAnalyzer.transformCommitIdTimeListToMap(commitIdCommitDates);
        Map<String, Integer> repositoriesSortedInMap = createHeatmapMap();

        for (CrawledRepository crawledRepo : allReposFinishedProcessing) {
            int crawledRevisionSize = useOnlyRelevantCommits ? crawledRepo.getRevisionCommitIdList().size() : null;
            double projectAgeMonth = this.projectAgeDesignDecisionAnalyzer.calculateDifferenceInMinMaxProjectAge(crawledRepo, commitIdTimeMap);
            int amountUniqueMigDecForRepository = findUniqueMigrationDecisionsForRepository(ddList, crawledRepo).size();
            if(amountUniqueMigDecForRepository > 0) {
                String keyPart1 = null;
                if (crawledRevisionSize <= 10) {
                    keyPart1 = "10C";
                } else if (crawledRevisionSize <= 100) {
                    keyPart1 = "100C";
                } else {
                    keyPart1 = "MaxC";
                }
                if (keyPart1 != null) {
                    updateRepositoriesSortedInMapByRevSize(keyPart1, repositoriesSortedInMap, projectAgeMonth, amountUniqueMigDecForRepository);
                }
            }
        }

        return repositoriesSortedInMap;
    }

    private Map<String, Integer> createMapOfCommitsAndAge(List<CrawledRepository> allReposFinishedProcessing,
                                                          List<CrawledRevision> commitIdCommitDates) {
        Map<String, LocalDateTime> commitIdTimeMap = this.projectAgeDesignDecisionAnalyzer.transformCommitIdTimeListToMap(commitIdCommitDates);
        Map<String, Integer> repositoriesSortedInMap = createHeatmapMap();

        for (CrawledRepository crawledRepo : allReposFinishedProcessing) {
            int crawledRevisionSize = crawledRepo.getRevisionCommitIdList().size();
            double projectAgeMonth = this.projectAgeDesignDecisionAnalyzer.calculateDifferenceInMinMaxProjectAge(crawledRepo, commitIdTimeMap);

            String keyPart1 = null;
            if(crawledRevisionSize <= 10) {
                keyPart1 = "10C";
            } else if (crawledRevisionSize <= 100) {
                keyPart1 = "100C";
            } else {
                keyPart1 = "MaxC";
            }
            if(keyPart1 != null) {
                updateRepositoriesSortedInMapByRevSize(keyPart1, repositoriesSortedInMap, projectAgeMonth, 1);
            }
        }

        return repositoriesSortedInMap;
    }

    private Map<String, Integer> createHeatmapMap() {
        Map<String, Integer> repositoriesSortedInMap = new HashMap<>();
        repositoriesSortedInMap.put("10C-1M", 0);
        repositoriesSortedInMap.put("100C-1M", 0);
        repositoriesSortedInMap.put("MaxC-1M", 0);
        repositoriesSortedInMap.put("10C-12M", 0);
        repositoriesSortedInMap.put("100C-12M", 0);
        repositoriesSortedInMap.put("MaxC-12M", 0);
        repositoriesSortedInMap.put("10C-MaxM", 0);
        repositoriesSortedInMap.put("100C-MaxM", 0);
        repositoriesSortedInMap.put("MaxC-MaxM", 0);
        return repositoriesSortedInMap;
    }

    private void updateRepositoriesSortedInMapByRevSize(String keyPart1, Map<String, Integer> repositoriesSortedInMap,
                                                        double projectAgeMonth, int increment) {
        String keyPart2 = null;
        if(projectAgeMonth <= 1d) {
            keyPart2 = "1M";
        } else if (projectAgeMonth <= 12d) {
            keyPart2 = "12M";
        } else {
            keyPart2 = "MaxM";
        }

        if(keyPart2 != null) {
            Integer updatedCounter = repositoriesSortedInMap.get(keyPart1 + "-" + keyPart2) + increment;
            repositoriesSortedInMap.put(keyPart1 + "-" + keyPart2, updatedCounter);
        }
    }
}
