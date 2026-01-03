package edu.hm.ccwi.matilda.base.model.state;

import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ProjectProfile implements Serializable {

    private String matildaRequestId;

    private MatildaStatusCode status;

    private String projectName;

    private String repositoryName;

    private String uri;

    private String dateOfClone;

    /**
     * Default-Constructor.
     */
    public ProjectProfile() {
    }

    public ProjectProfile(String matildaRequestId) {
        this.matildaRequestId = matildaRequestId;
    }

    public ProjectProfile(String matildaRequestId, MatildaStatusCode status, String projectName, String repositoryName, String uri) {
        this.matildaRequestId = matildaRequestId;
        this.status = status;
        this.projectName = projectName;
        this.repositoryName = repositoryName;
        this.uri = uri;
    }

    public ProjectProfile(String matildaRequestId, MatildaStatusCode status, String projectName, String repositoryName,
                          String uri, String dateOfClone) {
        this.matildaRequestId = matildaRequestId;
        this.status = status;
        this.projectName = projectName;
        this.repositoryName = repositoryName;
        this.uri = uri;
        this.dateOfClone = dateOfClone;
    }

    public String getMatildaRequestId() {
        return matildaRequestId;
    }

    public void setMatildaRequestId(String matildaRequestId) {
        this.matildaRequestId = matildaRequestId;
    }

    public MatildaStatusCode getStatus() { return status; }

    public void setStatus(MatildaStatusCode status) { this.status = status; }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDateOfClone() {
        return dateOfClone;
    }

    public void setDateOfClone(String dateOfClone) {
        this.dateOfClone = dateOfClone;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectProfile that = (ProjectProfile) o;
        return Objects.equals(matildaRequestId, that.matildaRequestId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(matildaRequestId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}