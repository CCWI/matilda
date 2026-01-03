package edu.hm.ccwi.matilda.crawler.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Max.Auch
 */
@Service
public class CrawlerGHService extends GenericCrawlerService {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerGHService.class);
    private static final String GH_SOURCE = "github.source";
    private static final String GH_SEARCH_URL = "https://api.github.com/search/repositories";

    public CrawlerGHService(JanitorService janitorService) {
        super(janitorService);
    }

    private GHRepository getRepository(String repoName) {
        GitHub github;
        GHRepository repo = null;
        try {
            // Use environment variable GITHUB_TOKEN for authentication
            String githubToken = System.getenv("GITHUB_TOKEN");
            if (githubToken != null && !githubToken.isEmpty()) {
                github = GitHub.connectUsingOAuth(githubToken);
            } else {
                github = GitHub.connectAnonymously();
                LOG.warn("No GITHUB_TOKEN found, connecting anonymously with rate limits");
            }
            LOG.info("Start connecting to github.getRepository(" + repoName + ")");
            repo = github.getRepository(repoName);
            LOG.info("Connection to github.getRepository(" + repoName + ") established");
        } catch (IOException e) {
            LOG.error("Error occurred while cloning. Getting the repository brought up the following error and will " +
                    "therefore be ignored: {}", e.getMessage());
        }
        return repo;
    }

    public String cloneRepoStructure(@NotEmpty String repoProjName, @NotEmpty String rootPath, boolean runJanitor,
                                     LocalDate dateSince) {
        LOG.info("Clone repository structure {} since date: {}", repoProjName, dateSince);
        GHRepository repo = getRepository(repoProjName);
        if (repo != null) {
            return crawlRemoteRepo(repoProjName, rootPath, runJanitor, dateSince, GH_SOURCE, repo.getHttpTransportUrl());
        }
        return null;
    }

    public List<String> getRepoList(String queryText, String language, Integer page, Integer perPage,
                                    String sort, String order) throws IOException, NullPointerException {
        CloseableHttpResponse result = null;
        List<String> repoList = new ArrayList<>();
        try {
            HttpGet request = new HttpGet(GH_SEARCH_URL + "?q=" + queryText + "+language:" + language
                    + "&page=" + page + "&per_page=" + perPage + "&sort=" + sort + "&order=" + order);
            request.addHeader("content-type", "application/json");
            result = HttpClientBuilder.create().build().execute(request);
            JsonElement jelement = JsonParser.parseString(EntityUtils.toString(result.getEntity(), "UTF-8"));

            if (jelement instanceof JsonObject) {
                JsonObject jobject = jelement.getAsJsonObject();
                jelement = jobject.get("items");
            }
            if (jelement != null) {
                JsonArray jarr = jelement.getAsJsonArray();
                if (jarr != null) {
                    for (int i = 0; i < jarr.size(); i++) {
                        String fullName = ((JsonObject) jarr.get(i)).get("full_name").toString();
                        repoList.add(fullName.substring(1, fullName.length() - 1));
                    }
                }
            }
        } finally {
            if (result != null) {
                result.close();
            }
        }
        return repoList;
    }
}