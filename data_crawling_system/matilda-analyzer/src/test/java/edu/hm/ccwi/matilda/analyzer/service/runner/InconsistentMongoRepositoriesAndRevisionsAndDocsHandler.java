package edu.hm.ccwi.matilda.analyzer.service.runner;

import edu.hm.ccwi.matilda.analyzer.MatildaAnalyzerApplication;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaClient;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaSender;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDocumentation;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.util.*;

/**
 * NO TEST!
 * This Class is used to cleanup inconsistencies in mongoDB
 */
@SpringBootTest(classes = MatildaAnalyzerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class InconsistentMongoRepositoriesAndRevisionsAndDocsHandler {

    private static final Logger LOG = LoggerFactory.getLogger(InconsistentMongoRepositoriesAndRevisionsAndDocsHandler.class);

    private static final boolean WRITE_MODE = true;
    protected static final String STATE_SERVICE_PROJECT_PROFILE_URL = "http://localhost:8086/project/profile";
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
        List<String> processedRepoProjects = findProcessedProjects();
        Set<String> usedRevisionIds = new HashSet<>();

        LOG.info("Found {} processed Projects in Status {}", processedRepoProjects.size(), STATE_OF_FINISHED_CRAWLING);

        Set<String> brokenProjects = new HashSet<>();
        int noRevAndDocCounter = 0;
        int noDocCounter = 0;
        int noRevCounter = 0;
        int stateCounter = 0;
        int repoWithoutValidRevCounter = 0;

        // (1) ANALYZE FROM REPOSITORY PERSPECTIVE
        for (String processedRepoProject : processedRepoProjects) {
            Set<String> notLinkedRevisions = new HashSet<>();
            LOG.info("cleanup-state: {}/{}", stateCounter++, processedRepoProjects.size());
            CrawledRepository crawledMongoRepository = mongoRepoRepo.getCrawledRepositoryById(processedRepoProject);
            ProjectProfile projectProfileByRepoProj = getProjectProfileByRepoProj(crawledMongoRepository.getRepositoryName(),
                    crawledMongoRepository.getProjectName());
            boolean repoHasAtLeastOneValidRev = false;
            boolean updatedMongoRepo = false;
            for (String revision : crawledMongoRepository.getRevisionCommitIdList()) {
                Optional<CrawledRevision> rev = mongoRevRepo.findById(revision);
                Optional<CrawledDocumentation> doc = mongoDocRepository.findById(revision);

                if(rev.isEmpty() && doc.isEmpty()) {
                    noRevAndDocCounter++;
                    //LOG.warn("{} -> For Revision {} is NO REV AND DOC AVAILABLE! (Found {} of those dead bodies)", processedRepoProject, revision, noRevAndDocCounter);
                    brokenProjects.add(processedRepoProject);
                    if (WRITE_MODE) {
                        // REMOVE REV FROM REPO-LIST AND UPDATE REPO IN THE END!
                        notLinkedRevisions.add(revision);
                        updatedMongoRepo = true;
                    }
                } else if(rev.isPresent() && doc.isEmpty()) {
                    noDocCounter++;
                    brokenProjects.add(processedRepoProject);
                } else if(rev.isEmpty() && doc.isPresent()) {
                    noRevCounter++;
                    brokenProjects.add(processedRepoProject);
                    if (WRITE_MODE) {
                        // REMOVE REV FROM REPO-LIST AND UPDATE REPO IN THE END!
                        notLinkedRevisions.add(revision);
                        updatedMongoRepo = true;
                    }
                } else {
                    repoHasAtLeastOneValidRev = true;
                    usedRevisionIds.add(revision);
                }
            }

            if(updatedMongoRepo && CollectionUtils.isNotEmpty(notLinkedRevisions)) {
                if (WRITE_MODE) {
                    crawledMongoRepository.getRevisionCommitIdList().removeAll(notLinkedRevisions);
                    mongoRepoRepo.save(crawledMongoRepository);
                }
            }

            if(!repoHasAtLeastOneValidRev) {
                repoWithoutValidRevCounter++;
                if (WRITE_MODE) {
                    mongoRepoRepo.delete(crawledMongoRepository);
                    projectProfileByRepoProj.setStatus(MatildaStatusCode.ERROR_PROJECT_NOT_SUPPORTED);
                    saveOrUpdateProjectProfile(projectProfileByRepoProj);
                }
            }
        }

        LOG.info("#######################################################");
        LOG.info("################## FIRST SUMMARY ######################");
        LOG.info("#######################################################");
        LOG.info("1. Check from Repo-Perspective:");
        LOG.info("  Amount of broken Projects: {}/{}", brokenProjects.size(), processedRepoProjects.size());
        LOG.info("  Amount of no revs and docs: {}", noRevAndDocCounter);
        LOG.info("  Amount of no docs: {}", noDocCounter);
        LOG.info("  Amount of no revs: {}", noRevCounter);
        LOG.info("  Amount of RepositoryProjects without a valid rev: {} (would be removed!)", repoWithoutValidRevCounter);

        // (2) LOOK AT UNLINKED REVS FROM THE PERSPECTIVE OF THE REVS
        int knownRevCounter = 0;
        int unknownRevCounter = 0;

        long count = mongoRevRepo.count();
        int pageSize = 1000;
        long pages = count / pageSize;
        for (int i = 0; i < pages; i++) {
            LOG.info("Analyzing unlinked Revs - Page: {}/{}", i, pages);
            Page<CrawledRevision> allRevisionsOnPage = mongoRevRepo.findAll(PageRequest.of(i, pageSize));
            for (CrawledRevision crawledRev : allRevisionsOnPage) {
                if(usedRevisionIds.contains(crawledRev.getCommitId())) {
                    knownRevCounter++;
                } else {
                    unknownRevCounter++;
                    if (WRITE_MODE) {
                        // remove rev from MongoDB
                        mongoRevRepo.delete(crawledRev);
                    }
                }
            }
        }

        // (3) REPORT
        LOG.info("#######################################################");
        LOG.info("################## TOTAL SUMMARY ######################");
        LOG.info("#######################################################");
        LOG.info("1. Check from Repo-Perspective:");
        LOG.info("  Amount of broken Projects: {}/{}", brokenProjects.size(), processedRepoProjects.size());
        LOG.info("  Amount of no revs and docs: {}", noRevAndDocCounter);
        LOG.info("  Amount of no docs: {}", noDocCounter);
        LOG.info("  Amount of no revs: {}", noRevCounter);
        LOG.info("  Amount of RepositoryProjects without a valid rev: {} (would be removed!)", repoWithoutValidRevCounter);
        LOG.info("");
        LOG.info("2. Check from Rev/Doc-Perspective:");
        LOG.info("  Amount of used revs (mongo-table includes around 1.2M): {}, unknown are: {}", knownRevCounter, unknownRevCounter);
    }

    public void saveOrUpdateProjectProfile(ProjectProfile projectProfile) throws RestClientException {
        ResponseEntity<String> response = restTemplate.exchange(STATE_SERVICE_PROJECT_PROFILE_URL, HttpMethod.PUT,
                new HttpEntity<>(projectProfile, new HttpHeaders()), String.class);
        if (ObjectUtils.isEmpty(response)) {
            throw new RestClientException("State service error occurred. No response received");
        } else if (response.getStatusCode().is5xxServerError()) {
            throw new RestClientException("State service error occurred. Http error code received: " + response.getStatusCode());
        }
    }

    public ProjectProfile getProjectProfileByRepoProj(String repositoryName, String projectName) throws RestClientException {
        String url = STATE_SERVICE_PROJECT_PROFILE_URL + "?repositoryName=" + repositoryName + "&projectName=" + projectName;
        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

        ResponseEntity<ProjectProfile> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(defaultHeaders), ProjectProfile.class);
        if (ObjectUtils.isEmpty(response)) {
            throw new RestClientException("State service error occurred. No response received");
        } else if (response.getStatusCode().is5xxServerError()) {
            throw new RestClientException("State service error occurred. Http error code received: " + response.getStatusCode());
        }
        return response.getBody();
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
}
