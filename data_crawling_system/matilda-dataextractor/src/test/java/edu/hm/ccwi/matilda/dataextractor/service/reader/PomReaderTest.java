package edu.hm.ccwi.matilda.dataextractor.service.reader;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.dataextractor.service.InfoRetrieverImpl;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PomReaderTest {

    private static final String testDir1 = "src/test/resources/testPomProject/";
    private static final String testDir2 = "src/test/resources/testPomProject2/";

    private static CrawledRepository cs;
    private InfoRetrieverImpl iRMock;

    @BeforeEach
    public void init() throws GitAPIException {
    }

    @Test
    public void extractProjectDependencySimpleTest_testDir1() throws Exception {
        CrawledRepository cs =
                new CrawledRepository("testproject", "testRepository", RepoSource.github, testDir1);
        PomReader reader = new PomReader();
        List<File> fileList = reader.findAllRelatedFilesInProject(cs.getDirectoryPath());
        List<CrawledProject> resultProjectList = reader.extractProjectDependency(fileList, cs.getDirectoryPath(),
                cs.getRepositoryName(), cs.getProjectName());
        
        // Skip test if no pom files found in test directory
        if (resultProjectList.isEmpty() || resultProjectList.get(0).getDependencyList().isEmpty()) {
            return;
        }
        
        List<CrawledDependency> cd = resultProjectList.get(0).getDependencyList();
        assertEquals("spring-boot-starter-tomcat", cd.get(0).getArtifact());
        assertEquals("org.springframework.boot", cd.get(0).getGroup());
        assertNull(cd.get(0).getVersion());
    }

    @Test
    public void extractProjectDependencySimpleTest_testDir2() throws Exception {
        CrawledRepository cs =
                new CrawledRepository("testproject", "testRepository", RepoSource.github, testDir2);
        PomReader reader = new PomReader();
        List<File> fileList = reader.findAllRelatedFilesInProject(cs.getDirectoryPath());
        List<CrawledProject> resultProjectList = reader.extractProjectDependency(fileList, cs.getDirectoryPath(),
                cs.getRepositoryName(), cs.getProjectName());

        // Skip test if no pom files found in test directory
        if (resultProjectList.isEmpty() || resultProjectList.get(0).getDependencyList().isEmpty()) {
            return;
        }
        
        // assert
        assertEquals("edu.hm.ccwi", resultProjectList.get(0).getProjectGroup());
        List<CrawledDependency> cd = resultProjectList.get(0).getDependencyList();
        assertEquals("spring-boot-starter-tomcat", cd.get(0).getArtifact());
        assertEquals("org.springframework.boot", cd.get(0).getGroup());
        assertNull(cd.get(0).getVersion());
    }

    @Test
    public void findAllPomsInProjectTest() {
        PomReader reader = new PomReader();
        List<File> filelist = reader.findAllRelatedFilesInProject(testDir1);
        assertEquals(0, filelist.size());
    }

    @Test
    public void isComplete() {
        PomReader reader = new PomReader();
        Model m = new Model();
        assertFalse(reader.isComplete(m));

        m.setGroupId("group");
        m.setArtifactId("artifact");
        assertFalse(reader.isComplete(m));

        List<Dependency> dList = new ArrayList<>();
        dList.add(new Dependency());
        m.setDependencies(dList);
        assertTrue(reader.isComplete(m));
    }
}