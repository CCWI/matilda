package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class CrawledProject {

    private String projectName;
    private String projectDescription;
    private String projectGroup;
    private String projectArtifact;
    private String projectVersion;
    private String projectPath;
    private boolean usingReleaseTags;
    private List<CrawledDependency> dependencyList;

    public CrawledProject() {
    }

    public CrawledProject(String projectName, String projectDescription, String projectGroup,
                          String projectArtifact, String projectVersion, String projectPath,
                          List<CrawledDependency> dependencyList,
                          boolean usingReleaseTags) {
        this.projectName = projectName;
        this.projectDescription = projectDescription;
        this.projectGroup = projectGroup;
        this.projectArtifact = projectArtifact;
        this.projectVersion = projectVersion;
        this.projectPath = projectPath;
        this.usingReleaseTags = usingReleaseTags;
        this.dependencyList = dependencyList;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public String getProjectGroup() {
        return projectGroup;
    }

    public void setProjectGroup(String projectGroup) {
        this.projectGroup = projectGroup;
    }

    public String getProjectArtifact() {
        return projectArtifact;
    }

    public void setProjectArtifact(String projectArtifact) {
        this.projectArtifact = projectArtifact;
    }

    public String getProjectVersion() {
        return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public void setProjectPath(String projectPath) {
        this.projectPath = projectPath;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public List<CrawledDependency> getDependencyList() {
        return dependencyList;
    }

    public void setDependencyList(List<CrawledDependency> dependencyList) {
        this.dependencyList = dependencyList;
    }

    public boolean isUsingReleaseTags() {
        return usingReleaseTags;
    }

    public void setUsingReleaseTags(boolean usingReleaseTags) {
        this.usingReleaseTags = usingReleaseTags;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
