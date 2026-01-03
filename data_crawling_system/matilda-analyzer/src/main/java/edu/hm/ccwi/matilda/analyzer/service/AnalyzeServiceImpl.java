package edu.hm.ccwi.matilda.analyzer.service;

import edu.hm.ccwi.matilda.analyzer.exception.AnalyzerException;
import edu.hm.ccwi.matilda.analyzer.service.decision.DecisionDetectionService;
import edu.hm.ccwi.matilda.analyzer.service.decision.DesignDecisionExtractor;
import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyzeServiceImpl extends GenericAnalyzer implements AnalyzeService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeServiceImpl.class);

    private static int analyzerCounter = 0;

    final CrawledSoftwareRepository mongoRepoRepo;
    final DesignDecisionExtractor designDecisionExtractor;
    final AnalyzerStateHandler stateHandler;
    final DecisionDetectionService decisionDetectionService;

    public AnalyzeServiceImpl(CrawledSoftwareRepository mongoRepoRepo, CrawledRevisionRepository mongoRevRepo,
                              DesignDecisionExtractor designDecisionExtractor, AnalyzerStateHandler stateHandler,
                              DecisionDetectionService decisionDetectionService) {
        super(mongoRevRepo);
        this.mongoRepoRepo = mongoRepoRepo;
        this.designDecisionExtractor = designDecisionExtractor;
        this.stateHandler = stateHandler;
        this.decisionDetectionService = decisionDetectionService;
    }

    /**
     * Main method to analyze crawled project.
     */
    @Override
    public String analyzeProject(String crawledRepositoryId) {
        LOG.info("");
        LOG.info("");
        LOG.info("___________________________________________________________________________________________________");
        LOG.info("________[{}]__________ START ANALYZER FOR {} __________________", ++analyzerCounter, crawledRepositoryId);

        if(startAnalyzing(crawledRepositoryId)) {
            LOG.info("______________________ SUCCESSFULLY FINISHED ANALYZING REPOSITORY ______________________");
        } else {
            LOG.info("______________________ END FAILED ANALYZING REPOSITORY ______________________");
        }
        LOG.info("");
        LOG.info("");
        return crawledRepositoryId;
    }

    @Override
    public List<String> findAndUpdateAllAnalyzableRepoIdsByState(String matildaState) {
        List<ProjectProfile> projectProfilesByState = stateHandler.getProjectProfilesByState(matildaState);
        List<String> idList = new ArrayList<>();
        for (ProjectProfile projectProfile : projectProfilesByState) {
            idList.add(projectProfile.getRepositoryName() + ":" + projectProfile.getProjectName());
            projectProfile.setStatus(MatildaStatusCode.FINISHED_DATA_EXTRACTION);
            stateHandler.saveOrUpdateProjectProfile(projectProfile);
        }
        return idList;
    }

    private boolean startAnalyzing(String crawledRepositoryId) {
        MatildaStatusCode matildaStatusCode = null;
        ProjectProfile projectProfile = null;
        boolean success;

        try {
            CrawledRepository crawledRepository = mongoRepoRepo.findById(crawledRepositoryId).orElse(null);
            if (crawledRepository == null || !crawledRepository.isValid()) {
                throw new AnalyzerException("Did not find Repository for received id " + crawledRepositoryId);
            }

            // load data from db
            projectProfile = stateHandler.getProjectProfileByRepoProj(crawledRepository.getRepositoryName(), crawledRepository.getProjectName());
            matildaStatusCode = updateProjectProfileState(projectProfile);
            List<CrawledRevision> revisionListOfRepository = retrieveRevisionsFromRepository(crawledRepository);

            // DecisionDetection
            decisionDetectionService.analyzeAndSaveDesignDecisions(crawledRepository, revisionListOfRepository);

            matildaStatusCode = MatildaStatusCode.FINISHED_ANALYZING_PROJECT;
            success = true;

        } catch (Exception e) {
            LOG.error("A general exception occurred: {}", e.getMessage());
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
            success = false;
        } finally {
            if (projectProfile != null) { // Only update Status if project profile is initialized in DB
                projectProfile.setStatus(matildaStatusCode);
                stateHandler.saveOrUpdateProjectProfile(projectProfile);
            }
        }
        return success;
    }

    private MatildaStatusCode updateProjectProfileState(ProjectProfile projectProfile) throws StateException {
        if(projectProfile != null) {
            projectProfile.setStatus(MatildaStatusCode.RECEIVED_REQUEST_FOR_ANALYZING_PROJECT);
            stateHandler.saveOrUpdateProjectProfile(projectProfile);
            return MatildaStatusCode.RECEIVED_REQUEST_FOR_ANALYZING_PROJECT;
        } else {
            throw new StateException("No project profile found for received analyzing request");
        }
    }
}
