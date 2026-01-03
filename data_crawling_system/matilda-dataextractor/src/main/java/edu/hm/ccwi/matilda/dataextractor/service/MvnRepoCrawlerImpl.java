package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

@Service
public class MvnRepoCrawlerImpl implements MvnRepoCrawler {

    private static final Logger LOG = LoggerFactory.getLogger(MvnRepoCrawlerImpl.class);
    private static final HttpClient httpClient = HttpClient.newBuilder().version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10)).build();

    /**
     * Temporal cache of dependencies, which were not available for crawling once and shouldn't be crawled multiple times
     */
    private final Set<String> notFoundDependencyGACache = new HashSet<>();

    public MvnRepoPage crawlMvnRepo(CrawledDependency cDependency) {
        String htmlPage = getValidMvnRepoHtmlPage(cDependency.getGroup(), cDependency.getArtifact());

        String category = "";
        String[] tagMatches = null;

        if (StringUtils.isNotEmpty(htmlPage)) {
            String[] categoryArray = (Pattern.compile("(?<=class=\"b c\">)(.+?)(?=</a>)").matcher(htmlPage)
                    .results().map(MatchResult::group).toArray(String[]::new));
            if (ArrayUtils.isNotEmpty(categoryArray)) {
                category = categoryArray[0];
            }
            tagMatches = Pattern.compile("(?<=class=\"b tag\">)(.+?)(?=</a>)").matcher(htmlPage)
                    .results().map(MatchResult::group).toArray(String[]::new);
        }
        return new MvnRepoPage(htmlPage, category, tagMatches);
    }

    public boolean isCrawledMvnRepoDependencyUncategorizedAndUntagged(MvnRepoPage mvnRepoPage) {
        return mvnRepoPage == null || (org.codehaus.plexus.util.StringUtils.isEmpty(mvnRepoPage.getCategory()) &&
                (mvnRepoPage.getTagMatches() == null || ArrayUtils.isEmpty(mvnRepoPage.getTagMatches())));
    }

    private String getValidMvnRepoHtmlPage(String groupId, String artifactId) {

        if (notFoundDependencyGACache.contains(groupId + ":" + artifactId)) {
            LOG.debug("Dependency already searched in this session and not found -> ignore {}:{}", groupId, artifactId);
            return "";
        }

        String htmlPage = "";
        try {
            Thread.sleep(8000 + new Random().nextInt(3000 - 1) + 1L); // prevent mvnrepo from blocking
            htmlPage = getMvnRepoHtmlPage(groupId, artifactId);
        } catch (InterruptedException e1) {
            LOG.error("Thread cannot be send to sleep. It is not tired.", e1);
        } catch (NoSuchElementException e) {
            LOG.warn("Error while accessing MVNRepository-Pages: " + e.getMessage());
            notFoundDependencyGACache.add(groupId + ":" + artifactId);
        } catch (Exception e) {
            LOG.warn("Error while accessing MVNRepository-Pages: " + e.getMessage());
            notFoundDependencyGACache.add(groupId + ":" + artifactId);
        }
        return htmlPage;
    }

    private String getMvnRepoHtmlPage(String groupId, String artifactId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create("https://mvnrepository.com/artifact/" + groupId + "/" + artifactId))
                .setHeader("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:87.0) Gecko/20100101 Firefox/87.0")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 400) {
            return response.body();
        } else if (response.statusCode() == 403) {
            LOG.error("MvnRepository sent FORBIDDEN - Requests might be blocked by now for: " +
                    "https://mvnrepository.com/artifact/" + groupId + "/" + artifactId);
            throw new Exception("MvnRepository sent FORBIDDEN - Requests might be blocked by now for: " +
                    "https://mvnrepository.com/artifact/" + groupId + "/" + artifactId);
        } else {
            throw new NoSuchElementException("MvnRepository URL not available: "
                    + "https://mvnrepository.com/artifact/" + groupId + "/" + artifactId);
        }
    }
}