package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;

import java.util.List;

public interface ProjectProfileService {

    ProjectProfile getProfileOfAnalyzedProject(String matildaProjectId);

    ProjectProfile getProfileOfAnalyzedProject(String repositoryName, String projectName);

    List<ProjectProfile> getProfileOfAnalyzedProjectByState(String matildaState);

    String saveOrUpdateProjectProfile(ProjectProfile projectProfile) throws StateException, StateException;

    String updateProjectState(String repositoryName, String projectName, Integer matildaStatusId);

    List<ProjectProfile> getProfileListOfAnalyzedProjects();
}
