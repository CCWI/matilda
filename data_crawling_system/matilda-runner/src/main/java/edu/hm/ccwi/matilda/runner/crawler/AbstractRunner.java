package edu.hm.ccwi.matilda.runner.crawler;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class AbstractRunner {

    protected static final String GH_SEARCH_URL = "https://api.github.com/search/repositories";
    protected static final String GATEWAY_PROJECT_PROFILE_LIST_URL = "http://localhost:8081/gw/state/project/profiles";
    protected static final String GATEWAY_ANALYZE_PROJECT_URL = "http://localhost:8081/gw/analyze/project";

    protected final RestTemplate restTemplate;

    public AbstractRunner() { this.restTemplate = new RestTemplate(); }

    protected ResponseEntity<Map> requestGatewayToAnalyzeProject(String uri, String matildaRequestId,
                                                       RestTemplate restTemplate, String gatewayAnalyzeProjectUrl) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("url", uri);
        requestParams.put("matildaId", matildaRequestId);

        return restTemplate.exchange(gatewayAnalyzeProjectUrl, HttpMethod.POST,
                new HttpEntity<>(requestParams, new HttpHeaders()), Map.class);
    }
}
