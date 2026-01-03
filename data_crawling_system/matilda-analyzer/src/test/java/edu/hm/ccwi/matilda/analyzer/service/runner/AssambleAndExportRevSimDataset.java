package edu.hm.ccwi.matilda.analyzer.service.runner;

import edu.hm.ccwi.matilda.analyzer.MatildaAnalyzerApplication;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaClient;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaSender;
import edu.hm.ccwi.matilda.analyzer.utils.CsvUtils;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.persistence.mongo.model.*;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.*;

/**
 * NO TEST!
 * CoSim/RevSim:
 * This Class creates a dataset about docs and used libraries as csv in the target-folder of all revisions of repos/projects.
 */
@SpringBootTest(classes = MatildaAnalyzerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class AssambleAndExportRevSimDataset {

    private static final Logger LOG = LoggerFactory.getLogger(AssambleAndExportRevSimDataset.class);

    protected static final String GATEWAY_PROJECT_PROFILE_LIST_URL = "http://localhost:8081/gw/state/project/profiles";
    private static final String STATE_OF_FINISHED_CRAWLING = "142";

    @Inject
    CrawledSoftwareRepository mongoRepoRepo;

    @Inject
    CrawledRevisionRepository mongoRevRepo;

    @Inject
    CrawledDocumentationRepository mongoDocRepository;

    @Inject
    RestTemplate restTemplate;

    @MockBean
    AnalyzeCommandKafkaClient analyzeCommandKafkaClient;

    @MockBean
    AnalyzeCommandKafkaSender analyzeCommandKafkaSender;

    @Test
    void cleanupNotRelevantMongoRepositoriesAndRevisions() {
        //CsvUtils.createOrOverwriteNewCsv("revsim-dataset-v1.csv");
        Set<Integer> datasetDistinctHashSet = new HashSet<>();
        List<String> processedRepoProjects = findProcessedProjects();
        LOG.info("Found {} processed Projects in Status {}", processedRepoProjects.size(), STATE_OF_FINISHED_CRAWLING);

        Set<String> brokenProjects = new HashSet<>();
        Set<String> usedRevisions = new HashSet<>();
        int noRevAndDocCounter = 0;
        int noDocCounter = 0;
        int noRevCounter = 0;
        int stateCounter = 0;
        for (String processedRepoProject : processedRepoProjects) {
            LOG.info("cleanup-state: {}/{}", stateCounter++, processedRepoProjects.size());
            CrawledRepository crawledMongoRepository = mongoRepoRepo.getCrawledRepositoryById(processedRepoProject);
            for (String revision : crawledMongoRepository.getRevisionCommitIdList()) {
                String revLine = null;
                Optional<CrawledRevision> rev = mongoRevRepo.findById(revision);
                Optional<CrawledDocumentation> doc = mongoDocRepository.findById(revision);

                if(rev.isEmpty() && doc.isEmpty()) {
                    noRevAndDocCounter++;
                    //LOG.warn("{} -> For Revision {} is NO REV AND DOC AVAILABLE! (Found {} of those dead bodies)", processedRepoProject, revision, noRevAndDocCounter);
                    brokenProjects.add(processedRepoProject);
                } else if(rev.isPresent() && doc.isEmpty()) {
                    noDocCounter++;
                    //LOG.warn("{} -> For Revision {} is  NO DOC AVAILABLE! (Found {} of those dead bodies)", processedRepoProject, revision, noDocCounter);
                    brokenProjects.add(processedRepoProject);
                } else if(rev.isEmpty() && doc.isPresent()) {
                    noRevCounter++;
                    //LOG.warn("{} -> For Revision {} is NO REV AVAILABLE! (Found {} of those dead bodies)", processedRepoProject, revision, noRevCounter);
                    brokenProjects.add(processedRepoProject);
                } else {
                    usedRevisions.add(revision);
                    List<String> documentationFileList = doc.get().getDocumentationFileList();
                    StringJoiner docFileStringJoiner = new StringJoiner("|");
                    if(CollectionUtils.isNotEmpty(documentationFileList)) {
                        for (String docFile : documentationFileList) {
                            docFile = docFile.replace(";", ".")
                                    .replace("  ", " ").trim();
                            docFile = docFile.replaceAll("\\R", " ").replaceAll("\\r|\\n", " ").trim();
                            docFile = docFile.replace("\n", " ").replace("\r", " ");
                            docFile = html2text(docFile);
                            docFileStringJoiner.add(docFile);
                        }
                    }

                    StringJoiner revDependencyStringJoiner = new StringJoiner("|");
                    if(CollectionUtils.isNotEmpty(rev.get().getProjectList())) {
                        for (CrawledProject crawledProject : rev.get().getProjectList()) {
                            if (CollectionUtils.isNotEmpty(crawledProject.getDependencyList())) {
                                for (CrawledDependency crawledDependency : crawledProject.getDependencyList()) {
                                    if (crawledDependency.isRelevant()) {
                                        revDependencyStringJoiner.add(crawledDependency.toGAString());
                                    }
                                }
                            }
                        }
                    }
                    if(StringUtils.isNotEmpty(docFileStringJoiner.toString()) && StringUtils.isNotEmpty(revDependencyStringJoiner.toString())) {
                        revLine = processedRepoProject + "; " + docFileStringJoiner + "; " + revDependencyStringJoiner;
                    }
                }

                if(revLine != null) {
                    int hashedRevLine = hashCode(revLine);
                    if(!datasetDistinctHashSet.contains(hashedRevLine)) {
                        datasetDistinctHashSet.add(hashedRevLine);
                        CsvUtils.appendToCsv("revsim-dataset-v1.csv", revLine, false);
                    }
                }
            }
        }

        LOG.info("#################################################");
        LOG.info("################## SUMMARY ######################");
        LOG.info("#################################################");
        LOG.info("Amount of broken Projects: {}/{}", brokenProjects.size(), processedRepoProjects.size());
        LOG.info("Amount of no revs and docs: {}", noRevAndDocCounter);
        LOG.info("Amount of no docs: {}", noDocCounter);
        LOG.info("Amount of no revs: {}", noRevCounter);
        LOG.info("Amount of used revs (mongo-table includes around 1.2M): {}", usedRevisions.size());
    }

    private List<String> findProcessedProjects() {
        List<String> processedRepoProjects = new ArrayList<>();

        ResponseEntity<List> response = restTemplate.exchange(GATEWAY_PROJECT_PROFILE_LIST_URL, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), List.class);

        LOG.info("Got response of state service: {}", response.getStatusCode());
        if (response.getStatusCode() != HttpStatus.OK) {
            System.err.println("Rerun stopped, since request for project profiles failed.");
            System.err.println("--> Gateway request resulted in: " + response.getStatusCode().value());
        }

        for (Object o : response.getBody()) {
            if (o instanceof LinkedHashMap) {
                Map<String, String> pp = (LinkedHashMap) o;
                String uri = pp.get("uri");
                String status = pp.get("status");
                String projectName = pp.get("projectName");
                String repositoryName = pp.get("repositoryName");

                // check if profile includes: url, id, state - generate id if needed
                if (uri == null || status == null) {
                    LOG.error("Could not process project profile for status: {}, uri: {} -> skip.", status, uri);
                    continue;
                }

                if (STATE_OF_FINISHED_CRAWLING != null && !StringUtils.equalsIgnoreCase(status,
                        MatildaStatusCode.getMatildaStatusCode(Integer.valueOf(STATE_OF_FINISHED_CRAWLING)).toString())) {
                    continue;
                }
                processedRepoProjects.add(repositoryName + ":" + projectName);
            }
        }
        return processedRepoProjects;
    }

    public String html2text(String html) {
        return Jsoup.parse(html).text();
    }

    public int hashCode(String revLine) {
        return new HashCodeBuilder(17, 37).append(revLine).toHashCode();
    }
}
