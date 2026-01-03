package edu.hm.ccwi.matilda.korpus.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class CrawledDependency {

    private String group;
    private String artifact;
    private String version;
    private boolean usedInCode;
    private boolean isRelevant;
    private boolean newlyAdded;
    private boolean isRemoved;
    private LibCategory categorylabel;
    private String tagList;
    CrawledDependency replacedBy;

    public CrawledDependency() {
    }

    public CrawledDependency(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.isRelevant = true; // Default: assume relevant until auto-validation implemented
    }

    public CrawledDependency(String group, String artifact, String version, boolean isRelevant,
                             LibCategory categorylabel, String tagList) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.isRelevant = isRelevant;
        this.categorylabel = categorylabel;
        this.tagList = tagList;
    }

    public CrawledDependency(String group, String artifact, String version, boolean usedInCode,
                             boolean isRelevant, boolean newlyAdded, boolean isRemoved, LibCategory categorylabel,
                             String tagList, CrawledDependency replacedBy) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.usedInCode = usedInCode;
        this.isRelevant = isRelevant;
        this.newlyAdded = newlyAdded;
        this.isRemoved = isRemoved;
        this.categorylabel = categorylabel;
        this.tagList = tagList;
        this.replacedBy = replacedBy;
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

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isUsedInCode() {
        return usedInCode;
    }

    public void setUsedInCode(boolean usedInCode) {
        this.usedInCode = usedInCode;
    }

    public boolean isNewlyAdded() {
        return newlyAdded;
    }

    public void setNewlyAdded(boolean newlyAdded) {
        this.newlyAdded = newlyAdded;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public void setRemoved(boolean removed) {
        isRemoved = removed;
    }

    public LibCategory getCategorylabel() {
        return categorylabel;
    }

    public void setCategorylabel(LibCategory categorylabel) {
        this.categorylabel = categorylabel;
    }

    public CrawledDependency getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(CrawledDependency replacedBy) {
        this.replacedBy = replacedBy;
    }

    public void setRelevant(boolean relevant) { isRelevant = relevant; }

    public boolean isRelevant() { return isRelevant; }

    public String getTagList() { return tagList; }

    public void setTagList(String tagList) { this.tagList = tagList; }

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