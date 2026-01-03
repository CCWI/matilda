package edu.hm.ccwi.matilda.gateway.rest;

import edu.hm.ccwi.matilda.gateway.service.AnalyzeService;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/gw")
@CrossOrigin(origins = "*")
public class AnalyzeResource {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeResource.class);

    final AnalyzeService analyzeProject;

    public AnalyzeResource(AnalyzeService analyzeProject) {
        this.analyzeProject = analyzeProject;
    }

    @PostMapping(value = "/analyze/project", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> analyzeProject(@RequestBody Map<String, String> requestParams) throws Exception {
        try {
            String url = parseRequestBodyFor(requestParams, "url");
            if(StringUtils.isEmpty(url)) { throw new Exception("Request URL is empty - deny request"); }
            String matildaId = parseRequestBodyFor(requestParams, "matildaId");
            return Collections.singletonMap("mId", analyzeProject.analyzeProject(url, matildaId));
        } catch (Exception e) {
            LOG.error("Error occurred while sending message: {}", e.getMessage());
            throw new Exception("Error occurred while forwarding a message.");
        }
    }

    @GetMapping(value = "/")
    private String parseRequestBodyFor(Map<String, String> requestParams, String searchString) throws Exception {
        String requestValue = null;
        if(MapUtils.isNotEmpty(requestParams) && requestParams.containsKey(searchString)) {
            requestValue = requestParams.get(searchString);
        } else {
            throw new Exception(searchString + " not found in request");
        }
        return requestValue;
    }
}