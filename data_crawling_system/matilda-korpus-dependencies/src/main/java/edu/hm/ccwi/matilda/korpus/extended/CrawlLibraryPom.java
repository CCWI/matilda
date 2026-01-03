package edu.hm.ccwi.matilda.korpus.extended;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document
public class CrawlLibraryPom {

    @Id
    private String id;

    private String mvnRepoCategory;

    private String matildaCategory;

    private List<String> tagList;

    private String groupId;

    private String artifactId;

    private String name;

    private String description;

    private String url;

    private List<CrawlLibraryPomDependency> dependencies;

    private List<CrawlLibraryPomDeveloper> developers;

    private String strippedPom;

    public CrawlLibraryPom() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMvnRepoCategory() {
        return mvnRepoCategory;
    }

    public void setMvnRepoCategory(String mvnRepoCategory) {
        this.mvnRepoCategory = mvnRepoCategory;
    }

    public String getMatildaCategory() {
        return matildaCategory;
    }

    public void setMatildaCategory(String matildaCategory) {
        this.matildaCategory = matildaCategory;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<CrawlLibraryPomDependency> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<CrawlLibraryPomDependency> dependencies) {
        this.dependencies = dependencies;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<CrawlLibraryPomDeveloper> getDevelopers() {
        return developers;
    }

    public void setDevelopers(List<CrawlLibraryPomDeveloper> developers) {
        this.developers = developers;
    }

    public List<String> getTagList() {
        return tagList;
    }

    public void setTagList(List<String> tagList) {
        this.tagList = tagList;
    }

    public String getStrippedPom() {
        return strippedPom;
    }

    public void setStrippedPom(String strippedPom) {
        this.strippedPom = strippedPom;
    }

    @Override
    public String toString() {
        return "CrawlLibraryPom{" +
                "id='" + id + '\'' +
                ", mvnRepoCategory='" + mvnRepoCategory + '\'' +
                ", matildaCategory='" + matildaCategory + '\'' +
                ", tagList=" + tagList +
                ", groupId='" + groupId + '\'' +
                ", artifactId='" + artifactId + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", dependencies=" + dependencies +
                ", developers=" + developers +
                '}';
    }
}
