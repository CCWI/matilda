package edu.hm.ccwi.matilda.korpus.service.impl;

import com.google.gson.Gson;
import edu.hm.ccwi.matilda.korpus.model.*;
import edu.hm.ccwi.matilda.korpus.service.GenericAnalyzer;
import edu.hm.ccwi.matilda.korpus.service.KorpusService;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledSoftwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KorpusServiceImpl implements KorpusService {

    @Inject
    CrawledSoftwareRepository mongoRepos;
    @Inject
    CrawledRevisionRepository mongoRevs;
    @Inject
    CrawledDocumentationRepository mongoDocs;

    public GeneralKorpusResults analyzeGeneralStats() {
        GenericAnalyzer gAnalyzer = new GenericAnalyzer();
        int pageNumber = 0;
        int pageLimit = 100;
        Page<CrawledRepository> page;
        do {
            page = mongoRepos.findAll(PageRequest.of(pageNumber, pageLimit));
            page.forEach(crawledRepository -> gAnalyzer.analyze(crawledRepository));
            pageNumber++;
        } while (!page.isLast());

        return gAnalyzer.getResults();
    }

    @Override
    public String qsAnalyses() {
        Map<String, List<String>> resultMap = new HashMap<>();
        List<String> reposWithoutRevision = new ArrayList<>(); // 1
        List<String> revisionOfRepoNotAvailable = new ArrayList<>(); // 2
        List<String> revisionWithoutProjects = new ArrayList<>(); // 3
        List<String> documentsNotAvailableForRevision = new ArrayList<>(); // 4
        List<String> projectsWithoutDependencies = new ArrayList<>(); // 5
        List<String> nothingChangedInDependencyLists = new ArrayList<>(); // 6

        List<CrawledRepository> crawledRepositoryList = mongoRepos.findAll();
        if (crawledRepositoryList == null || crawledRepositoryList.isEmpty()) {
            return "No Repositories found for QS!";
        }

        for (CrawledRepository cRepo : crawledRepositoryList) {
            // 1. check if cRepo contains Revisions
            if (cRepo.getRevisionCommitIdList() == null || cRepo.getRevisionCommitIdList().isEmpty()) {
                reposWithoutRevision.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + ">>" + cRepo.getId());
            }
            for (String revId : cRepo.getRevisionCommitIdList()) {
                // 2. check if linked revisions are available
                if (!mongoRevs.existsById(revId)) {
                    revisionOfRepoNotAvailable.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + ">>" + revId);
                } else {
                    CrawledRevision cRev = mongoRevs.getCrawledRevById(revId);
                    // 3. check for cRevs without Projects
                    if (cRev.getProjectList() == null || cRev.getProjectList().isEmpty()) {
                        revisionWithoutProjects.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + ">>" + revId);
                    }
                    // 4. check if documentation is available for revision.
                    if (!mongoDocs.existsById(cRev.getCommitId())) {
                        documentsNotAvailableForRevision.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + ">>" + revId);
                    }

                    boolean someDependencyChanged = false;
                    for (CrawledProject crawledProject : cRev.getProjectList()) {
                        // 5. Projekte ohne Dependencies
                        if (crawledProject.getDependencyList() == null || crawledProject.getDependencyList().isEmpty()) {
                            projectsWithoutDependencies.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + "|"
                                    + revId + ">>" + crawledProject.getProjectGroup() + ":" + crawledProject.getProjectArtifact());
                        }
                        for (CrawledDependency crawledDependency : crawledProject.getDependencyList()) {
                            if(crawledDependency.isNewlyAdded() || crawledDependency.isRemoved()) {
                                someDependencyChanged = true;
                                break;
                            }
                        }
                    }
                    // 6. check if nothing changed in dependencies
                    if (!someDependencyChanged) {
                        nothingChangedInDependencyLists.add(cRepo.getRepositoryName() + "|" + cRepo.getProjectName() + ">>" + revId);
                    }
                }
            }
        }

        resultMap.put("reposWithoutRevision = " + reposWithoutRevision.size(), reposWithoutRevision);
        resultMap.put("revisionOfRepoNotAvailable = " + revisionOfRepoNotAvailable.size(), revisionOfRepoNotAvailable);
        resultMap.put("revisionWithoutProjects = " + revisionWithoutProjects.size(), revisionWithoutProjects);
        resultMap.put("documentsNotAvailableForRevision = " + documentsNotAvailableForRevision.size(), documentsNotAvailableForRevision);
        resultMap.put("projectsWithoutDependencies = " + projectsWithoutDependencies.size(), projectsWithoutDependencies);
        resultMap.put("nothingChangedInDependencyLists = " + nothingChangedInDependencyLists.size(), nothingChangedInDependencyLists);

        return new Gson().toJson(resultMap);
    }
}
