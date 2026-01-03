package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Document
public class GACategoryTag {

    @Id
    private String id;
    private String group;
    private String artifact;
    private String category;
    private List<String> tags;
    private boolean crawled;

    public GACategoryTag() {
        this.tags = new ArrayList<>();
    }

    public GACategoryTag(String group, String artifact, String category, List<String> tags, boolean crawled) {
        this.id = group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.category = category;
        this.tags = tags;
        this.crawled = crawled;
    }

    public GACategoryTag(String id) {
        this.id = id;
        String[] ga = id.split(":");
        if(ga.length >= 2) {
            this.group = ga[0];
            this.artifact = ga[1];
        }
        this.category = null;
        this.tags = new ArrayList<>();
    }

    public GACategoryTag(String group, String artifact) {
        this.group = group;
        this.artifact = artifact;
        this.id =  group + ":" + artifact;
        this.category = null;
        this.tags = new ArrayList<>();
    }

    public GACategoryTag(String group, String artifact, String category) {
        this.id =  group + ":" + artifact;
        this.group = group;
        this.artifact = artifact;
        this.category = category;
        this.tags = new ArrayList<>();
    }

    public synchronized void addTag(String tag) {
        this.tags.add(tag);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GACategoryTag that = (GACategoryTag) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}