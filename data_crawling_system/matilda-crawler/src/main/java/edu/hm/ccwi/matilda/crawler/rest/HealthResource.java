package edu.hm.ccwi.matilda.crawler.rest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthResource {

    private static final Logger LOG = LoggerFactory.getLogger(HealthResource.class);

    @GetMapping(value = "/ping")
    public ResponseEntity<String> ping() {
        LOG.info("Received PING -> Commandement?");
        return ResponseEntity.ok("commandement?");
    }
}
