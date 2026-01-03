package edu.hm.ccwi.matilda.analyzer.service.stats;

import edu.hm.ccwi.matilda.analyzer.TestContext;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaClient;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaSender;
import edu.hm.ccwi.matilda.analyzer.service.AnalyzerStateHandler;
import edu.hm.ccwi.matilda.analyzer.service.decision.DesignDecisionExtractor;
import edu.hm.ccwi.matilda.analyzer.service.model.AnalyzedGeneralResults;
import edu.hm.ccwi.matilda.analyzer.utils.CsvUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

/**
 * NOTE: This is not a test class but a runner for statistical analyses on dissertation datasets.
 * Run manually when performing data analysis, not part of automated test suite.
 * Disabled by default to prevent accidental execution during CI/CD.
 */
@Disabled("Manual runner for statistical analysis - not an automated test")
@SpringBootTest(classes = {TestContext.class, AnalyzeStatsServiceImpl.class, DesignDecisionExtractor.class,
        AnalyzerStateHandler.class}, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("runner")
@ExtendWith(SpringExtension.class)
class AnalyzeStatsServiceImplRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeStatsServiceImplRunner.class);

    @Autowired
    AnalyzeStatsService analyzeStatsService;

    @MockBean
    AnalyzeCommandKafkaClient analyzeCommandKafkaClient;

    @MockBean
    AnalyzeCommandKafkaSender analyzeCommandKafkaSender;

    @Test
    public void analyzeGeneralStatsRunner() {
        AnalyzedGeneralResults results = analyzeStatsService.analyzeGeneralStats();
        LOG.info("::: Result: {}", results.toString());
    }


    @Test
    public void analyzeCategoriesOfMigrationsAndCommitsRunner_forRelevantCommitsOnly() {
        LOG.info("::: Results: ");
        for (String result : analyzeStatsService.analyzeCategoriesOfMigrationsAndCommits(true)) {
            LOG.info("      ::: Result: {}", result);
        }
    }

    @Test
    public void analyzeCategoriesOfMigrationsAndCommitsRunner_forAllCommits() {
        LOG.info("::: Results: ");
        for (String result : analyzeStatsService.analyzeCategoriesOfMigrationsAndCommits(false)) {
            LOG.info("      ::: Result: {}", result);
        }
    }

    @Test
    public void analyzeCategoriesOfMigrationsAndProjectAgeRunner() {
        LOG.info("::: Results: ");
        for (String result : analyzeStatsService.analyzeCategoriesOfMigrationsAndProjectAge()) {
            LOG.info("      ::: Result: {}", result);
        }
    }

    @Test
    public void analyzeProjectCommitAgeAmountOfProjectMapRunner() {
        LOG.info("::: Results: ");
        for (String result : analyzeStatsService.analyzeProjectCommitAgeAmountOfProjectMap(false)) {
            LOG.info("      ::: Result: {}", result);
        }
    }

    @Test
    public void analyzeProjectCommitAgeDesignDecisionMapRunner() {
        LOG.info("::: Results: ");
        for (String result : analyzeStatsService.analyzeProjectCommitAgeDesignDecisionMap(false)) {
            LOG.info("      ::: Result: {}", result);
        }
    }

    // ############################### START dissDataAnalysis ########################################
    /**
     * DONE already in 03.2022.
     */
    @Test
    public void dissDataAnalysis_exportSeveralRepositoryProjectData() {
        List<String> results = analyzeStatsService.retrieveDataListForRepositoryProjectsEDITION1();
        LOG.info("Amount of retrieved project data: {}; writing to csv....", results.size());
        for(String resultLine : results) {
            CsvUtils.appendToCsv("analysis-repoproject-dataset.csv", resultLine, false);
        }
        LOG.info("Finished processing");
    }

    /**
     * Done already on 07.06.2022
     */
    @Test
    public void dissDataAnalysis_exportOverallRevisionTimeStampList() {
        List<String> results = analyzeStatsService.retrieveOverallRevisionTimeStampList();
        LOG.info("Amount of retrieved results: {}; writing to csv....", results.size());
        for(String resultLine : results) {
            CsvUtils.appendToCsv("analysis-overallrevisiontimestamps-dataset.csv", resultLine, false);
        }
        LOG.info("Finished processing");
    }

    @Test
    public void dissDataAnalysis_exportOverallDecisionTimestempList() {
        List<String> results = analyzeStatsService.retrieveOverallMaximumAmountOfSWComponentsPerProjectList();
        LOG.info("Amount of retrieved results: {}; writing to csv....", results.size());
        for(String resultLine : results) {
            CsvUtils.appendToCsv("analysis-overallMaximumAmountOfSWComponentsPerProject-dataset.csv", resultLine, false);
        }
        LOG.info("Finished processing");
    }

    /**
     * Just for checking
     */
    @Test
    public void amountOfAllRelevantRepositoriesAvailableInMongoCrawledStatisticTest() {
        int i = analyzeStatsService.amountOfAllRelevantRepositoriesAvailableInMongoCrawledStatistic();
        System.out.println(i); // Result: Every relevant Repository should be available in crawledStatistic
    }
}