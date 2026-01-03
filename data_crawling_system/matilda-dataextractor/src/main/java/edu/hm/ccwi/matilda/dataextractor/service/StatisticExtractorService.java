package edu.hm.ccwi.matilda.dataextractor.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledStatistic;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledStatisticRepository;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Extract data from project including dependency-details and documentation.
 *
 * @author Max.Auch
 */
@Service
public class StatisticExtractorService {

    private static final Logger LOG = LoggerFactory.getLogger(StatisticExtractorService.class);

    private final CrawledStatisticRepository statisticRepo;

    public StatisticExtractorService(CrawledStatisticRepository statisticRepo) {
        this.statisticRepo = statisticRepo;
    }

    public void saveStatistic(CrawledStatistic crawledStatistic) {
        statisticRepo.save(crawledStatistic);
        LOG.info("SAVED crawling-statistic to database");
    }

    CrawledStatistic createCrawledStatistic(String repoName, String projectName, RepoSource source, Git git,
                                            List<CrawledRevision> revList) throws GitAPIException {
        CrawledStatistic crawledStatistic = new CrawledStatistic(repoName, projectName, source.name());
        if (statisticRepo.existsById(crawledStatistic.getStatisticId())) {
            crawledStatistic = statisticRepo.findById(crawledStatistic.getStatisticId()).get();
        }
        crawledStatistic.setNumberOfBranches(git.branchList().call().size());
        crawledStatistic.setNumberOfRevisions(revList.size());
        crawledStatistic.setAverageNumberOfMvnProjectsInRevisions(calculateAverageNumberOfMvnProjects(revList));
        crawledStatistic.setNumberOfDependeciesInFirstRevision(calculateNumberOfDependencies(revList.get(0).getProjectList()));
        crawledStatistic.setNumberOfDependeciesInLastRevision(calculateNumberOfDependencies(Iterables.getLast(revList).getProjectList()));
        return crawledStatistic;
    }

    void updateCrawledStatistic(List<CrawledRevision> revList, CrawledStatistic crawledStatistic) {
        crawledStatistic.setNumberOfRelevantDependeciesInFirstRevision(calculateNumberOfDependencies(revList.get(0).getProjectList()));
        crawledStatistic.setNumberOfRelevantDependeciesInLastRevision(calculateNumberOfDependencies(Iterables.getLast(revList).getProjectList()));
    }

    Integer calculateNumberOfDependencies(List<CrawledProject> projectList) {
        int numberOfdependencies = 0;
        for(CrawledProject project : projectList) {
            numberOfdependencies = numberOfdependencies + project.getDependencyList().size();
        }
        return numberOfdependencies;
    }

    private Integer calculateAverageNumberOfMvnProjects(List<CrawledRevision> revList) {
        int numberOfLibraries = 0;
        for(CrawledRevision rev : revList) {
            numberOfLibraries = numberOfLibraries + rev.getProjectList().size();
        }

        return numberOfLibraries / revList.size();
    }
}