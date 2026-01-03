package edu.hm.ccwi.matilda.state.rest;

import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.jpa.service.ProjectProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/project")
public class ProjectProfileResource {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectProfileResource.class);

    final ProjectProfileService projectProfileService;

    public ProjectProfileResource(ProjectProfileService projectProfileService) {
        this.projectProfileService = projectProfileService;
    }

    @RequestMapping(value = "/profile/{mid}", method = RequestMethod.GET)
    public ResponseEntity<ProjectProfile> getProjectByMID(@PathVariable("mid") String matildaProjectId) {
        LOG.info("Incoming request for analyzed project profile: {}", matildaProjectId);
        try {
            ProjectProfile projectProfile = projectProfileService.getProfileOfAnalyzedProject(matildaProjectId);
            return ResponseEntity.ok(projectProfile);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public ResponseEntity<ProjectProfile> getProjectByRepoProj(@RequestParam(value = "repositoryName") String repositoryName,
                                                               @RequestParam(value = "projectName") String projectName) {
        LOG.info("Incoming request for analyzed project profile: {}/{}", repositoryName, projectName);
        try {
            return ResponseEntity.ok(projectProfileService.getProfileOfAnalyzedProject(repositoryName, projectName));
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/profiles-of-state", method = RequestMethod.GET)
    public ResponseEntity<List<ProjectProfile>> getProjectByState(@RequestParam(value = "matildaState") String matildaState) {
        LOG.info("Incoming request for all analyzed project profiles with current state: {}", matildaState);
        try {
            return ResponseEntity.ok(projectProfileService.getProfileOfAnalyzedProjectByState(matildaState));
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/profiles", method = RequestMethod.GET)
    public ResponseEntity<List<ProjectProfile>> getAllProjectProfiles() {
        LOG.info("Incoming request for all project profiles");
        try {
            List<ProjectProfile> projectProfile = projectProfileService.getProfileListOfAnalyzedProjects();
            LOG.info("Returning a list of {} projectprofiles.", projectProfile.size());
            return ResponseEntity.ok(projectProfile);
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping(value = "/profile")
    public ResponseEntity<String> saveOrUpdateProjectProfile(@RequestBody ProjectProfile projectProfile) {
        try {
            LOG.trace("New create or update request for project profile: {}", projectProfile.getMatildaRequestId());
            String matildaRequestId = projectProfileService.saveOrUpdateProjectProfile(projectProfile);
            return ResponseEntity.ok(matildaRequestId);
        } catch(Exception e) {
            LOG.error("An error occurred: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @PutMapping(value = "/profile/state")
    public ResponseEntity<String> updateProjectState(@RequestParam(value = "repositoryName") String repositoryName,
                                                     @RequestParam(value = "projectName") String projectName,
                                                     @RequestParam(value = "matildaStatusId") Integer matildaStatusId) {
        LOG.info("New update status {} request project: {}/{}", matildaStatusId, repositoryName, projectName);
        try {
            String matildaRequestId = projectProfileService.updateProjectState(repositoryName, projectName, matildaStatusId);
            return ResponseEntity.ok(matildaRequestId);
        } catch(Exception e) {
            // This handling is not totally correct, since not all exceptions (like the thrown NotFoundException) should
            // lead to an internal server error. But it simplifies in the current state of MATILDA and could be fixed in the future.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}