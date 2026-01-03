package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

import java.util.ArrayList;
import java.util.List;

public class CrawledDocumentation {

    @Id
    private String commitId;
    private String language;
    private List<String> documentationFileList;
    private List<String> entitySet;
    private List<String> keywordSet;

    public CrawledDocumentation(){
        documentationFileList = new ArrayList<>();
        entitySet = new ArrayList<>();
        keywordSet = new ArrayList<>();
    }

    public CrawledDocumentation(String commitId, List<String> documentationFileList) {
        this.commitId = commitId;
        this.documentationFileList = documentationFileList;
    }

    public CrawledDocumentation(String commitId, String language, List<String> documentationFileList, List<String> entitySet,
                                List<String> keywordSet) {
        this.commitId = commitId;
        this.language = language;
        this.documentationFileList = documentationFileList;
        this.entitySet = entitySet;
        this.keywordSet = keywordSet;
    }

    public List<String> getDocumentationFileList() {
        return documentationFileList;
    }

    public void setDocumentationFileList(List<String> documentationFileList) {
        this.documentationFileList = documentationFileList;
    }

    public String getCommitId() { return commitId; }

    public List<String> getEntitySet() {
        return entitySet;
    }

    public void setEntitySet(List<String> entitySet) {
        this.entitySet = entitySet;
    }

    public List<String> getKeywordSet() {
        return keywordSet;
    }

    public void setKeywordSet(List<String> keywordSet) {
        this.keywordSet = keywordSet;
    }

    public String getLanguage() { return language; }

    public void setLanguage(String language) { this.language = language; }

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
