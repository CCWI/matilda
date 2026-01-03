package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.jpa.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.PersistenceException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjetProfilePersistenceServiceIT extends IntegrationTestBase {

    @Autowired
    private ProjectProfileService projectProfileService;

    @Test
    public void testSaveUpdateAndLoadProjectProfiles() throws PersistenceException, StateException {
        // Arrange
        UUID requestId1 = UUID.randomUUID();
        UUID requestId2 = UUID.randomUUID();
        String dateOfClone = LocalDateTime.now().toString();
        ProjectProfile projectProfile1 = new ProjectProfile(requestId1.toString(), MatildaStatusCode.FINISHED_ANALYZING_PROJECT,
                "testProject1", "testRepo1", "testUri1", dateOfClone);
        ProjectProfile projectProfile2 = new ProjectProfile(requestId2.toString(), MatildaStatusCode.FINISHED_ANALYZING_PROJECT,
                "testProject2", "testRepo2", "testUri2", LocalDateTime.now().toString());
        //TODO check if Dependency-Relation was ever used! Sounds like it is the new Library-Table but it does not make sense at Project Level -> rev-level!
        
        // Act
        projectProfileService.saveOrUpdateProjectProfile(projectProfile1);
        String requestIdOfSavedPP2 = projectProfileService.saveOrUpdateProjectProfile(projectProfile2);

        String updatedProjectProfile2ReqId = projectProfileService.updateProjectState(projectProfile2.getRepositoryName(), projectProfile2.getProjectName(),
                MatildaStatusCode.ERROR_GENERAL.getStatusCode());

        List<ProjectProfile> profileOfAnalyzedProjectByState142 = 
                projectProfileService.getProfileOfAnalyzedProjectByState(String.valueOf(MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode()));

        ProjectProfile savedProjectProfile2 = projectProfileService.getProfileOfAnalyzedProject(requestIdOfSavedPP2);

        // Assert
        assertThat(profileOfAnalyzedProjectByState142).hasSize(1);
        assertThat(profileOfAnalyzedProjectByState142.get(0).getDateOfClone()).isEqualTo(dateOfClone);
        assertThat(profileOfAnalyzedProjectByState142.get(0).getMatildaRequestId()).isEqualTo(requestId1.toString());
        assertThat(updatedProjectProfile2ReqId).isEqualTo(requestId2.toString());
        assertThat(savedProjectProfile2.getStatus().getStatusCode()).isEqualTo(MatildaStatusCode.ERROR_GENERAL.getStatusCode());
    }
}