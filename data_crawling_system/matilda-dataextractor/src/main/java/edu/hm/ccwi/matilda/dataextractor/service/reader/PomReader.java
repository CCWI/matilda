package edu.hm.ccwi.matilda.dataextractor.service.reader;

import edu.hm.ccwi.matilda.base.util.IOHandler;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

/**
 * Class to handle all entries in a pom.xml
 * @author Max.Auch
 */
@Service
public class PomReader implements DMFReader {

    private static final Logger LOG = LoggerFactory.getLogger(PomReader.class);

    private static final String STD_MAVEN_FILE_NAME = "pom.xml";

    public List<File> findAllRelatedFilesInProject(String projectUri) {
        return new IOHandler().findFile(STD_MAVEN_FILE_NAME, new File(projectUri));
    }

    /**
     * Extract all existing Dependencies from all dependency-management-files.
     */
    public List<CrawledProject> extractProjectDependency(List<File> pomFilesInProject, String revisionDirPath, String repoName, String projectName) {
        List<CrawledProject> resultCPList = new ArrayList<>();
        int pomParserErrorCounter = 0;

        try {
            for (File pom : pomFilesInProject) {
                try {
                    Model model = getModel(pom.getAbsolutePath());
                    CrawledProject cp = createProject(model, new File(revisionDirPath), pom.getAbsolutePath(), repoName, projectName);
                    if (isNotEmpty(cp)) {
                        // check if GroupId is null -> if so, get groupId from parent, parent's parent, ...
                        if (StringUtils.isEmpty(cp.getProjectGroup()) && isNotEmpty(model.getParent())) {
                            cp.setProjectGroup(model.getParent().getGroupId());
                        }
                        resultCPList.add(cp);
                    }
                } catch (IOException e) {
                    LOG.error("POM-Parser: {}", e.getMessage());
                    pomParserErrorCounter = +1;
                } catch (XmlPullParserException e) {
                    LOG.debug("XmlPullParserException: {}", e.getMessage());
                    pomParserErrorCounter = +1;
                }
            }
        } finally {
            if(pomParserErrorCounter > 0) {
                LOG.error("Amount of errors occurred while parsing {} pom-files: {}", pomFilesInProject.size(), pomParserErrorCounter);
            }
        }
        return resultCPList;
    }


    private CrawledProject createProject(Model model, File rootProjectDir, String projectDir, String repoName, String projectName) {
        if (!isComplete(model)) { return null; }
        boolean usingReleaseTags = isProjectUsingReleaseTags(rootProjectDir.getPath(), repoName, projectName);
        List<CrawledDependency> pdList = extractDependencies(model);

        return new CrawledProject(model.getName(), model.getDescription(), model.getGroupId(),
                model.getArtifactId(), model.getVersion(), projectDir, pdList, usingReleaseTags);
    }

    private List<CrawledDependency> extractDependencies(Model model) {
        List<CrawledDependency> projectDependencyList = new ArrayList<>();
        for (Dependency dep : model.getDependencies()) {
            projectDependencyList.add(new CrawledDependency(dep.getGroupId(), dep.getArtifactId(), dep.getVersion()));
        }
        return projectDependencyList;
    }

    /**
     * Check if path contains releaseTag.
     */
    private boolean isProjectUsingReleaseTags(String path, String repoName, String projectName) {
        return path.contains(repoName + File.separator + projectName + File.separator + "refs" + File.separator + "tags");
    }

    /**
     * check if articleID or dependencies are missing.
     */
    protected boolean isComplete(Model model) {
        return !((model.getArtifactId() == null || model.getArtifactId().isEmpty()) ||
                (model.getDependencies() == null || model.getDependencies().size() <= 0));
    }

    private Model getModel(String projectDir) throws IOException, XmlPullParserException {
        try(FileReader fr = new FileReader(projectDir + File.separator + STD_MAVEN_FILE_NAME)) {
            return new MavenXpp3Reader().read(fr, true);
        }
    }
}
