package edu.hm.ccwi.matilda.analyzer.rest;

import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaClient;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaSender;
import edu.hm.ccwi.matilda.analyzer.service.AnalyzeService;
import edu.hm.ccwi.matilda.analyzer.service.model.AnalyzedGeneralResults;
import edu.hm.ccwi.matilda.analyzer.service.stats.AnalyzeStatsService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/analyze")
public class AnalyzeResource {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeResource.class);

    final AnalyzeService analyzeService;
    final AnalyzeStatsService analyzeStatsService;
    final AnalyzeCommandKafkaClient analyzeCommandKafkaClient;
    final AnalyzeCommandKafkaSender analyzeCommandKafkaSender;

    public AnalyzeResource(AnalyzeService analyzeService, AnalyzeStatsService analyzeStatsService,
                           AnalyzeCommandKafkaClient analyzeCommandKafkaClient,
                           AnalyzeCommandKafkaSender analyzeCommandKafkaSender) {
        this.analyzeService = analyzeService;
        this.analyzeStatsService = analyzeStatsService;
        this.analyzeCommandKafkaClient = analyzeCommandKafkaClient;
        this.analyzeCommandKafkaSender = analyzeCommandKafkaSender;
    }

    @GetMapping(value = "/general")
    public AnalyzedGeneralResults analyzeGeneralStats() {
        return analyzeStatsService.analyzeGeneralStats();
    }

    @GetMapping(value = "/general/migrations-commits/heatmap", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> analyzeForHeatmapOfMigrationsAndCommits(@RequestParam(value = "decisionsubject") String decisionsubject) {
        LOG.info("Received request to analyze repositories and DDs for HeatmapOfMigrationsAndCommits - filtered by: {}", decisionsubject);
        if(StringUtils.isEmpty(decisionsubject) || StringUtils.isEmpty(decisionsubject.trim()) ||
                StringUtils.equals(decisionsubject, "null")) {
            decisionsubject = null;
        }
        return analyzeStatsService.analyzeHeatmapStatOfMigrationsAndRelevantCommits(decisionsubject);
    }

    /**
     * Analyzation-ready projects have the MatildaStatusCode 121 (= finished dataextraction)
     */
    @GetMapping(value = "/queue/projects")
    public ResponseEntity<List<String>> requeueAnalyzableProjects(@RequestParam(value = "statusCode") String statusCode) {
        List<String> allAnalyzableRepositoryIdsByState = analyzeService.findAndUpdateAllAnalyzableRepoIdsByState(statusCode);

        if (CollectionUtils.isNotEmpty(allAnalyzableRepositoryIdsByState)) {
            for (String analyzableRepositoryId : allAnalyzableRepositoryIdsByState) {
                if(StringUtils.isNotEmpty(analyzableRepositoryId)) {
                    analyzeCommandKafkaSender.analyzeRepositoryInMatildaProcess(analyzableRepositoryId);
                } else {
                    LOG.warn("analyzableRepositoryId is empty -> Skip and iterate further repositories");
                }
            }
        } else {
            LOG.warn("analyzableRepositoryIdsByStateList is empty -> Skip and iterate further repositories");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(allAnalyzableRepositoryIdsByState);
    }

    @GetMapping(value = "/project")
    public ResponseEntity analyzeProjectWithoutMatildaProcessQueues(@RequestParam(value = "crawledRepoId") String crawledRepoId) {
        if (crawledRepoId != null) {
            analyzeService.analyzeProject(crawledRepoId);
        } else {
            LOG.warn("ExtractedRepoId is null -> Skip, since it is not relevant or an exception occurred...");
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok().build();
    }
}