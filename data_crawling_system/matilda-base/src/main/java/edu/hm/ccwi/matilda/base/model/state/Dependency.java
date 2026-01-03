package edu.hm.ccwi.matilda.base.model.state;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dependency implements Serializable {

    private String groupArtifactId;

    private String groupId;

    private String artifactId;

    private List<ProjectProfile> projectProfiles;

    public Dependency() {
        this.projectProfiles = new ArrayList<>();
    }

    public Dependency(String groupId, String artifactId) {
        this.groupArtifactId = groupId + ":" + artifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.projectProfiles = new ArrayList<>();
    }

    public Dependency(String groupArtifactId, String groupId, String artifactId) {
        this.groupArtifactId = groupArtifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public Dependency(String groupArtifactId, String groupId, String artifactId,
                      List<ProjectProfile> projectProfiles) {
        this.groupArtifactId = groupArtifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.projectProfiles = projectProfiles;
    }

    public String getGroupArtifactId() {
        return groupArtifactId;
    }

    public void setGroupArtifactId(String groupArtifactId) {
        this.groupArtifactId = groupArtifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dependency that = (Dependency) o;
        return Objects.equals(groupArtifactId, that.groupArtifactId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupArtifactId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
