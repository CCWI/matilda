package edu.hm.ccwi.matilda.runner.crawler;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.hm.ccwi.matilda.runner.config.RunnerConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.http.*;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;

/**
 * Normal Runner to crawl, extract and analyze Data.
 */
public class Runner extends AbstractRunner {

    public static void main(String[] args) throws InterruptedException {
        new RunnerConfig("configuration.properties");
        new Runner().runRunner();
    }

    /**
     * TODO:
     * 1) Get Settings for Crawling
     * 2) callCrawlerForReponames()
     * -) Check State-Service for Project Profile (NEIN! WIE EIN CLIENT KANN ICH JA EINFACH ÜBERGEBEN UND IM BLÖDSTEN FALL KOMMT VOM GW ZURÜCK, DASS DAS NICHT GEHT -> HANDLE!
     * 3) Create call to GW for crawling each
     * 4) Handle errors + logging
     * 5) Extend configuration + cleanup existing config + remove old runner-classes + remove unneeded dependencies!
     * 6) START RUNNER (AFTER COMPLETING RERUNNER !!!!!!!!!!!!!!!!)
     */
    private void runRunner() throws InterruptedException {

        for (String searchText : RunnerConfig.crawlerSearchText) {                                                          // matilda.crawler.searchText
            for (String crawlerOrder : RunnerConfig.crawlerOrder) {
                System.out.println("Crawling topic: " + searchText);

                for (int crawlingCounter = 0; crawlingCounter < RunnerConfig.crawlerAmountOfRepos; crawlingCounter++) {     // matilda.crawler.amountOfRepos
                    int pageToCrawl = 1 + crawlingCounter / RunnerConfig.pageAmount;                                        // matilda.crawler.pageAmount
                    System.out.println("__Start crawling page : " + pageToCrawl + " / " + RunnerConfig.pageAmount);

                    try {
                        List<String> githubUrlList = getGithubUrlList(searchText, RunnerConfig.crawlerProgrammingLanguage,
                                pageToCrawl, RunnerConfig.pageAmount, RunnerConfig.crawlerResultSort, crawlerOrder);

                        if (CollectionUtils.isEmpty(githubUrlList)) {
                            System.err.println("__Could not process projects, because of an empty url-list.");
                            break;
                        }

                        // execute run for project in list
                        for (String uri : githubUrlList) {
                            executeRequestForUri(uri);
                            Thread.sleep(400);
                        }
                        crawlingCounter = crawlingCounter + RunnerConfig.pageAmount;
                        Thread.sleep(4000);

                    } catch (IOException e) {
                        System.err.println("__Error while retrieving repository list." + e.getMessage());
                        break;
                    }
                }
            }
            Thread.sleep(10000);
        }
    }

    private void executeRequestForUri(String uri) throws InterruptedException {
        String matildaRequestId = UUID.randomUUID().toString();
        System.out.println("____Sending request: " + matildaRequestId + "  ::  " + uri);
        ResponseEntity<Map> response = requestGatewayToAnalyzeProject(uri, matildaRequestId, restTemplate, GATEWAY_ANALYZE_PROJECT_URL);
        if (!response.getStatusCode().is2xxSuccessful()) {
            System.err.println("____Request to analyze project profile failed for: " + uri + " -> ignore and continue.");
            return;
        }
    }

    public List<String> getGithubUrlList(String queryText, String language, Integer page, Integer perPage,
                                         String sort, String order) throws IOException, NullPointerException {
        HttpResponse result = null;
        List<String> repoList = new ArrayList<>();
        try {
            HttpGet request = new HttpGet(GH_SEARCH_URL + "?q=" + queryText + "+language:" + language
                    + "&page=" + page + "&per_page=" + perPage + "&sort=" + sort + "&order=" + order);
            request.addHeader("content-type", "application/json");
            result = HttpClientBuilder.create().build().execute(request);
            JsonElement jelement = JsonParser.parseString(EntityUtils.toString(result.getEntity(), "UTF-8"));

            if (jelement instanceof JsonObject & jelement != null) {
                JsonObject jobject = jelement.getAsJsonObject();
                jelement = jobject.get("items");
            }
            if (jelement != null) {
                JsonArray jarr = jelement.getAsJsonArray();
                if (jarr != null) {
                    for (int i = 0; i < jarr.size(); i++) {
                        String htmlUrl = ((JsonObject) jarr.get(i)).get("html_url").toString(); // full_name
                        String strippedUrl = htmlUrl.replace("\"", "");
                        repoList.add(strippedUrl);
                    }
                }
            }
        } finally {
            if (result != null) {
                ((CloseableHttpResponse) result).close();
            }
        }
        return repoList;
    }
}