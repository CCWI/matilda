package edu.hm.ccwi.matilda.base.model.library;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Objects;

public class Library {

    private String groupArtifactId;

    private String groupId;

    private String artifactId;

    private Long category;

    private String tags;

    private List<LibraryTechnology> technologies;

    public Library() {
    }

    public Library(String groupId, String artifactId) {
        this.groupArtifactId = groupId + ":" + artifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public Library(String groupArtifactId, String groupId, String artifactId) {
        this.groupArtifactId = groupArtifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public Library(String groupArtifactId, String groupId, String artifactId, long category, String tags) {
        this.groupArtifactId = groupArtifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.category = category;
        this.tags = tags;
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

    public Long getCategory() {
        return category;
    }

    public void setCategory(Long category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Library that = (Library) o;
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
