package edu.hm.ccwi.matilda.gateway.rest;

import edu.hm.ccwi.matilda.gateway.service.ResultService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/gw")
@CrossOrigin(origins = "*")
public class ResultResource {

    private static final Logger LOG = LoggerFactory.getLogger(ResultResource.class);

    final ResultService resultService;

    public ResultResource(ResultService resultService) {
        this.resultService = resultService;
    }

    @GetMapping(value = "/project/profile", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getProjectProfile(@RequestParam(value = "matildaRequestId") String matildaRequestId) {
        LOG.info("Received project profile request for UUID: " + matildaRequestId);
        return resultService.getProjectProfile(matildaRequestId);
    }

    @GetMapping(value = "/project/recommendation/result", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getResultCard(@RequestParam(value = "matildaRequestId") String matildaRequestId) {
        LOG.info("Received result request for UUID: " + matildaRequestId);
        return resultService.requestResultCard(matildaRequestId);
    }

    @GetMapping(value = "/project/recommendation/result/test", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Object> getTestResultCard(@RequestParam(value = "matildaRequestId") String matildaRequestId) {
        LOG.info("Received result request for UUID: " + matildaRequestId);
        return resultService.requestResultCardTest(matildaRequestId);
    }
}