package edu.hm.ccwi.matilda.dataextractor.service.fixture;

import edu.hm.ccwi.matilda.base.model.enumeration.RevisionType;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestExampleSoftwareRepository {

    public CrawledRevision crawledRevisionParent;
    public CrawledRevision crawledRevisionChild;
    public CrawledRevision crawledRevisionChildOfChild;
    public CrawledRevision crawledRevisionChildOfChild2;
    public CrawledRevision crawledRevisionThirdGenerationChild;

    public LocalDateTime lastCommitTime = LocalDateTime.now().plusYears(10);


    public List<CrawledRevision> create() {
        List<CrawledRevision> crawledRevisionList = new LinkedList<>();

        // ##### define proper dateTimes for test #####
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minus(1, ChronoUnit.DAYS);
        LocalDateTime yesterdayMorning = yesterday.with(LocalTime.of(8, 0, 0));
        LocalDateTime yesterdayEvening = yesterday.with(LocalTime.of(18, 59, 59));
        LocalDateTime yesterdayNight = yesterday.with(LocalTime.of(23, 59, 59));

        this.crawledRevisionParent = createParent(yesterdayMorning);
        this.crawledRevisionChild = createSubParent(yesterdayEvening);
        this.crawledRevisionChildOfChild = createSubSubSequent1(yesterdayNight, List.of("a"));
        this.crawledRevisionChildOfChild2 = createSubSubSequent2(yesterdayNight, List.of("b"));
        this.crawledRevisionThirdGenerationChild = createThirdGenChild(now);

        List<CrawledRevision> unchangedNextGenChild =
                createUnchangedNextGenChild(null, 200, List.of("a", "b"), "X");
        List<CrawledRevision> lastCommit10YearsInFuture =
                createUnchangedNextGenChild(lastCommitTime, 1, List.of("a", "b"), "E");

        // ##### Add Revisions to list in a not accurate order
        crawledRevisionList.add(crawledRevisionChildOfChild);
        crawledRevisionList.add(crawledRevisionChildOfChild2);
        crawledRevisionList.add(crawledRevisionChild);
        crawledRevisionList.add(crawledRevisionThirdGenerationChild);
        crawledRevisionList.add(crawledRevisionParent);

        for (CrawledRevision crawledRevision : unchangedNextGenChild) {
            crawledRevisionList.add(crawledRevision);
        }

        for (CrawledRevision lastCommitCrawledRevision : lastCommit10YearsInFuture) {
            crawledRevisionList.add(lastCommitCrawledRevision);
        }

        return crawledRevisionList;
    }

    private CrawledRevision createParent(LocalDateTime yesterdayMorning) {
        // ##### 1) init parent revision #####
        List<CrawledProject> projectParentList = new ArrayList<>();
        List<CrawledDependency> dependencyParentList = new ArrayList<>();
        dependencyParentList.add(new CrawledDependency("com.test", "kafka", "1", true, null, new ArrayList<>()));
        projectParentList.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                "unittest", "0.1", "someProjectPath", dependencyParentList, false));
        CrawledRevision crawledRevisionParent = new CrawledRevision("parentId01", RevisionType.commit,
                "unittest", yesterdayMorning, LocalDateTime.now(), projectParentList);
        crawledRevisionParent.setBranchList(List.of("a", "b"));
        return crawledRevisionParent;
    }

    private CrawledRevision createSubParent(LocalDateTime yesterdayEvening) {
        // ##### 2) init subsequent revision #####
        List<CrawledProject> projectChildList = new ArrayList<>();
        List<CrawledDependency> dependencyChildList = new ArrayList<>();
        dependencyChildList.add(new CrawledDependency("com.test", "kafka", "1", true, null, new ArrayList<>()));
        dependencyChildList.add(new CrawledDependency("com.test", "spark", "5", true, null, new ArrayList<>()));
        projectChildList.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                "unittest", "0.2", "someProjectPath", dependencyChildList, false));
        CrawledRevision crawledRevisionChild = new CrawledRevision("subseqId01", RevisionType.commit,
                "unittest", yesterdayEvening, LocalDateTime.now(), projectChildList);
        crawledRevisionChild.setBranchList(List.of("a", "b"));

        return crawledRevisionChild;
    }

    private CrawledRevision createSubSubSequent1(LocalDateTime yesterdayNight, List<String> branches) {
        // ##### 3) init subsubsequent revision nr. 1 #####
        List<CrawledProject> projectChildOfChildList = new ArrayList<>();
        List<CrawledDependency> dependencyChildOfChildList = new ArrayList<>();
        dependencyChildOfChildList.add(new CrawledDependency("com.test", "kafka", "3", true, null, new ArrayList<>()));
        dependencyChildOfChildList.add(new CrawledDependency("com.test", "flink", "8", true, null, new ArrayList<>()));
        projectChildOfChildList.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                "unittest", "0.3", "someProjectPath", dependencyChildOfChildList, false));
        CrawledRevision crawledRevisionChildOfChild = new CrawledRevision("subseqId02-1", RevisionType.commit,
                "unittest", yesterdayNight, LocalDateTime.now(), projectChildOfChildList);
        crawledRevisionChildOfChild.setBranchList(branches);

        return crawledRevisionChildOfChild;
    }

    private CrawledRevision createSubSubSequent2(LocalDateTime yesterdayNight, List<String> branches) {
        // ##### 5) init subsubsequent revision nr. 2 #####
        List<CrawledProject> projectChildOfChildList2 = new ArrayList<>();
        List<CrawledDependency> dependencyChildOfChildList2 = new ArrayList<>();
        dependencyChildOfChildList2.add(new CrawledDependency("com.test", "kafka", "1", true, null, new ArrayList<>()));
        dependencyChildOfChildList2.add(new CrawledDependency("com.test", "spark", "5", true, null, new ArrayList<>()));
        projectChildOfChildList2.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                "unittest", "0.2", "someProjectPath", dependencyChildOfChildList2, false));
        CrawledRevision crawledRevisionChildOfChild2 = new CrawledRevision("subseqId02-2", RevisionType.commit,
                "unittest", yesterdayNight, LocalDateTime.now(), projectChildOfChildList2);
        crawledRevisionChildOfChild2.setBranchList(branches);

        return crawledRevisionChildOfChild2;
    }

    private CrawledRevision createThirdGenChild(LocalDateTime now) {
        // ##### 4) init subsubsubsequent revision #####
        List<CrawledProject> projectThirdGenerationChildList = new ArrayList<>();
        List<CrawledDependency> dependencyThirdGenerationChildList = new ArrayList<>();
        dependencyThirdGenerationChildList.add(new CrawledDependency("com.test", "kafka", "3", true, null, new ArrayList<>()));
        dependencyThirdGenerationChildList.add(new CrawledDependency("com.test", "flink", "8", true, null, new ArrayList<>()));
        projectThirdGenerationChildList.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                "unittest", "0.4", "someProjectPath", dependencyThirdGenerationChildList, false));
        CrawledRevision crawledRevisionThirdGenerationChild = new CrawledRevision("subseqId03", RevisionType.commit,
                "unittest", now, LocalDateTime.now(), projectThirdGenerationChildList);
        crawledRevisionThirdGenerationChild.setBranchList(List.of("a"));

        return crawledRevisionThirdGenerationChild;
    }

    /**
     * Create a series of test-data.
     *
     * @param commitTime - If null, a variable committime is used, be calling LocalDateTime.now() and adding an hour for every revision to create
     * @param amount - Amount of equal Revisions to create
     * @param branches - Branches in which the Revisions should be placed
     * @return
     */
    private List<CrawledRevision> createUnchangedNextGenChild(LocalDateTime commitTime, int amount, List<String> branches, String commitIdSuffix) {
        List<CrawledRevision> crawledRevisions = new ArrayList<>();
        for (String branch : branches) {
            for (int amountCounter = 0; amountCounter < amount; amountCounter++) {
                // ##### init followup revisions #####
                List<CrawledProject> projectThirdGenerationChildList = new ArrayList<>();
                List<CrawledDependency> dependencyThirdGenerationChildList = new ArrayList<>();
                dependencyThirdGenerationChildList.add(new CrawledDependency("com.test", "kafka", "3", true, null, new ArrayList<>()));
                dependencyThirdGenerationChildList.add(new CrawledDependency("com.test", "flink", "8", true, null, new ArrayList<>()));
                projectThirdGenerationChildList.add(new CrawledProject("testProject", "description...", "hm.unit.test",
                        "unittest", "0.4", "someProjectPath", dependencyThirdGenerationChildList, false));

                LocalDateTime t = commitTime == null ? LocalDateTime.now().plusHours(amountCounter) : commitTime;

                CrawledRevision crawledRevision = new CrawledRevision("followupSubseqId-" + branch + commitIdSuffix + amountCounter,
                        RevisionType.commit, "unittest", t, LocalDateTime.now(), projectThirdGenerationChildList);
                crawledRevision.setBranchList(List.of(branch));
                crawledRevisions.add(crawledRevision);
            }
        }
        return crawledRevisions;
    }
}
