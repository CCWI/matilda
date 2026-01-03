package edu.hm.ccwi.matilda.persistence.jpa.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.jpa.repo.ProjectProfileRepository;
import edu.hm.ccwi.matilda.persistence.jpa.util.MappingProjectProfileEntityService;
import edu.hm.ccwi.matilda.persistence.jpa.model.ProjectProfileEntity;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProjectProfileServiceImpl implements ProjectProfileService {

    private static final Logger LOG = LoggerFactory.getLogger(ProjectProfileService.class);

    final ProjectProfileRepository projectProfileRepository;
    final MappingProjectProfileEntityService mappingService;

    public ProjectProfileServiceImpl(ProjectProfileRepository projectProfileRepository, MappingProjectProfileEntityService mappingService) {
        this.projectProfileRepository = projectProfileRepository;
        this.mappingService = mappingService;
    }

    @Override
    public ProjectProfile getProfileOfAnalyzedProject(String matildaProjectId) {
        ProjectProfileEntity ppe = projectProfileRepository.findByMatildaRequestId(matildaProjectId).orElse(null);
        return mappingService.mapProjectProfileEntityToProjectProfile(ppe);
    }

    @Override
    public ProjectProfile getProfileOfAnalyzedProject(String repositoryName, String projectName) {
        ProjectProfileEntity ppe =  projectProfileRepository.findByRepositoryNameAndProjectName(repositoryName, projectName).orElse(null);
        return ppe != null ? mappingService.mapProjectProfileEntityToProjectProfile(ppe) : null;
    }

    @Override
    public List<ProjectProfile> getProfileOfAnalyzedProjectByState(String matildaState) {
        List<ProjectProfile> projectProfileList = new ArrayList<>();
        List<ProjectProfileEntity> ppeList =  projectProfileRepository.findAllByStatus(Integer.valueOf(matildaState));
        LOG.info("Found {} ProjectProfileEntities for request.", Iterables.size(ppeList));
        for (ProjectProfileEntity projectProfileEntity : ppeList) {
            projectProfileList.add(mappingService.mapProjectProfileEntityToProjectProfile(projectProfileEntity));
        }
        return projectProfileList;
    }

    @Override
    public List<ProjectProfile> getProfileListOfAnalyzedProjects() {
        List<ProjectProfile> projectProfileList = new ArrayList<>();
        Iterable<ProjectProfileEntity> all = projectProfileRepository.findAll();
        LOG.info("Found {} ProjectProfileEntities for request.", Iterables.size(all));
        for (ProjectProfileEntity projectProfileEntity : all) {
            projectProfileList.add(mappingService.mapProjectProfileEntityToProjectProfile(projectProfileEntity));
        }
        return projectProfileList;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String saveOrUpdateProjectProfile(ProjectProfile projectProfile) throws StateException {
        if(projectProfile != null && StringUtils.isNotEmpty(projectProfile.getRepositoryName()) &&
                StringUtils.isNotEmpty(projectProfile.getProjectName())) {
            ProjectProfileEntity ppEntity = projectProfileRepository.findByRepositoryNameAndProjectName(projectProfile.getRepositoryName(),
                    projectProfile.getProjectName()).orElse(mappingService.mapProjectProfileToProjectProfileEntity(projectProfile));
            if (StringUtils.equals(ppEntity.getMatildaRequestId(), projectProfile.getMatildaRequestId()) ||
                    StringUtils.isEmpty(ppEntity.getMatildaRequestId())) {
                ppEntity = updateProjectProfileValues(ppEntity, projectProfile);
                return projectProfileRepository.save(ppEntity).getMatildaRequestId();
            } else {
                throw new StateException("ProjectProfile " + projectProfile.getRepositoryName() + "/" + projectProfile.getProjectName()
                        + " already available but matilda request id differs -> update not possible and therefore skipped");
            }
        } else {
            return projectProfileRepository.save(mappingService.mapProjectProfileToProjectProfileEntity(projectProfile)).getMatildaRequestId();
        }

    }

    private ProjectProfileEntity updateProjectProfileValues(ProjectProfileEntity ppEntity, ProjectProfile projectProfile) {
        ppEntity.setStatus(projectProfile.getStatus().getStatusCode());
        ppEntity.setDateOfClone(projectProfile.getDateOfClone());
        ppEntity.setMatildaRequestId(projectProfile.getMatildaRequestId());
        ppEntity.setProjectName(projectProfile.getProjectName());
        ppEntity.setRepositoryName(projectProfile.getRepositoryName());
        ppEntity.setUri(projectProfile.getUri());

        return ppEntity;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String updateProjectState(String repositoryName, String projectName, Integer matildaStatusId) throws EntityNotFoundException {
        ProjectProfileEntity projectProfile = projectProfileRepository.findByRepositoryNameAndProjectName(repositoryName, projectName)
                .orElseThrow(() -> new EntityNotFoundException("Project Profile for Request Id not found: " + repositoryName + "/" + projectName));
        projectProfile.setStatus(matildaStatusId);
        return projectProfileRepository.save(projectProfile).getMatildaRequestId();
    }

}
