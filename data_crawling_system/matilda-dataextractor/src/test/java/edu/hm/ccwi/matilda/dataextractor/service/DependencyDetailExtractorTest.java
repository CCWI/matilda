package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DependencyDetailExtractorTest {

    DependencyDetailExtractor dde;

    List<CrawledDependency> originCrawledDepList;
    List<CrawledDependency> furtherCrawledDepList;

    String artifact1 = "artifact1";
    String artifact2 = "artifact2";
    String artifact3 = "artifact3";
    String artifact4 = "artifact4";
    String artifact5 = "artifact5";
    String artifact6 = "artifact6";
    String artifact7 = "artifact7";

    @BeforeEach
    public void init() {
        dde = new DependencyDetailExtractor(null, null, null);
        originCrawledDepList = new ArrayList<>();
        furtherCrawledDepList = new ArrayList<>();

        ArrayList<String> tag1 = new ArrayList();
        tag1.add("irrelevantTag");

        ArrayList<String> tag2 = new ArrayList();
        tag2.add("software");
        tag2.add("nice");
        tag2.add("tag");

        ArrayList<String> tag3 = new ArrayList();
        tag3.add("cookies");

        ArrayList<String> tag4 = new ArrayList();
        tag4.add("differentTag");

        ArrayList<String> tag5 = new ArrayList();
        tag5.add("tag");
        tag5.add("software");
        tag5.add("nice");

        ArrayList<String> tag6 = new ArrayList();
        tag6.add("winter");
        tag6.add("cookies");
        tag6.add("train");

        originCrawledDepList.add(new CrawledDependency("g1", artifact1, "1", true, LibCategory.BUILD_TOOLS.getName(), new ArrayList<>()));
        originCrawledDepList.add(new CrawledDependency("g2", artifact2, "1", true, LibCategory.CLOUD_COMPUTING.getName(), tag1)); // removed & replaced by 4
        originCrawledDepList.add(new CrawledDependency("g3", artifact3, "1", true, null, tag2)); // removed & replaced by 5
        originCrawledDepList.add(new CrawledDependency("g4", artifact4, "1", true, null, tag3)); // removed & replaced by 6

        furtherCrawledDepList.add(new CrawledDependency("g1", artifact1, "1", true, LibCategory.BUILD_TOOLS.getName(), new ArrayList<>()));
        furtherCrawledDepList.add(new CrawledDependency("g5", artifact5, "1", true, LibCategory.CLOUD_COMPUTING.getName(), tag4)); // newly added
        furtherCrawledDepList.add(new CrawledDependency("g6", artifact6, "1", true, null, tag5)); // newly added
        furtherCrawledDepList.add(new CrawledDependency("g7", artifact7, "1", true, null, tag6)); // newly added
    }

    @Test
    public void testCheckDependenciesChangedBetweenLists() {
        assertTrue(dde.checkDepListChanged(originCrawledDepList, furtherCrawledDepList));
    }

    @Test
    public void testChangedRelevantDependenciesBetweenLists() {
        List<CrawledDependency> cdList = dde.analyzeRemovedAndNewlyAddedDependencies(originCrawledDepList, furtherCrawledDepList);
        assertFalse(cdList.stream().anyMatch(dependency -> dependency.getArtifact().equals(artifact1)));
        for(CrawledDependency cd : cdList) {
            if(cd.getArtifact().equals(artifact2)) {
                assertFalse(cd.isNewlyAdded());
                assertTrue(cd.isRemoved());
                assertTrue(cd.getReplacedBy() != null && cd.getReplacedBy().getArtifact().equals(artifact5));
                continue;
            }
            if(cd.getArtifact().equals(artifact3)) {
                assertTrue(cd.isRemoved());
                continue;
            }
            if(cd.getArtifact().equals(artifact4)) {
                assertTrue(cd.isRemoved());
                continue;
            }
            if(cd.getArtifact().equals(artifact5)) {
                assertTrue(cd.isNewlyAdded());
                continue;
            }
            if(cd.getArtifact().equals(artifact6)) {
                assertTrue(cd.isNewlyAdded());
                continue;
            }
            if(cd.getArtifact().equals(artifact7)) {
                assertTrue(cd.isNewlyAdded());
                continue;
            }
        }
    }

    @Test
    public void checkForAllDependenciesRemovedInDepListTest() {

        List<CrawledDependency> list1 = new ArrayList<>();
        list1.add(new CrawledDependency("g", "a1", "1", true, true, false, true, null, new ArrayList<>(), null));
        list1.add(new CrawledDependency("g", "a2", "1", true, true, false, true, null, new ArrayList<>(), null));
        list1.add(new CrawledDependency("g", "a3", "1", true, true, false, true, null, new ArrayList<>(), null));

        assertTrue(dde.checkForAllDependenciesRemovedInBiggerDependencyList(list1));

        List<CrawledDependency> list2 = new ArrayList<>();
        list2.add(new CrawledDependency("g", "a1", "1", true, true, false, true, null, new ArrayList<>(), null));
        list2.add(new CrawledDependency("g", "a2", "1", true, true, false, true, null, new ArrayList<>(), null));
        list2.add(new CrawledDependency("g", "a3", "1", true, true, false, false, null, new ArrayList<>(), null));

        assertFalse(dde.checkForAllDependenciesRemovedInBiggerDependencyList(list2));

        List<CrawledDependency> list3 = new ArrayList<>();
        list3.add(new CrawledDependency("g", "a1", "1", true, true, false, true, null, new ArrayList<>(), null));
        list3.add(new CrawledDependency("g", "a2", "1", true, true, false, true, null, new ArrayList<>(), null));

        assertFalse(dde.checkForAllDependenciesRemovedInBiggerDependencyList(list3));

    }
}
