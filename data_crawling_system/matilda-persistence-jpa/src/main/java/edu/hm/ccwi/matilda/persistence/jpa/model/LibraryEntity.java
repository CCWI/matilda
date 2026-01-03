package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "MATILDA_LIBRARY")
public class LibraryEntity {

    @Id
    @Column(name = "ID")
    private String groupArtifactId;

    @Column(name = "GROUP_ID")
    private String groupId;

    @Column(name = "ARTIFACT_ID")
    private String artifactId;

    @Column(name = "CATEGORY_ID")
    private Long category;

    @Column(name = "TAGS")
    private String tags;

    @ManyToMany(targetEntity = CharacteristicEntity.class, mappedBy = "libraries", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Column(name = "CHARACTERISTICS")
    private Set<CharacteristicEntity> characteristics;

    public LibraryEntity() {
    }

    public LibraryEntity(String gaId) {
        this.groupArtifactId = gaId;
        String[] ga = gaId.split(":");
        if(ga.length >= 2) {
            this.groupId = ga[0];
            this.artifactId = ga[1];
        }
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

    public LibraryEntity(String groupArtifactId, String groupId, String artifactId, Long category, String tags) {
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

    public Set<CharacteristicEntity> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(Set<CharacteristicEntity> characteristics) {
        this.characteristics = characteristics;
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
