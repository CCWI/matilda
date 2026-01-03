package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.ProjectProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectProfileRepository extends JpaRepository<ProjectProfileEntity, String> {

        Optional<ProjectProfileEntity> findByMatildaRequestId(String matildaRequestId);

        Optional<ProjectProfileEntity> findByRepositoryNameAndProjectName(String repositoryName, String projectName);

        List<ProjectProfileEntity> findAllByStatus(Integer matildaStatus);
}
