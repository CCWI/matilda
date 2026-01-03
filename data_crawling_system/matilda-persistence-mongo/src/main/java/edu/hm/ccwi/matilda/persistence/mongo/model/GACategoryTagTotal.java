package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class GACategoryTagTotal {

    @Id
    private String groupArtifact;
    private String group;
    private String artifact;
    private String category;
    private List<String> tags;
    private boolean crawled;

    public GACategoryTagTotal() {
        this.tags = new ArrayList<>();
    }

    public GACategoryTagTotal(String group, String artifact, String category, List<String> tags, boolean crawled) {
        this.groupArtifact = group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.category = category;
        this.tags = tags;
        this.crawled = crawled;
    }

    public GACategoryTagTotal(String groupArtifact) {
        this.groupArtifact =  groupArtifact;
        String[] ga = groupArtifact.split(":");
        if(ga.length >= 2) {
            this.group = ga[0];
            this.artifact = ga[1];
        }
        this.category = null;
        this.tags = new ArrayList<>();
    }

    public GACategoryTagTotal(String group, String artifact) {
        this.group = group;
        this.artifact = artifact;
        this.groupArtifact =  group + ":" + artifact;
        this.category = null;
        this.tags = new ArrayList<>();
    }

    public GACategoryTagTotal(String group, String artifact, String category) {
        this.groupArtifact =  group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.category = category;
        this.tags = new ArrayList<>();
    }

    public GACategoryTagTotal(String group, String artifact, boolean crawled) {
        this.groupArtifact =  group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.crawled = crawled;
        this.tags = new ArrayList<>();
    }

    public synchronized void addTag(String tag) {
        this.tags.add(tag);
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public boolean isCrawled() { return crawled; }

    public void setCrawled(boolean crawled) { this.crawled = crawled; }

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