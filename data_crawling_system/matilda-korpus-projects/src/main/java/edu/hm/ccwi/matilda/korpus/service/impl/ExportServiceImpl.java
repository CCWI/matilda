package edu.hm.ccwi.matilda.korpus.service.impl;

import edu.hm.ccwi.matilda.korpus.model.*;
import edu.hm.ccwi.matilda.korpus.service.CategorizationService;
import edu.hm.ccwi.matilda.korpus.service.ExportService;
import edu.hm.ccwi.matilda.korpus.service.model.Domain;
import edu.hm.ccwi.matilda.korpus.service.model.MatrixKorpusRow;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledSoftwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExportServiceImpl implements ExportService {

    private static final String CSV_SEPARATOR = ",";

    @Inject
    CategorizationService categorizationService;
    @Inject
    CrawledSoftwareRepository mongoRepos;
    @Inject
    CrawledRevisionRepository mongoRevs;

    BufferedWriter writer;

    public void exportMinimalKorpus() throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("exportedCrawledRepositories.csv"), "UTF-8"));
        int pageNumber = 0;
        int pageLimit = 100;
        Page<CrawledRepository> page;
        writer.write("commitId,repositoryName,projectName,projectGroup,projectArtifact,projectVersion,dependencyGroup1,dependencyArtifact1,dependencyGroup2,dependencyArtifact2,dependencyGroup3,dependencyArtifact3");
        writer.newLine();
        do {
            page = mongoRepos.findAll(PageRequest.of(pageNumber, pageLimit));
            for (CrawledRepository crawledRepository : page) {
                String revId = crawledRepository.getRevisionCommitIdList().get(crawledRepository.getRevisionCommitIdList().size() - 1);
                CrawledRevision crawledRevision = mongoRevs.getCrawledRevById(revId);
                List<String> csvRowList = createCsvRow(crawledRepository, crawledRevision);
                if(csvRowList != null && !csvRowList.isEmpty()) {
                    for(String csvRow : csvRowList) {
                        writer.write(csvRow);
                        writer.newLine();
                    }
                }
            }
            pageNumber++;
        } while (!page.isLast());


        writer.flush();
        writer.close();
    }


    /**
     *
     * @param sizeOfDependencies
     * @return
     */
    public List<MatrixKorpusRow> getMinimalKorpus(int sizeOfDependencies) {
        List<MatrixKorpusRow> csvRowList = new ArrayList<>();
        int pageNumber = 0; int pageLimit = 100;
        Page<CrawledRepository> page;
        do {
            page = mongoRepos.findAll(PageRequest.of(pageNumber, pageLimit));
            for (CrawledRepository repo : page) {
                String revId = repo.getRevisionCommitIdList().get(repo.getRevisionCommitIdList().size() - 1);
                CrawledRevision revision = mongoRevs.getCrawledRevById(revId);
                // include only latest commit of leafs.
                if (revision != null && revision.getProjectList() != null && (revision.getSubsequentCommitIdList() == null ||
                        revision.getSubsequentCommitIdList().isEmpty() || revision.getSubsequentCommitIdList().get(0) == null ||
                        revision.getSubsequentCommitIdList().get(0).isEmpty())) {
                    for (CrawledProject crawledProject : revision.getProjectList()) {
                        // include only all mvn-GA-projects without velocity-placeholder: "${}" and with >= 5 - for first test - dependencies.
                        if(!crawledProject.getProjectGroup().contains("$") && !crawledProject.getProjectArtifact().contains("$")
                                && crawledProject.getDependencyList() != null && crawledProject.getDependencyList().size() >= sizeOfDependencies) {
                            MatrixKorpusRow matrixKorpusRow = new MatrixKorpusRow(revision.getCommitId(), repo.getRepositoryName() + ":" + repo.getProjectName(),
                                    crawledProject.getProjectGroup() + ":" + crawledProject.getProjectArtifact());

                            for (CrawledDependency dependency : crawledProject.getDependencyList()) {
                                matrixKorpusRow.getDependencyGA().add(dependency.getGroup() + ":" + dependency.getArtifact());
                            }
                            // avoid duplicate mvn-GA-projects
                            if(!isDuplicateEntry(csvRowList, matrixKorpusRow)) {
                                csvRowList.add(matrixKorpusRow);
                            }
                        }
                    }
                }
            }
            pageNumber++;
        } while (!page.isLast());

        return csvRowList;
    }

    /**
     * Helper-Method - find duplicate mvn-GA-Projects.
     *
     * @param csvRowList
     * @param matrixKorpusRow
     * @return
     */
    private boolean isDuplicateEntry(List<MatrixKorpusRow> csvRowList, MatrixKorpusRow matrixKorpusRow) {
        boolean isDuplicateEntry = false;
        for(MatrixKorpusRow korpusRow : csvRowList) {
            if(korpusRow.getGa().equalsIgnoreCase(matrixKorpusRow.getGa())) {
                isDuplicateEntry = true;
                break;
            }
        }
        return isDuplicateEntry;
    }

    private List<String> getDistinctDependencyList(List<MatrixKorpusRow> matrixKorpusRowList) {
        List<String> dependencyList = new ArrayList<>();
        matrixKorpusRowList.forEach(row -> row.getDependencyGA().forEach(dependency -> {
            if (!dependencyList.contains(dependency)) { dependencyList.add(dependency); }
        }));

        return dependencyList;
    }

    private List<String> getFullMongoDistinctDependencyList() {
        List<String> dependencyList = new ArrayList<>();
        Page<CrawledRepository> page;
        int pageNumber = 0; int pageLimit = 100;
        do {
            page = mongoRepos.findAll(PageRequest.of(pageNumber, pageLimit));
            for (CrawledRepository crawledRepository : page) {
                String revId = crawledRepository.getRevisionCommitIdList().get(crawledRepository.getRevisionCommitIdList().size() - 1);
                CrawledRevision crawledRevision = mongoRevs.getCrawledRevById(revId);
                if(crawledRevision != null && crawledRevision.getProjectList() != null) {
                    for (CrawledProject crawledProject : crawledRevision.getProjectList()) {
                        for (CrawledDependency dependency : crawledProject.getDependencyList()) {
                            String newDependency = dependency.getGroup() + ":" + dependency.getArtifact();
                            if (!dependencyList.contains(newDependency)) { dependencyList.add(newDependency); }
                        }
                    }
                }
            }
            pageNumber++;
        } while (!page.isLast());

        return dependencyList;
    }

    private List<String> createCsvRow(CrawledRepository repo, CrawledRevision revision) {
        List<String> csvRowList = new ArrayList();

        if (revision != null && revision.getProjectList() != null && (revision.getSubsequentCommitIdList() == null ||
                revision.getSubsequentCommitIdList().isEmpty() || revision.getSubsequentCommitIdList().get(0) == null ||
                revision.getSubsequentCommitIdList().get(0).isEmpty())) {
            for (CrawledProject crawledProject : revision.getProjectList()) {
                StringBuffer oneLine = new StringBuffer();
                oneLine.append(revision.getCommitId());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(repo.getRepositoryName());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(repo.getProjectName());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(crawledProject.getProjectGroup());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(crawledProject.getProjectArtifact());
                oneLine.append(CSV_SEPARATOR);
                oneLine.append(crawledProject.getProjectVersion());
                oneLine.append(CSV_SEPARATOR);
                for (CrawledDependency dependency : crawledProject.getDependencyList()) {
                    oneLine.append(dependency.getGroup());
                    oneLine.append(CSV_SEPARATOR);
                    oneLine.append(dependency.getArtifact());
                    oneLine.append(CSV_SEPARATOR);
                }
                csvRowList.add(oneLine.toString());
            }
        }
        return csvRowList;
    }
}