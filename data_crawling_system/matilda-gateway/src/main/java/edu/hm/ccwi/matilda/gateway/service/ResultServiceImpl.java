package edu.hm.ccwi.matilda.gateway.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ResultServiceImpl implements ResultService {

    private static final String RESULT_SERVICE_NAME = "MATILDA-RECOMMENDER";

    private final EurekaClient discoveryClient;
    private final RestTemplate restTemplate;

    public ResultServiceImpl(@Qualifier("eurekaClient") EurekaClient discoveryClient, RestTemplate gwRestTemplate) {
        this.discoveryClient = discoveryClient;
        this.restTemplate = gwRestTemplate;
    }

    public ResponseEntity<Object> getProjectProfile(String matildaProjectId) {
        return restTemplate.getForEntity(retrieveBaseUrl() +
                "/project/profile?matildaProjectId=" + matildaProjectId, Object.class);
    }

    public ResponseEntity<Object> requestResultCard(String matildaProjectId) {
        String url = retrieveBaseUrl() + "/project/recommendation/result?matildaProjectId=" + matildaProjectId;
        return restTemplate.getForEntity(url, Object.class);
    }

    public ResponseEntity<Object> requestResultCardTest(String matildaProjectId) {
        String url = retrieveBaseUrl() + "/project/recommendation/result/test?matildaProjectId=" + matildaProjectId;
        return this.restTemplate.getForEntity(url, Object.class);
    }

    private String retrieveBaseUrl() {
        InstanceInfo instanceInfo = discoveryClient.getApplication(RESULT_SERVICE_NAME).getInstances().get(0);
        return "http://" + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
    }
}
