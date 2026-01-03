package edu.hm.ccwi.matilda.crawler.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
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
public class CrawlerBBService extends GenericCrawlerService {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerBBService.class);

    private static final String BB_SOURCE = "bitbucket.source";
    private static final String BB_SEARCH_URL = "https://api.bitbucket.org/2.0/repositories";
    private static final String BB_CLONE_URL = "https://bitbucket.org/<NAMESPACE>/<PROJECTNAME>.git";

    public CrawlerBBService(JanitorService janitorService) {
        super(janitorService);
    }

    public String cloneRepoStructure(@NotEmpty String repoName, @NotEmpty String rootPath,
                                     boolean runJanitor, LocalDate dateSince) {
        String cloneUri = BB_CLONE_URL.replace("<NAMESPACE>/<PROJECTNAME>", repoName).replace(" ", "");
        if (cloneUri != null) {
            return crawlRemoteRepo(repoName, rootPath, runJanitor, dateSince, BB_SOURCE, cloneUri);
        }
        return null;
    }

    public List<String> getRepoList(String queryText, String language, Integer page, Integer perPage,
                                    String sort, String order) throws IOException, NullPointerException {
        // https://api.bitbucket.org/2.0/repositories?q=crawler&page=3&pagelen=3&lang=java
        HttpGet request = new HttpGet(BB_SEARCH_URL + "?page=" + page + "&pagelen=" + perPage +
                "&lang=" + language);
        request.addHeader("content-type", "application/json");
        HttpResponse result = HttpClientBuilder.create().build().execute(request);
        JsonElement jelement = JsonParser.parseString(EntityUtils.toString(result.getEntity(), "UTF-8"));

        List<String> repoList = new ArrayList<>();
        if (jelement instanceof JsonObject && jelement != null) {
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
        return repoList;
    }
}