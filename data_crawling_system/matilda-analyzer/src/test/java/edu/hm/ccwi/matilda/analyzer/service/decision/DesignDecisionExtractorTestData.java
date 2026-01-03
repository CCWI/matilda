package edu.hm.ccwi.matilda.analyzer.service.decision;

import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.base.model.enumeration.RevisionType;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

public class DesignDecisionExtractorTestData {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSS");

    private static final String PROJECT_NAME = "SampleGitStructureProject";
    private static final String REPOSITORY_NAME = "CodeMax";
    private static final String ROOT_DIRECTORY = "/home/max/matilda-test3-repos/crawled/";
    private static final String COMMIT_ID_1 = "155d0cd721190556053a529b885761a5481c26f5";
    private static final String COMMIT_ID_2 = "3a458b8ee83028dd5266f00994ead57fbd3c69d9";
    private static final String COMMIT_ID_3 = "8a3934904be97af92ca2453f959778d4c52f6b4e";
    private static final String COMMIT_ID_4 = "6a5702e2bd69be11afb847efc3a17c074cc71ff2";
    private static final String COMMIT_ID_5 = "494c839db992fb260bb5a368227e589b49ab703c";
    public static final String PROJECT_GROUP = "edu.hm.ccwi";
    public static final String PROJECT_ARTIFACT = "sampleGitStructureProject";

    public static CrawledRepository createTestableRepositoryObject() {
        CrawledRepository repository = new CrawledRepository();
        repository.setId(REPOSITORY_NAME + ":" + PROJECT_NAME);
        repository.setRepositoryName(REPOSITORY_NAME);
        repository.setProjectName(PROJECT_NAME);
        repository.setDirectoryPath("/opt/matilda/crawling/" + REPOSITORY_NAME + "/" + PROJECT_NAME);
        repository.setSource(RepoSource.github);
        repository.setRevisionCommitIdList(List.of(COMMIT_ID_1, COMMIT_ID_2, COMMIT_ID_3, COMMIT_ID_4, COMMIT_ID_5));
        return repository;
    }

    /**
     * 155 ->   3a4   -> 494    -> mergedFeatureCommit1    -> 494-subseq    -> mergedFeatureCommit2     -> 494-subseq-subseq    -> 494-mergeCommit
     *                -> 6a5
     *                -> 8a3
     */
    public static List<CrawledRevision> createTestableRevisionObject() {
        List<CrawledRevision> crawledRevisions = new ArrayList<>();

        /**
         * Initial commit
         */
        CrawledRevision crawledRevision1 = new CrawledRevision();
        crawledRevision1.setCommitId(COMMIT_ID_1);
        crawledRevision1.addSubsequentRevision(COMMIT_ID_2);
        crawledRevision1.addBranch("refs/heads/feature1");
        crawledRevision1.addBranch("refs/heads/master");
        crawledRevision1.addBranch("refs/heads/orange");
        crawledRevision1.addBranch("refs/heads/yellow");
        crawledRevision1.setCommitDate(LocalDateTime.parse("2018-04-01T15:42:15.0000", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision1.setDateOfClone(LocalDateTime.parse("2020-09-24T16:09:29.4580", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision1.setDirectoryPath(ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522597335_" + COMMIT_ID_1);
        crawledRevision1.setType(RevisionType.commit);

        List<CrawledProject> crawledProjects1 = new ArrayList<>();
        List<CrawledDependency> crawledDependencies1 = new ArrayList<>();
        crawledDependencies1.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-tomcat", "", true, true, true, false, List.of("server","spring","webserver")));
        crawledDependencies1.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-security", "", true, true, true, false, List.of("security","spring")));
        crawledDependencies1.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-web", "", true, true, true, false, List.of("spring","web")));
        crawledDependencies1.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-actuator", "", true, true, true, false, List.of("spring")));

        crawledProjects1.add(new CrawledProject(PROJECT_NAME, PROJECT_NAME, PROJECT_GROUP, PROJECT_ARTIFACT, "0.1",
                ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522597335_" + COMMIT_ID_1,
                crawledDependencies1, false));
        crawledRevision1.setProjectList(crawledProjects1);


        /**
         * Revision 2: Add Spark (branch point for alternatives)
         */
        CrawledRevision crawledRevision2 = new CrawledRevision();
        crawledRevision2.setCommitId(COMMIT_ID_2);
        crawledRevision2.addSubsequentRevision(COMMIT_ID_3);
        crawledRevision2.addSubsequentRevision(COMMIT_ID_4);
        crawledRevision2.addBranch("refs/heads/master");
        crawledRevision2.setCommitDate(LocalDateTime.parse("2018-04-01T20:09:28.0000", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision2.setDateOfClone(LocalDateTime.parse("2020-09-24T16:09:29.4570", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision2.setDirectoryPath(ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522613368_" + COMMIT_ID_2);
        crawledRevision2.setType(RevisionType.commit);

        List<CrawledProject> crawledProjects2 = new ArrayList<>();
        List<CrawledDependency> crawledDependencies2 = new ArrayList<>();
        crawledDependencies2.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-tomcat", "", true, false, true, false, List.of("server","spring","webserver")));
        crawledDependencies2.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-security", "", true, false, true, false, List.of("security","spring")));
        crawledDependencies2.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-web", "", true, false, true, false, List.of("spring","web")));
        crawledDependencies2.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-actuator", "", true, false, true, false, List.of("spring")));
        crawledDependencies2.add(new CrawledDependency("org.apache.spark", "spark-core_2.11", "2.2.0", true, true, true, false, List.of("spark","bigdata")));

        crawledProjects2.add(new CrawledProject(PROJECT_NAME, PROJECT_NAME, PROJECT_GROUP, PROJECT_ARTIFACT, "0.1",
                ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522613368_" + COMMIT_ID_2,
                crawledDependencies2, false));
        crawledRevision2.setProjectList(crawledProjects2);

        /**
         * Revision 3: Replace Spark with Kafka (branch 1)
         */
        CrawledRevision crawledRevision3 = new CrawledRevision();
        crawledRevision3.setCommitId(COMMIT_ID_3);
        crawledRevision3.addBranch("refs/heads/feature-kafka");
        crawledRevision3.setCommitDate(LocalDateTime.parse("2018-04-02T10:15:00.0000", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision3.setDateOfClone(LocalDateTime.parse("2020-09-24T16:09:29.4560", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision3.setDirectoryPath(ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522658100_" + COMMIT_ID_3);
        crawledRevision3.setType(RevisionType.commit);

        List<CrawledProject> crawledProjects3 = new ArrayList<>();
        List<CrawledDependency> crawledDependencies3 = new ArrayList<>();
        crawledDependencies3.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-tomcat", "", true, false, true, false, List.of("server","spring","webserver")));
        crawledDependencies3.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-security", "", true, false, true, false, List.of("security","spring")));
        crawledDependencies3.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-web", "", true, false, true, false, List.of("spring","web")));
        crawledDependencies3.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-actuator", "", true, false, true, false, List.of("spring")));
        crawledDependencies3.add(new CrawledDependency("org.apache.spark", "spark-core_2.11", "2.2.0", true, false, false, true, List.of("spark","bigdata")));
        crawledDependencies3.add(new CrawledDependency("org.apache.kafka", "kafka-core", "1.1.0", true, true, true, false, List.of("kafka","streaming")));

        crawledProjects3.add(new CrawledProject(PROJECT_NAME, PROJECT_NAME, PROJECT_GROUP, PROJECT_ARTIFACT, "0.1",
                ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522658100_" + COMMIT_ID_3,
                crawledDependencies3, false));
        crawledRevision3.setProjectList(crawledProjects3);

        /**
         * Revision 4: Replace Spark with Flink (branch 2, parallel to rev 3)
         */
        CrawledRevision crawledRevision4 = new CrawledRevision();
        crawledRevision4.setCommitId(COMMIT_ID_4);
        crawledRevision4.addSubsequentRevision(COMMIT_ID_5);
        crawledRevision4.addBranch("refs/heads/feature-flink");
        crawledRevision4.setCommitDate(LocalDateTime.parse("2018-04-03T14:20:00.0000", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision4.setDateOfClone(LocalDateTime.parse("2020-09-24T16:09:29.4550", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision4.setDirectoryPath(ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522759200_" + COMMIT_ID_4);
        crawledRevision4.setType(RevisionType.commit);

        List<CrawledProject> crawledProjects4 = new ArrayList<>();
        List<CrawledDependency> crawledDependencies4 = new ArrayList<>();
        crawledDependencies4.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-tomcat", "", true, false, true, false, List.of("server","spring","webserver")));
        crawledDependencies4.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-security", "", true, false, true, false, List.of("security","spring")));
        crawledDependencies4.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-web", "", true, false, true, false, List.of("spring","web")));
        crawledDependencies4.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-actuator", "", true, false, true, false, List.of("spring")));
        crawledDependencies4.add(new CrawledDependency("org.apache.spark", "spark-core_2.11", "2.2.0", true, false, false, true, List.of("spark","bigdata")));
        crawledDependencies4.add(new CrawledDependency("org.apache.flink", "flink-core", "1.5.0", true, true, true, false, List.of("flink","streaming")));

        crawledProjects4.add(new CrawledProject(PROJECT_NAME, PROJECT_NAME, PROJECT_GROUP, PROJECT_ARTIFACT, "0.1",
                ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522759200_" + COMMIT_ID_4,
                crawledDependencies4, false));
        crawledRevision4.setProjectList(crawledProjects4);

        /**
         * Revision 5: Replace Flink with Blockchain
         */
        CrawledRevision crawledRevision5 = new CrawledRevision();
        crawledRevision5.setCommitId(COMMIT_ID_5);
        crawledRevision5.addBranch("refs/heads/feature-blockchain");
        crawledRevision5.setCommitDate(LocalDateTime.parse("2018-04-04T16:30:00.0000", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision5.setDateOfClone(LocalDateTime.parse("2020-09-24T16:09:29.4540", DATE_FORMATTER.withResolverStyle(ResolverStyle.STRICT)));
        crawledRevision5.setDirectoryPath(ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522853400_" + COMMIT_ID_5);
        crawledRevision5.setType(RevisionType.commit);

        List<CrawledProject> crawledProjects5 = new ArrayList<>();
        List<CrawledDependency> crawledDependencies5 = new ArrayList<>();
        crawledDependencies5.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-tomcat", "", true, false, true, false, List.of("server","spring","webserver")));
        crawledDependencies5.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-security", "", true, false, true, false, List.of("security","spring")));
        crawledDependencies5.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-web", "", true, false, true, false, List.of("spring","web")));
        crawledDependencies5.add(new CrawledDependency("org.springframework.boot", "spring-boot-starter-actuator", "", true, false, true, false, List.of("spring")));
        crawledDependencies5.add(new CrawledDependency("org.apache.flink", "flink-core", "1.5.0", true, false, false, true, List.of("flink","streaming")));
        crawledDependencies5.add(new CrawledDependency("org.apache.blockchain", "blockchain-core", "2.0.0", true, true, true, false, List.of("blockchain","distributed")));

        crawledProjects5.add(new CrawledProject(PROJECT_NAME, PROJECT_NAME, PROJECT_GROUP, PROJECT_ARTIFACT, "0.1",
                ROOT_DIRECTORY + REPOSITORY_NAME + "/" + PROJECT_NAME + "/commits/1522853400_" + COMMIT_ID_5,
                crawledDependencies5, false));
        crawledRevision5.setProjectList(crawledProjects5);

        crawledRevisions.add(crawledRevision1);
        crawledRevisions.add(crawledRevision2);
        crawledRevisions.add(crawledRevision3);
        crawledRevisions.add(crawledRevision4);
        crawledRevisions.add(crawledRevision5);

        return crawledRevisions;
    }
}
