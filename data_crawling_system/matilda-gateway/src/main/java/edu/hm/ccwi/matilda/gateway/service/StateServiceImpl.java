package edu.hm.ccwi.matilda.gateway.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import edu.hm.ccwi.matilda.gateway.exception.GatewayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class StateServiceImpl implements StateService {

    private static final Logger LOG = LoggerFactory.getLogger(StateService.class);
    private static final String APPLICATION_NAME = "MATILDA-STATE";
    private static final String PROTOCOL = "http://";

    private final EurekaClient discoveryClient;
    private final HttpEntity<String> entity;

    private final RestTemplate restTemplate;

    public StateServiceImpl(@Qualifier("eurekaClient") EurekaClient discoveryClient) {
        this.discoveryClient = discoveryClient;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        this.entity = new HttpEntity<>(headers);
        this.restTemplate = new RestTemplate();
    }

    /**
     * Create Matilda Request
     */
    public List stateProjectProfiles() throws GatewayException {
        LOG.info("Retrieving project profile list");
        String url = retrieveBaseUrl() + "/project/profiles";

        ResponseEntity<ArrayList> response = this.restTemplate.exchange(url, HttpMethod.GET, entity, ArrayList.class);
        if(response.getStatusCode().is5xxServerError()) {
            throw new GatewayException("State service error occurred.");
        }
        return response.getBody();
    }

    private String retrieveBaseUrl() {
        InstanceInfo instanceInfo = this.discoveryClient.getApplication(APPLICATION_NAME).getInstances().get(0);
        return PROTOCOL + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
    }
}
