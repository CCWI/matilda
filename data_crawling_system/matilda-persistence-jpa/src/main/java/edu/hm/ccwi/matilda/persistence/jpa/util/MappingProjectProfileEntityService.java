package edu.hm.ccwi.matilda.persistence.jpa.util;

import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.persistence.jpa.model.ProjectProfileEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MappingProjectProfileEntityService {

    private static final Logger LOG = LoggerFactory.getLogger(MappingProjectProfileEntityService.class);

    public ProjectProfile mapProjectProfileEntityToProjectProfile(ProjectProfileEntity ppe) {
        if(ppe == null) {
            LOG.debug("mapProjectProfileEntityToProjectProfile - personprofile was null.");
            return null;
        } else {
            return new ProjectProfile(ppe.getMatildaRequestId(), MatildaStatusCode.getMatildaStatusCode(ppe.getStatus()),
                    ppe.getProjectName(), ppe.getRepositoryName(), ppe.getUri(), ppe.getDateOfClone());
        }
    }

    public ProjectProfileEntity mapProjectProfileToProjectProfileEntity(ProjectProfile pp) throws StateException {
        if(pp == null) {
            throw new StateException("Project Profile is null and therefore cannot be mapped to entity");
        }
        return new ProjectProfileEntity(pp.getMatildaRequestId(), pp.getStatus().getStatusCode(), pp.getProjectName(),
                pp.getRepositoryName(), pp.getUri(), pp.getDateOfClone());

    }
}
