package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.analyzer.service.AnalyzerStateHandler;
import edu.hm.ccwi.matilda.analyzer.service.GenericAnalyzer;
import edu.hm.ccwi.matilda.analyzer.service.decision.DesignDecisionExtractor;
import edu.hm.ccwi.matilda.analyzer.service.model.AnalyzedGeneralResults;
import edu.hm.ccwi.matilda.analyzer.utils.CsvUtils;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledStatistic;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledStatisticRepository;
import edu.hm.ccwi.matilda.persistence.jpa.repo.ExtractedDesignDecisionRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AnalyzeStatsServiceImpl extends GenericAnalyzer implements AnalyzeStatsService {
    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeStatsServiceImpl.class);
    final CrawledSoftwareRepository mongoRepoRepo;
    final CrawledRevisionRepository mongoRevRepo;
    final CrawledStatisticRepository mongoCrawledStatistic;
    final ExtractedDesignDecisionRepository extractedDesignDecisionRepository;
    final DesignDecisionExtractor designDecisionExtractor;
    final AnalyzerStateHandler stateHandler;

    public AnalyzeStatsServiceImpl(CrawledSoftwareRepository mongoRepoRepo, CrawledRevisionRepository mongoRevRepo,
                                   CrawledStatisticRepository mongoCrawledStatistic,
                                   ExtractedDesignDecisionRepository extractedDesignDecisionRepository,
                                   DesignDecisionExtractor designDecisionExtractor, AnalyzerStateHandler stateHandler) {
        super(mongoRevRepo);
        this.mongoRepoRepo = mongoRepoRepo;
        this.mongoRevRepo = mongoRevRepo;
        this.mongoCrawledStatistic = mongoCrawledStatistic;
        this.extractedDesignDecisionRepository = extractedDesignDecisionRepository;
        this.designDecisionExtractor = designDecisionExtractor;
        this.stateHandler = stateHandler;
    }

    public AnalyzedGeneralResults analyzeGeneralStats() {
        LOG.info("General Analyses.... START - load Mongo-Repos");
        AnalyzedGeneralResults analyzedGeneralResults = new AnalyzedGeneralResults();

        List<CrawledRepository> allRepos = mongoRepoRepo.findAll();
        LOG.info("General Analyses.... Filter Repos");
        List<ProjectProfile> projectProfilesNoRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<ProjectProfile> projectProfilesRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<CrawledRepository> allReposFinishedProcessing = filterForAllFinishedRepos(allRepos, projectProfilesNoRecom, projectProfilesRecom);

        LOG.info("General Analyses.... Analyse basics");
        Set<String> repoNameSet = new HashSet<>();
        Set<String> projectNameSet = new HashSet<>();
        for (CrawledRepository crawledRepository : allReposFinishedProcessing) {
            repoNameSet.add(crawledRepository.getRepositoryName());
            projectNameSet.add(crawledRepository.getProjectName());
        }

        analyzedGeneralResults.setNumberAnalyzedRepositories(repoNameSet.size());
        analyzedGeneralResults.setNumberAnalyzedProjects(projectNameSet.size());

        LOG.info("General Analyses: Analyse commits per repository");
        calcNumberOfCommitsPerRepoProject(allReposFinishedProcessing, analyzedGeneralResults);
        LOG.info("General Analyses: Analyse mean project age");
        calcAgeOfAllCrawledRepositories(allReposFinishedProcessing, analyzedGeneralResults);
        LOG.info("General Analyses: Analyse dependecies per project");
        calcAmountOfDependenciesOverAllCrawledRepositories(allReposFinishedProcessing, analyzedGeneralResults);
        LOG.info("General Analyses.... FINISHED");

        return analyzedGeneralResults;
    }

    public List<String> analyzeHeatmapStatOfMigrationsAndRelevantCommits(String decisionSubject) {
        LOG.info("Load repositories from MongoDB");
        List<CrawledRepository> allRepos = mongoRepoRepo.findAll();

        LOG.info("Started Heatmap Analyses.... Filter Repos");
        List<ProjectProfile> projectProfilesNoRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<ProjectProfile> projectProfilesRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<CrawledRepository> allReposFinishedProcessing = filterForAllFinishedRepos(allRepos, projectProfilesNoRecom, projectProfilesRecom);

        LOG.info("Generate Heatmaps on Design Decisions for decision subject {}", decisionSubject);
        List<ExtractedDesignDecisionEntity> ddList = decisionSubject != null ?
                extractedDesignDecisionRepository.findByDecisionSubject(decisionSubject) : extractedDesignDecisionRepository.findAll();
        return new ProjectCommitSizeDesignDecisionAnalyzer().calculateHeatmapForAmountOfMigrationsByRelevantCommits(ddList, allReposFinishedProcessing,
                decisionSubject);
    }

    @Override
    public List<String> analyzeCategoriesOfMigrationsAndCommits(boolean useOnlyRelevantCommits) {
        Map<String, CrawledStatistic> crawledStatisticIdMap = null;
        if (!useOnlyRelevantCommits) {
            crawledStatisticIdMap = createCrawledStatisticIdMap();
        }
        return new ProjectCommitSizeDesignDecisionAnalyzer().calculateSummaryMigrationsAndCommitsOfEachCategory(
                extractedDesignDecisionRepository.findAll(), retrieveAllReposFinishedProcessing(), crawledStatisticIdMap,
                useOnlyRelevantCommits);
    }

    private Map<String, CrawledStatistic> createCrawledStatisticIdMap() {
        Map<String, CrawledStatistic> crawledStatisticIdMap;
        crawledStatisticIdMap = new HashMap<>();
        for (CrawledStatistic crawledStatistic : mongoCrawledStatistic.findAll()) {
            crawledStatisticIdMap.put(crawledStatistic.getStatisticId(), crawledStatistic);
        }
        return crawledStatisticIdMap;
    }

    @Override
    public List<String> analyzeCategoriesOfMigrationsAndProjectAge() {
        return new ProjectAgeDesignDecisionAnalyzer().calculateSummaryMigrationsAndProjectAgeOfEachCategory(
                extractedDesignDecisionRepository.findAll(), retrieveAllReposFinishedProcessing(), mongoRevRepo.findAllByCommitId());
    }

    @Override
    public List<String> analyzeProjectCommitAgeAmountOfProjectMap(boolean useOnlyRelevantCommits) {
        return new ProjectCommitAgeDesignDecisionMapAnalyzer().createHeatmapForAmountOfProjectsByCommitCatAndAgeCat(
                retrieveAllReposFinishedProcessing(), mongoRevRepo.findAllByCommitId(), useOnlyRelevantCommits);
    }

    @Override
    public List<String> analyzeProjectCommitAgeDesignDecisionMap(boolean useOnlyRelevantCommits) {
        List<ExtractedDesignDecisionEntity> ddList = extractedDesignDecisionRepository.findAll();
        return new ProjectCommitAgeDesignDecisionMapAnalyzer().createHeatmapForDesignDecisionsByProjectsCommitCatAndProjectsAgeCat(
                ddList, retrieveAllReposFinishedProcessing(), mongoRevRepo.findAllByCommitId(), useOnlyRelevantCommits);
    }

    @Override
    public int amountOfAllRelevantRepositoriesAvailableInMongoCrawledStatistic() {
        List<CrawledRepository> allReposFinishedProcessing = retrieveAllReposFinishedProcessing();
        List<CrawledStatistic> allCrawledStatistics = mongoCrawledStatistic.findAll();
        List<String> allCrawledStatisticIds = new ArrayList<>();
        List<String> availableCrawledRepoInStatisticsList = new ArrayList<>();
        Set<String> allReposFinishedProcessingId = new HashSet<>();
        for (CrawledRepository crawledRepository : allReposFinishedProcessing) {
            allReposFinishedProcessingId.add(crawledRepository.getRepositoryName() + "-" + crawledRepository.getProjectName()
                    + "-" + crawledRepository.getSource().name());
        }

        LOG.info("Found size of repofinishedProcessing: {} and crawledstatistic(mongo): {}", allReposFinishedProcessing.size(),
                allCrawledStatistics.size());

        for (CrawledStatistic allCrawledStatistic : allCrawledStatistics) {
            allCrawledStatisticIds.add(allCrawledStatistic.getStatisticId());
        }

        int counter = 0;
        for (String crawledRepositoryId : allReposFinishedProcessingId) {
            if (allCrawledStatisticIds.contains(crawledRepositoryId)) {
                availableCrawledRepoInStatisticsList.add(crawledRepositoryId);
            } else {
                counter++;
            }
        }

        return counter;
    }

    private List<CrawledRepository> retrieveAllReposFinishedProcessing() {
        LOG.info("Started Heatmap Analyses.... Filter Repos");
        List<ProjectProfile> projectProfilesNoRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<ProjectProfile> projectProfilesRecom = stateHandler.getProjectProfilesByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));
        List<CrawledRepository> allReposFinishedProcessing = filterForAllFinishedRepos(mongoRepoRepo.findAll(), projectProfilesNoRecom, projectProfilesRecom);
        return allReposFinishedProcessing;
    }

    private List<CrawledRepository> filterForAllFinishedRepos(List<CrawledRepository> allRepos, List<ProjectProfile> projectProfilesNoRecom, List<ProjectProfile> projectProfilesRecom) {
        List<CrawledRepository> allReposFinishedProcessing = new ArrayList<>();
        for (CrawledRepository repo : allRepos) {
            if (isRepoAlreadyCrawledCompletely(repo, projectProfilesNoRecom, projectProfilesRecom)) {
                allReposFinishedProcessing.add(repo);
            }
        }
        return allReposFinishedProcessing;
    }

    private AnalyzedGeneralResults calcAgeOfAllCrawledRepositories(List<CrawledRepository> allReposFinishedProcessing,
                                                                   AnalyzedGeneralResults analyzedGeneralResults) {
        List<Long> repositoryDurationsByDay = new ArrayList<>();
        for (CrawledRepository crawledRepository : allReposFinishedProcessing) {
            if (crawledRepository != null && CollectionUtils.isNotEmpty(crawledRepository.getRevisionCommitIdList())) {
                LocalDateTime firstCommitDate = null;
                LocalDateTime lastCommitDate = null;
                for (String commitId : crawledRepository.getRevisionCommitIdList()) {
                    CrawledRevision crawledRev = mongoRevRepo.getCrawledRevById(commitId);

                    // calc average amount of dependencies per project in one revision -> FIND MAX
                    if (crawledRev != null && CollectionUtils.isNotEmpty(crawledRev.getProjectList())) {
                        LocalDateTime commitDate = crawledRev.getCommitDate();
                        if (firstCommitDate == null && lastCommitDate == null) {
                            firstCommitDate = commitDate;
                            lastCommitDate = commitDate;
                        } else if (firstCommitDate != null && commitDate.isBefore(firstCommitDate)) {
                            firstCommitDate = commitDate;
                        } else if (lastCommitDate != null && commitDate.isAfter(lastCommitDate)) {
                            lastCommitDate = commitDate;
                        }
                    }
                }

                if (firstCommitDate != null && lastCommitDate != null) {
                    long daysbetween = ChronoUnit.DAYS.between(firstCommitDate, lastCommitDate);
                    repositoryDurationsByDay.add(daysbetween);
                }
            }
        }

        DescriptiveStatistics statistics = new DescriptiveStatistics();
        for (Long durationDays : repositoryDurationsByDay) {
            statistics.addValue(durationDays.doubleValue());
        }

        LOG.info("Calculated Results of MeanProjectAge: {}", statistics.getMean());
        LOG.info("Calculated Results of StdOfMeanProjectAge: {}", statistics.getStandardDeviation());
        LOG.info("Calculated Results of MedianProjectAge: {}", statistics.getPercentile(50));
        LOG.info("Calculated Results of MedianProjectAge- Median: {}", statistics.getPercentile(50));
        LOG.info("Calculated Results of MedianProjectAge- Perc-60: {}", statistics.getPercentile(60));
        LOG.info("Calculated Results of MedianProjectAge- Perc-90: {}", statistics.getPercentile(90));
        LOG.info("Calculated Results of MedianProjectAge- Perc-95: {}", statistics.getPercentile(95));
        LOG.info("Calculated Results of MedianProjectAge- Perc-99: {}", statistics.getPercentile(99));
        LOG.info("Calculated Results of MaxProjectAge: {}", statistics.getMax());

        analyzedGeneralResults.setMeanDaysOfProjectDurationByCommit(statistics.getMean());
        analyzedGeneralResults.setStdDaysOfProjectDurationByCommit(statistics.getStandardDeviation());
        analyzedGeneralResults.setMedianDaysOfProjectDurationByCommit(statistics.getPercentile(50));
        analyzedGeneralResults.setPercentile60DaysOfProjectDurationByCommit(statistics.getPercentile(60));
        analyzedGeneralResults.setPercentile90DaysOfProjectDurationByCommit(statistics.getPercentile(90));
        analyzedGeneralResults.setPercentile95DaysOfProjectDurationByCommit(statistics.getPercentile(95));
        analyzedGeneralResults.setPercentile99DaysOfProjectDurationByCommit(statistics.getPercentile(99));
        analyzedGeneralResults.setMaxDaysOfProjectDurationByCommit(statistics.getMax());

        return analyzedGeneralResults;
    }

    private boolean isRepoAlreadyCrawledCompletely(CrawledRepository repo, List<ProjectProfile> projectProfilesNoRecom,
                                                   List<ProjectProfile> projectProfilesRecom) {
        return projectProfilesNoRecom.stream().anyMatch(projectProfile -> StringUtils.equals(repo.getProjectName(), projectProfile.getProjectName()) &&
                StringUtils.equals(repo.getRepositoryName(), projectProfile.getRepositoryName())) ||
                projectProfilesRecom.stream().anyMatch(projectProfile -> StringUtils.equals(repo.getProjectName(), projectProfile.getProjectName()) &&
                        StringUtils.equals(repo.getRepositoryName(), projectProfile.getRepositoryName()));
    }

    private AnalyzedGeneralResults calcNumberOfCommitsPerRepoProject(List<CrawledRepository> allReposFinishedProcessing,
                                                                     AnalyzedGeneralResults analyzedGeneralResults) {
        // calc relevant commit count
        DescriptiveStatistics statisticsRel = new DescriptiveStatistics();
        double[] numberOfRelevantCommitsArray = allReposFinishedProcessing.stream().map(crawledRepository ->
                Double.valueOf(crawledRepository.getRevisionCommitIdList().size())).mapToDouble(Double::doubleValue).toArray();
        int totalRelevantCommitCounter = 0;
        for (double numberOfCommits : numberOfRelevantCommitsArray) {
            statisticsRel.addValue(numberOfCommits);
            totalRelevantCommitCounter += numberOfCommits;
        }

        // calc total commit count
        DescriptiveStatistics statisticsTotal = new DescriptiveStatistics();
        List<CrawledStatistic> allCrawledStatistics = mongoCrawledStatistic.findAll();
        List<CrawledStatistic> filteredCrawledStatistics = new ArrayList<>();
        Set<String> allReposFinishedProcessingId = new HashSet<>();
        for (CrawledRepository crawledRepository : allReposFinishedProcessing) {
            allReposFinishedProcessingId.add(crawledRepository.getRepositoryName() + "-" + crawledRepository.getProjectName()
                    + "-" + crawledRepository.getSource().name());
        }

        for (CrawledStatistic crawledStatistic : allCrawledStatistics) {
            if (allReposFinishedProcessingId.contains(crawledStatistic.getStatisticId())) {
                filteredCrawledStatistics.add(crawledStatistic);
            }
        }

        LOG.info("...filtered crawledstatistics: {} of {} are used for analyzation", filteredCrawledStatistics.size(), allCrawledStatistics.size());

        double[] numberOfCommitsArray = filteredCrawledStatistics.stream().map(crawledStatistic ->
                Double.valueOf(crawledStatistic.getNumberOfRevisions())).mapToDouble(Double::doubleValue).toArray();
        int totalCommitCounter = 0;
        for (double numberOfCommits : numberOfCommitsArray) {
            statisticsTotal.addValue(numberOfCommits);
            totalCommitCounter += numberOfCommits;
        }

        logNumberOfCommitResults(statisticsTotal, "NumberOfTotalCommitsPerRepository");
        logNumberOfCommitResults(statisticsRel, "NumberOfRelevantCommitsPerRepository");

        analyzedGeneralResults.setTotalNumberOfCommits(totalCommitCounter);
        Map<String, Double> additionalStatsForTotalNumberOfCommits = new HashMap<>();
        additionalStatsForTotalNumberOfCommits.put("mean", statisticsTotal.getMean());
        additionalStatsForTotalNumberOfCommits.put("mean_std", statisticsTotal.getStandardDeviation());
        additionalStatsForTotalNumberOfCommits.put("mean_variance", statisticsTotal.getPopulationVariance());
        additionalStatsForTotalNumberOfCommits.put("percentile_25", statisticsTotal.getPercentile(25));
        additionalStatsForTotalNumberOfCommits.put("percentile_50", statisticsTotal.getPercentile(50));
        additionalStatsForTotalNumberOfCommits.put("percentile_60", statisticsTotal.getPercentile(60));
        additionalStatsForTotalNumberOfCommits.put("percentile_75", statisticsTotal.getPercentile(75));
        additionalStatsForTotalNumberOfCommits.put("percentile_90", statisticsTotal.getPercentile(90));
        additionalStatsForTotalNumberOfCommits.put("percentile_95", statisticsTotal.getPercentile(95));
        additionalStatsForTotalNumberOfCommits.put("percentile_99", statisticsTotal.getPercentile(99));
        additionalStatsForTotalNumberOfCommits.put("max", statisticsTotal.getMax());
        analyzedGeneralResults.setAdditionsToTotalNumberOfCommits(additionalStatsForTotalNumberOfCommits);

        analyzedGeneralResults.setTotalNumberOfRelevantCommits(totalRelevantCommitCounter);
        Map<String, Double> additionalStatsForTotalNumberOfRelevantCommits = new HashMap<>();
        additionalStatsForTotalNumberOfRelevantCommits.put("mean", statisticsRel.getMean());
        additionalStatsForTotalNumberOfRelevantCommits.put("mean_std", statisticsRel.getStandardDeviation());
        additionalStatsForTotalNumberOfRelevantCommits.put("mean_variance", statisticsRel.getPopulationVariance());
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_25", statisticsRel.getPercentile(25));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_50", statisticsRel.getPercentile(50));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_60", statisticsRel.getPercentile(60));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_75", statisticsRel.getPercentile(75));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_90", statisticsRel.getPercentile(90));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_95", statisticsRel.getPercentile(95));
        additionalStatsForTotalNumberOfRelevantCommits.put("percentile_99", statisticsRel.getPercentile(99));
        additionalStatsForTotalNumberOfRelevantCommits.put("max", statisticsRel.getMax());
        analyzedGeneralResults.setAdditionsToTotalNumberOfRelevantCommits(additionalStatsForTotalNumberOfRelevantCommits);
        return analyzedGeneralResults;
    }

    private void logNumberOfCommitResults(DescriptiveStatistics statistics, String subject) {
        LOG.info("Calculated Results of {} - Mean: {} :: Std: {}", subject, statistics.getMean(),
                Math.sqrt(statistics.getStandardDeviation()));
        LOG.info("Calculated Results of {} - Median: {}", subject, statistics.getPercentile(50));
        LOG.info("Calculated Results of {} - Perc-60: {}", subject, statistics.getPercentile(60));
        LOG.info("Calculated Results of {} - Perc-90: {}", subject, statistics.getPercentile(90));
        LOG.info("Calculated Results of {} - Perc-95: {}", subject, statistics.getPercentile(95));
        LOG.info("Calculated Results of {} - Perc-99: {}", subject, statistics.getPercentile(99));
        LOG.info("Calculated Results of {} - Max: {}", subject, statistics.getMax());
    }

    private AnalyzedGeneralResults calcAmountOfDependenciesOverAllCrawledRepositories(List<CrawledRepository> allRepos,
                                                                                      AnalyzedGeneralResults analyzedGeneralResults) {
        DescriptiveStatistics statistics = new DescriptiveStatistics();
        for (CrawledRepository crawledRepository : allRepos) {
            try {
                double dependencyAmount = calcAverageAmountOfDependenciesPerProjectOfCommitWithMaxAmountOfDependenciesInCrawledRepositoryProject(crawledRepository);
                statistics.addValue(dependencyAmount);
            } catch (EmptyStackException e) {
                LOG.warn("Found no dependencies in repository: {}:{}", crawledRepository.getRepositoryName(), crawledRepository.getProjectName());
            }
        }

        analyzedGeneralResults.setMeanAmountOfDependenciesPerProjectInRepoProject(statistics.getMean());
        Map<String, Double> additionsToAmountOfDependenciesPerProjectInRepoProject = new HashMap<>();
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("mean", statistics.getMean());
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("mean_std", statistics.getStandardDeviation());
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("mean_variance", statistics.getPopulationVariance());
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_25", statistics.getPercentile(25));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_50", statistics.getPercentile(50));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_60", statistics.getPercentile(60));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_75", statistics.getPercentile(75));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_90", statistics.getPercentile(90));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_95", statistics.getPercentile(95));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("percentile_99", statistics.getPercentile(99));
        additionsToAmountOfDependenciesPerProjectInRepoProject.put("max", statistics.getMax());
        analyzedGeneralResults.setAdditionsToTotalNumberOfRelevantCommits(additionsToAmountOfDependenciesPerProjectInRepoProject);

        return analyzedGeneralResults;
    }

    /**
     * Retrieve an average amount of dependencies over all projects (pom.xml-files) in a repository,
     * each by the commit with the maximum amount of dependencies.
     *
     * @throws EmptyStackException
     */
    double calcAverageAmountOfDependenciesPerProjectOfCommitWithMaxAmountOfDependenciesInCrawledRepositoryProject(
            CrawledRepository crawledRepository) throws EmptyStackException {
        Map<String, List<Integer>> projectDependenyAmountMap = new HashMap<>();
        // calc average amount of crawledRepository
        if (crawledRepository != null && CollectionUtils.isNotEmpty(crawledRepository.getRevisionCommitIdList())) {
            for (String commitId : crawledRepository.getRevisionCommitIdList()) {
                CrawledRevision crawledRev = mongoRevRepo.getCrawledRevById(commitId);

                // calc average amount of dependencies per project in one revision -> FIND MAX
                if (crawledRev != null && CollectionUtils.isNotEmpty(crawledRev.getProjectList())) {
                    for (CrawledProject crawledProject : crawledRev.getProjectList()) {
                        String id = crawledProject.getProjectGroup() + crawledProject.getProjectArtifact();
                        List<Integer> list = projectDependenyAmountMap.containsKey(id) ? projectDependenyAmountMap.get(id) : new ArrayList<>();
                        list.add(crawledProject.getDependencyList().size());
                        projectDependenyAmountMap.put(id, list);
                    }
                }
            }
        }

        if (MapUtils.isEmpty(projectDependenyAmountMap)) {
            throw new EmptyStackException();
        }

        List<Double> maximumList = new ArrayList<>();
        for (Map.Entry<String, List<Integer>> mapEntry : projectDependenyAmountMap.entrySet()) {
            OptionalDouble max = mapEntry.getValue().stream().mapToDouble(Integer::intValue).max();
            maximumList.add(max.orElse(0));
        }

        return StatUtils.mean(maximumList.stream().mapToDouble(Double::doubleValue).toArray());
    }

    public List<String> retrieveDataListForRepositoryProjectsEDITION1() {
        ProjectAgeDesignDecisionAnalyzer projectAgeDesignDecisionAnalyzer = new ProjectAgeDesignDecisionAnalyzer();

        LOG.info("Start loading data from database");
        List<CrawledRepository> allReposFinishedProcessing = retrieveAllReposFinishedProcessing();
        List<CrawledRevision> crawledRevisions = mongoRevRepo.findAllByCommitId();
        Map<String, CrawledStatistic> crawledStatisticsMap = createCrawledStatisticIdMap();
        Map<String, String> repoProjDateOfCloneMap = new HashMap<>();
        List<String> datasetEdition1 = new ArrayList<>();
        Map<String, LocalDateTime> commitIdTimeMap = projectAgeDesignDecisionAnalyzer.transformCommitIdTimeListToMap(crawledRevisions);

        LOG.info("Start loading dateOfClone-data from csv and transforming");
        String[] dateOfCloneRowArray = CsvUtils.readCsv(AnalyzeStatsServiceImpl.class, "extracted-date-of-clone-output.txt");
        for (String dateOfCloneRow : dateOfCloneRowArray) {
            String[] dateOfCloneSplits = dateOfCloneRow.split("\\|");
            if (dateOfCloneSplits.length == 2) {
                String dateOfCloneDirectoryPath = dateOfCloneSplits[0].strip();
                String[] splitDOCDirPath = dateOfCloneDirectoryPath.split("/");
                String repoName = splitDOCDirPath[splitDOCDirPath.length - 3];
                String projectName = splitDOCDirPath[splitDOCDirPath.length - 2];
                String dateOfClone = dateOfCloneSplits[1].strip().substring(0, 10).strip();
                repoProjDateOfCloneMap.put(repoName.toLowerCase() + ":" + projectName.toLowerCase(), dateOfClone);
            }
        }

        for (int i = 0; i < allReposFinishedProcessing.size(); i++) {
            CrawledRepository crawledRepo = allReposFinishedProcessing.get(i);
            double projectAgeInDays = projectAgeDesignDecisionAnalyzer.calculateMaxDaysBetweenRepoRevisions(commitIdTimeMap, 0, crawledRepo);
            double amountOfDependencies = calcAverageAmountOfDependenciesPerProjectOfCommitWithMaxAmountOfDependenciesInCrawledRepositoryProject(crawledRepo);
            // durchschnittliche Anzahl an Abhängigkeiten pro Revision in einem Softwareprojekt
            // Retrieve an average amount of dependencies over all projects in a softwareproject each by the commit with the maximum amount of dependencies.

            CrawledStatistic crawledStatistic = crawledStatisticsMap.get(crawledRepo.getRepositoryName() +
                    "-" + crawledRepo.getProjectName() + "-" + crawledRepo.getSource().name());

            String dateOfClone = repoProjDateOfCloneMap.get(crawledRepo.getRepositoryName().toLowerCase() + ":"
                    + crawledRepo.getProjectName().toLowerCase());

            String msg = crawledRepo.getRepositoryName()
                    + ", " + crawledRepo.getProjectName()
                    + ", " + projectAgeInDays
                    + ", " + crawledStatistic.getNumberOfRevisions()
                    + ", " + crawledStatistic.getNumberOfBranches()
                    + ", " + amountOfDependencies
                    + ", " + dateOfClone;
            datasetEdition1.add(msg);

            LOG.info("Progress: {}/{} -> {}", i, allReposFinishedProcessing.size(), msg);
        }

        return datasetEdition1;
    }

    public List<String> retrieveOverallRevisionTimeStampList() {
        LOG.info("Start loading data from database");
        List<CrawledRevision> uniqueCrawledRevisions = mongoRevRepo.findAllByCommitId();
        List<String> commitDatesList = new ArrayList<>();

        LOG.info("Start loading dateOfRevision-data from csv and transforming");

        for (CrawledRevision uniqueCrawledRevision : uniqueCrawledRevisions) {
            String commitDate = uniqueCrawledRevision.getCommitDate().toString();


            String msg = uniqueCrawledRevision.getCommitId()
                    + ", " + commitDate;
            commitDatesList.add(msg);
        }

        return commitDatesList;
    }

    public List<String> retrieveOverallMaximumAmountOfSWComponentsPerProjectList() {
        LOG.info("Start loading data from database");
        List<CrawledRepository> allReposFinishedProcessing = retrieveAllReposFinishedProcessing();
        List<String> maxAmountOfSWComponentsPerProjectList = new ArrayList<>();

        for (int i = 0; i < allReposFinishedProcessing.size(); i++) {
            CrawledRepository crawledRepo = allReposFinishedProcessing.get(i);

            Map<String, List<Integer>> swComponentsPerProjectAmountMap = new HashMap<>();
            // calc average amount of crawledRepository
            if (crawledRepo != null && CollectionUtils.isNotEmpty(crawledRepo.getRevisionCommitIdList())) {
                for (String commitId : crawledRepo.getRevisionCommitIdList()) {
                    CrawledRevision crawledRev = mongoRevRepo.getCrawledRevById(commitId);

                    // find amount of components per project per revision
                    if (crawledRev != null && CollectionUtils.isNotEmpty(crawledRev.getProjectList())) {
                        for (CrawledProject crawledProject : crawledRev.getProjectList()) {
                            String id = crawledRepo.getRepositoryName() + ":" + crawledRepo.getProjectName(); //crawledProject.getProjectGroup() + crawledProject.getProjectArtifact();
                            List<Integer> list = swComponentsPerProjectAmountMap.containsKey(id) ? swComponentsPerProjectAmountMap.get(id) : new ArrayList<>();
                            list.add(crawledRev.getProjectList().size());
                            swComponentsPerProjectAmountMap.put(id, list);
                        }
                    }
                }
            }

            if (MapUtils.isEmpty(swComponentsPerProjectAmountMap)) {
                throw new EmptyStackException();
            }

            for (Map.Entry<String, List<Integer>> mapEntry : swComponentsPerProjectAmountMap.entrySet()) {
                OptionalDouble max = mapEntry.getValue().stream().mapToDouble(Integer::intValue).max();
                String msg = crawledRepo.getRepositoryName()
                        + ", " + crawledRepo.getProjectName()
                        + ", " + max.orElse(0);
                maxAmountOfSWComponentsPerProjectList.add(msg);
            }
        }

        return maxAmountOfSWComponentsPerProjectList;
    }
}