package edu.hm.ccwi.matilda.runner.crawler;

import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.*;

import java.util.*;

/**
 * Runner to recrawl, reextract and reanalyze Data.
 * It can be configurated to recrawl only projects having a specific MatildaStatusCode.
 */
public class ReRunner extends AbstractRunner {

    private static final String STATE_TO_CRAWL = "142"; //"902"; // Provide a MatildaStatusCode to recrawl only specific projects

    public static void main(String[] args) throws InterruptedException {
        new ReRunner().runRerun();
    }

    private void runRerun() throws InterruptedException {
        ResponseEntity<List> response = restTemplate.exchange(GATEWAY_PROJECT_PROFILE_LIST_URL, HttpMethod.GET,
                new HttpEntity<>(new HttpHeaders()), List.class);

        if(response.getStatusCode() != HttpStatus.OK) {
            System.err.println("Rerun stopped, since request for project profiles failed.");
            System.err.println("--> Gateway request resulted in: " + response.getStatusCode().value());
        }

        for (Object o : response.getBody()) {
            if(o instanceof LinkedHashMap) {
                Map<String, String> pp = (LinkedHashMap) o;
                String uri = pp.get("uri");
                String status = pp.get("status");
                String matildaRequestId = pp.get("matildaRequestId");

                // check if profile includes: url, id, state - generate id if needed
                if(uri == null || status == null) {
                    System.err.println("Could not process project profile for status: " + status + " uri: " + uri + " -> skip.");
                    continue;
                }

                if(STATE_TO_CRAWL != null &&
                        !StringUtils.equalsIgnoreCase(status, MatildaStatusCode.getMatildaStatusCode(Integer.valueOf(STATE_TO_CRAWL)).toString())) {
                    //System.err.println("Not processing project profile because of status: " + status + " -> skip.");
                    continue;
                }

                if(StringUtils.isEmpty(matildaRequestId)) { matildaRequestId = UUID.randomUUID().toString(); }

                // execute rerun for project profile
                System.out.println("Sending request: " + matildaRequestId + "  ::  " + uri);
                ResponseEntity<Map> analyzeResponse = requestGatewayToAnalyzeProject(uri, matildaRequestId, restTemplate, GATEWAY_ANALYZE_PROJECT_URL);
                if (analyzeResponse != null && analyzeResponse.getStatusCode() != HttpStatus.OK) {
                    System.err.println("Request to analyze project profile failed for: " + uri + " -> ignore and continue.");
                    continue;
                }
            } else {
                System.err.println("received object is not of type project profile -> skip.");
                continue;
            }

            Thread.sleep(500); // SINCE MY DISCOVERY-SERVICE IS BREAKING DOWN LOCALLY WHEN I RUN THE RERUNNER - THIS MIGHT HELP!
        }
    }
}