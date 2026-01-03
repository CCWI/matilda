package edu.hm.ccwi.matilda.korpus.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document
public class CrawledRepository {

    @Id
    private String id;
    private String projectName;
    private String repositoryName;
    private RepoSource source;
    private String directoryPath;
    private List<String> revisionCommitIdList;

    public CrawledRepository() {
    }

    public CrawledRepository(String projectName, String repositoryName, RepoSource source, String directoryPath) {
        this.id = createId(repositoryName, projectName);
        this.projectName = projectName;
        this.repositoryName = repositoryName;
        this.source = source;
        this.directoryPath = directoryPath;
        this.revisionCommitIdList = new ArrayList<>();
    }

    public CrawledRepository(String projectName, String repositoryName, RepoSource source, String directoryPath,
                             List<String> revisionCommitIdList) {
        this.id = createId(repositoryName, projectName);
        this.projectName = projectName;
        this.repositoryName = repositoryName;
        this.source = source;
        this.directoryPath = directoryPath;
        this.revisionCommitIdList = revisionCommitIdList;
    }

    /**
     * Check if bean contains all values.
     * @return
     */
    public boolean isValid() {
        if(id != null && !(id.isEmpty()) &&
           projectName != null && !(projectName.isEmpty()) &&
           repositoryName != null && !(repositoryName.isEmpty()) &&
           source != null &&
           directoryPath != null && !(directoryPath.isEmpty()) &&
           revisionCommitIdList != null && (!revisionCommitIdList.isEmpty())) {
            return true;
        }
        return false;
    }

    private String createId(String repositoryName, String projectName) {
        return repositoryName + ":" + projectName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public List<String> getRevisionCommitIdList() { return revisionCommitIdList; }

    public void setRevisionCommitIdList(List<String> revisionCommitIdList) { this.revisionCommitIdList = revisionCommitIdList; }

    public RepoSource getSource() { return source; }

    public void setSource(RepoSource source) { this.source = source; }

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
