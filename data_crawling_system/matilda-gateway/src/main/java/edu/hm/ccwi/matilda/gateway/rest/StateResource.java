package edu.hm.ccwi.matilda.gateway.rest;

import edu.hm.ccwi.matilda.gateway.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gw")
@CrossOrigin(origins = "*")
public class StateResource {

    private static final Logger LOG = LoggerFactory.getLogger(StateResource.class);

    private final StateService stateService;

    public StateResource(StateService stateService) {
        this.stateService = stateService;
    }

    @GetMapping(value = "/state/project/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public List stateProjectProfileList() throws Exception {
        try {
            return stateService.stateProjectProfiles();
        } catch (Exception e) {
            LOG.error("Error occurred while retrieving project profile list: {}", e.getMessage());
            throw new Exception("Error occurred while retrieving project profiles.");
        }
    }
}