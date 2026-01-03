package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;
import java.util.Objects;

public class CrawledDependency {

    private String group;
    private String artifact;
    private String version;
    private boolean usedInCode;
    private boolean isRelevant;
    private boolean newlyAdded;
    private boolean isRemoved;
    private String category;
    private List<String> tagList;
    CrawledDependency replacedBy;

    public CrawledDependency() {
    }

    public CrawledDependency(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.isRelevant = true;
    }

    public CrawledDependency(String group, String artifact, String version, boolean usedInCode, boolean isRelevant,
                             boolean newlyAdded, boolean isRemoved, List<String> tagList) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.usedInCode = usedInCode;
        this.isRelevant = isRelevant;
        this.newlyAdded = newlyAdded;
        this.isRemoved = isRemoved;
        this.tagList = tagList;
    }

    public CrawledDependency(String group, String artifact, String version, boolean isRelevant,
                             String category, List<String> tagList) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.isRelevant = isRelevant;
        this.category = category;
        this.tagList = tagList;
    }

    public CrawledDependency(String group, String artifact, String version, boolean usedInCode,
                             boolean isRelevant, boolean newlyAdded, boolean isRemoved, String category,
                             List<String> tagList, CrawledDependency replacedBy) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.usedInCode = usedInCode;
        this.isRelevant = isRelevant;
        this.newlyAdded = newlyAdded;
        this.isRemoved = isRemoved;
        this.category = category;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public CrawledDependency getReplacedBy() {
        return replacedBy;
    }

    public void setReplacedBy(CrawledDependency replacedBy) {
        this.replacedBy = replacedBy;
    }

    public void setRelevant(boolean relevant) { isRelevant = relevant; }

    public boolean isRelevant() { return isRelevant; }

    public List<String> getTagList() { return tagList; }

    public void setTagList(List<String> tagList) { this.tagList = tagList; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawledDependency that = (CrawledDependency) o;
        return Objects.equals(group, that.group) &&
                Objects.equals(artifact, that.artifact) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, artifact, version);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /**
     * Create GAV-Identifier - separated
     * @return
     */
    public String toGAVString() {
        return this.group + ":" + this.artifact + ":" + this.version;
    }

    /**
     * Create GA-Identifier - separated by :
     * Introduced, since decisions should not consider versions
     * @return
     */
    public String toGAString() {
        return this.group + ":" + this.artifact;
    }
}