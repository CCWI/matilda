package edu.hm.ccwi.matilda.libmngm.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_LIBRARY")
public class LibraryEntity {

    @Id
    @Column(name = "GA_ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_LIBRARY_GENERATOR")
    @SequenceGenerator(name="MATILDA_LIBRARY_GENERATOR",sequenceName="MATILDA_LIBRARY_SEQUENCE", allocationSize=1)
    private String groupArtifactId;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "ARTIFACT_ID")
    private String artifactId;

    @Column(name = "CATEGORY_ID")
    private Long category;

    @Column(name = "TAGS")
    private String tags;

    @ManyToMany(targetEntity = LibraryTechnologyEntity.class, mappedBy = "libraries", cascade = CascadeType.ALL)
    @Column(name = "TECHNOLOGIES")
    private List<LibraryTechnologyEntity> technologies;

    public LibraryEntity() {
    }

    public LibraryEntity(String groupId, String artifactId) {
        this.groupArtifactId = groupId + ":" + artifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public LibraryEntity(String groupArtifactId, String groupId, String artifactId) {
        this.groupArtifactId = groupArtifactId;
        this.groupId = groupId;
        this.artifactId = artifactId;
    }

    public LibraryEntity(String groupArtifactId, String groupId, String artifactId, long category, String tags) {
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
        LibraryEntity that = (LibraryEntity) o;
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
