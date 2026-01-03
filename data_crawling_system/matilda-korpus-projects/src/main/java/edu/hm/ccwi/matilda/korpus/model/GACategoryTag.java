package edu.hm.ccwi.matilda.korpus.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public class GACategoryTag {

    @Id
    private String groupArtifact;
    private String group;
    private String artifact;
    private LibCategory categorylabel;
    private String tagList;

    public GACategoryTag() {
    }

    public GACategoryTag(String group, String artifact) {
        this.group = group;
        this.artifact = artifact;
        this.groupArtifact =  group + ":" + artifact;
        this.categorylabel = null;
        this.tagList = null;
    }

    public GACategoryTag(String group, String artifact, LibCategory categorylabel, String tagList) {
        this.groupArtifact =  group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.categorylabel = categorylabel;
        this.tagList = tagList;
    }

    public String getGroupArtifact() {
        return groupArtifact;
    }

    public void setGroupArtifact(String groupArtifact) {
        this.groupArtifact = groupArtifact;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    public LibCategory getCategorylabel() {
        return categorylabel;
    }

    public void setCategorylabel(LibCategory categorylabel) {
        this.categorylabel = categorylabel;
    }

    public String getTagList() {
        return tagList;
    }

    public void setTagList(String tagList) {
        this.tagList = tagList;
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