package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.dataextractor.service.fixture.TestExampleSoftwareRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.util.RevisionTimeSorter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataExtractorServiceImplTest {

    private DataExtractorServiceImpl dataExtractorService;
    private TestExampleSoftwareRepository sw;

    @BeforeEach
    public void init() {
        dataExtractorService = new DataExtractorServiceImpl(null, null, null, null,
                null, null, null, null, new UtilService(null,
                null, null), null);
        sw = new TestExampleSoftwareRepository();
    }


    /**
     *   Test of the following sw-structure:
     *   BEGINNING REV-TREE:
     *   Parent       SubParent          SubSubParent     ThirdGenChild
     *   kafka     -> (kafka, spark) -> (kafka, flink) -> (kafka, flink) -> 1000x (kafka, flink)
     *                               -> (kafka, spark) -> 1000x (kafka, flink)
     *
     *  EXPECTED RESULT:
     *  parentId01 -> subseqId01     -> subseqId02-1
     *                               -------------------> followupSubseqId-b0
     */
    @Test
    public void gatherAdditionalInfoByTreewalkComparisonTest() throws Exception {
        List<CrawledRevision> crawledRevisions = sw.create();

        crawledRevisions.sort(new RevisionTimeSorter());
        dataExtractorService.gatherAdditionalInfoByTreewalkComparisonAndAdjustment(crawledRevisions);

        // Check Revision-order
        assertThat(sw.crawledRevisionParent.getSubsequentCommitIdList()).contains(sw.crawledRevisionChild.getCommitId());
        assertThat(sw.crawledRevisionChild.getSubsequentCommitIdList()).contains(sw.crawledRevisionChildOfChild.getCommitId());

        // Check Parent's dependency
        assertThat(sw.crawledRevisionParent.getProjectList().get(0).getDependencyList().get(0).isNewlyAdded()).isTrue();
        assertThat(sw.crawledRevisionParent.getProjectList().get(0).getDependencyList().get(0).isRemoved()).isFalse();

        // Check FirstChild's dependency
        assertTrue(sw.crawledRevisionChild.getProjectList().get(0).getDependencyList().get(1).isNewlyAdded());
        assertTrue(sw.crawledRevisionChild.getProjectList().get(0).getDependencyList().get(1).isRemoved());
        assertFalse(sw.crawledRevisionChild.getProjectList().get(0).getDependencyList().get(0).isRemoved());
        assertFalse(sw.crawledRevisionChild.getProjectList().get(0).getDependencyList().get(0).isNewlyAdded());

        // Check if last child is correctly removed
        assertThat(crawledRevisions).hasSize(4);
        assertThat(crawledRevisions).extracting(CrawledRevision::getCommitId).contains("parentId01", "subseqId01", "subseqId02-1", "followupSubseqId-bX0");

        // Check ThirdChild's dependency
        List<CrawledDependency> childOfChildDependencies = sw.crawledRevisionChildOfChild.getProjectList().get(0).getDependencyList();
        assertThat(childOfChildDependencies.get(1).isNewlyAdded()).isTrue();
        assertThat(childOfChildDependencies.get(1).isRemoved()).isFalse();
        assertThat(childOfChildDependencies.get(0).isRemoved()).isFalse();
        assertThat(childOfChildDependencies.get(0).isNewlyAdded()).isFalse();
        assertThat(sw.crawledRevisionChildOfChild.getSimilarFollowupCommitsUntil()).isEqualTo(sw.lastCommitTime);
    }

    @Test
    void calculateMeanSubseqRevisionSize_success() {
        List<CrawledRevision> crawledRevisions = new ArrayList<>();

        CrawledRevision cr10 = new CrawledRevision();
        cr10.addSubsequentRevision("cr11", "cr12", "cr13", "cr14", "cr15", "cr16");
        crawledRevisions.add(cr10);

        CrawledRevision cr20 = new CrawledRevision();
        cr10.addSubsequentRevision("cr21", "cr22", "cr23", "cr24", "cr25", "cr26", "cr27", "cr28", "cr29");
        crawledRevisions.add(cr20);

        CrawledRevision cr30 = new CrawledRevision();
        cr10.addSubsequentRevision("cr31", "cr32");
        crawledRevisions.add(cr30);


        double v = this.dataExtractorService.calculateMeanSubseqRevisionSize(crawledRevisions);

        assertThat(v).isEqualTo(5.666666666666667);
    }

    @Test
    void calculateMeanSubseqRevisionSize_empty() {
        List<CrawledRevision> crawledRevisions = sw.create();

        double v = this.dataExtractorService.calculateMeanSubseqRevisionSize(crawledRevisions);

        assertThat(v).isEqualTo(0.0);
    }
}