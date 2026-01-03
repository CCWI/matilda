package edu.hm.ccwi.matilda.crawler.client;

import com.netflix.discovery.EurekaClient;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.base.util.DiscoveryClientManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class CrawlerStateHandler extends DiscoveryClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerStateHandler.class);

    private final RestTemplate restTemplate;
    private final HttpEntity<String> defaultHttpEntity;

    public CrawlerStateHandler(@Qualifier("eurekaClient") EurekaClient discoveryClient, RestTemplate restTemplate) {
        super(discoveryClient);
        this.restTemplate = restTemplate;
        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        this.defaultHttpEntity = new HttpEntity<>(defaultHeaders);
    }

    /**
     * Call service "matilda-state"
     */
    public ProjectProfile getProjectProfileByRepoProj(String repositoryName, String projectName) throws RestClientException {
        String url = retrieveBaseUrl(MATILDA_STATE_NAME) + "/project/profile?repositoryName=" + repositoryName +
                "&projectName=" + projectName;
        LOG.info("  Calling Project Profile: {}", url);

        ResponseEntity<ProjectProfile> response = restTemplate.exchange(url, HttpMethod.GET, defaultHttpEntity, ProjectProfile.class);
        checkResponseOnErrors(response);
        return response.getBody();
    }

    public void saveOrUpdateProjectProfile(ProjectProfile projectProfile) throws RestClientException {
        String url = retrieveBaseUrl(MATILDA_STATE_NAME) + "/project/profile";

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, new HttpEntity<>(projectProfile, new HttpHeaders()), String.class);
        checkResponseOnErrors(response);
    }

    public void updateStatusOfProjectProfile(String repoName, String projectName, MatildaStatusCode matildaStatusCode) throws RestClientException {
        LOG.debug("start updating status of project profile -> send request for: {}:{}/{}", repoName, projectName, matildaStatusCode);
        String url =  retrieveBaseUrl(MATILDA_STATE_NAME) + "/project/profile/state?repositoryName=" + repoName +
                "&projectName=" + projectName + "&matildaStatusId=" + matildaStatusCode.getStatusCode();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, null, String.class);
        LOG.debug("received response of status service: {} -> {}", response.getStatusCode(), response.getBody());
        checkResponseOnErrors(response);
    }
}
