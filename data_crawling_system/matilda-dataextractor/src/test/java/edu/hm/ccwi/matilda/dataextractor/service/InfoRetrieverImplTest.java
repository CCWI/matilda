package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.base.model.enumeration.RevisionType;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class InfoRetrieverImplTest {

    List<CrawledRevision> revList;
    CrawledDependency dependency01;
    CrawledDependency dependency02;
    CrawledDependency dependency03;
    CrawledDependency dependency04;

    @BeforeEach
    public void init() {
        revList = new ArrayList<>();

        dependency01 = new CrawledDependency("com.test", "kafka", "1", true, null, new ArrayList<>());
        dependency02 = new CrawledDependency("hm.unit.test", "internalDependency1", "1", true, null, new ArrayList<>());
        dependency03 = new CrawledDependency("hm.unit.test.subpackage", "internalDependency2", "1", true, null, new ArrayList<>());
        dependency04 = new CrawledDependency("${parent.group}", "${parent.artifact}", "1", true, null, new ArrayList<>());

        // ##### 1) init parent revision #####
        List<CrawledProject> projectParentList = new ArrayList<>();
        List<CrawledDependency> dependencyParentList = new ArrayList<>();
        dependencyParentList.add(dependency01);
        dependencyParentList.add(dependency02);
        dependencyParentList.add(dependency03);
        dependencyParentList.add(dependency04);
        CrawledProject crawledProjectParent = new CrawledProject("testProject", "description...",
                "hm.unit.test", "unittest", "0.1", "someProjectPath",
                dependencyParentList, false);
        projectParentList.add(crawledProjectParent);
        CrawledRevision crawledRevision = new CrawledRevision("parentId01", RevisionType.commit,
                "unittest", LocalDateTime.now(), LocalDateTime.now(), projectParentList);
        revList.add(crawledRevision);
    }

    @Test
    public void filterFirstPartyProjectDependenciesTest() {
        List<CrawledRevision> filteredRev = new InfoRetrieverImpl(null, null).filterFirstPartyProjectDependencies(revList);
        assertThat(filteredRev.get(0).getProjectList().get(0).getDependencyList()).contains(dependency01);
        assertThat(filteredRev.get(0).getProjectList().get(0).getDependencyList()).doesNotContain(dependency02);
        assertThat(filteredRev.get(0).getProjectList().get(0).getDependencyList()).doesNotContain(dependency03);
    }

    @Test
    public void filterUnresolvedProjectDependenciesTest() {
        List<CrawledRevision> filteredRev = new InfoRetrieverImpl(null, null).filterUnresolvedProjectDependencies(revList);
        assertThat(filteredRev.get(0).getProjectList().get(0).getDependencyList()).hasSize(3);
        assertThat(filteredRev.get(0).getProjectList().get(0).getDependencyList()).doesNotContain(dependency04);
    }
}
