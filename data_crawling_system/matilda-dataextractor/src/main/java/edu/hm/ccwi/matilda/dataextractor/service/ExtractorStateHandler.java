package edu.hm.ccwi.matilda.dataextractor.service;

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
public class ExtractorStateHandler extends DiscoveryClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractorStateHandler.class);

    private final RestTemplate restTemplate;
    private final HttpEntity<String> defaultHttpEntity;

    public ExtractorStateHandler(@Qualifier("eurekaClient") EurekaClient discoveryClient, RestTemplate restTemplate) {
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
        HttpEntity<ProjectProfile> requestEntity = new HttpEntity<>(projectProfile, new HttpHeaders());
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        checkResponseOnErrors(response);
    }

    public void updateStatusOfProjectProfile(String repoName, String projectName, MatildaStatusCode matildaStatusCode) throws RestClientException {
        String url =  retrieveBaseUrl(MATILDA_STATE_NAME) + "/project/profile/state?repositoryName=" + repoName +
                "&projectName=" + projectName + "&matildaStatusId=" + matildaStatusCode.getStatusCode();
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, null, String.class);
        checkResponseOnErrors(response);
    }
}
