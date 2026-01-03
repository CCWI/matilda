package edu.hm.ccwi.matilda.persistence.mongo.model.recomcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.List;

public class ResultCard {

    @Id
    private String matildaId;
    private int status;
    private String repoName;
    private String projectName;
    private String link;
    private String lastCommitIncludingChanges;
    private LocalDateTime commitDate;
    private LocalDateTime dateOfAnalyzation;
    private List<ResultProjectComponentCard> resultProjectComponentCards; //TODO update ppt-slides: No recom for revisions but for sw components

    public ResultCard(){
    }

    public ResultCard(String matildaId, int status, String repoName, String projectName, String link,
                      String lastCommitIncludingChanges, LocalDateTime commitDate, LocalDateTime dateOfAnalyzation,
                      List<ResultProjectComponentCard> resultProjectComponentCards) {
        this.matildaId = matildaId;
        this.status = status;
        this.repoName = repoName;
        this.projectName = projectName;
        this.link = link;
        this.lastCommitIncludingChanges = lastCommitIncludingChanges;
        this.commitDate = commitDate;
        this.dateOfAnalyzation = dateOfAnalyzation;
        this.resultProjectComponentCards = resultProjectComponentCards;
    }

    public String getMatildaId() {
        return matildaId;
    }

    public void setMatildaId(String matildaId) {
        this.matildaId = matildaId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLastCommitIncludingChanges() {
        return lastCommitIncludingChanges;
    }

    public void setLastCommitIncludingChanges(String lastCommitIncludingChanges) {
        this.lastCommitIncludingChanges = lastCommitIncludingChanges;
    }

    public LocalDateTime getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(LocalDateTime commitDate) {
        this.commitDate = commitDate;
    }

    public LocalDateTime getDateOfAnalyzation() {
        return dateOfAnalyzation;
    }

    public void setDateOfAnalyzation(LocalDateTime dateOfAnalyzation) {
        this.dateOfAnalyzation = dateOfAnalyzation;
    }

    public List<ResultProjectComponentCard> getResultProjectComponentCards() {
        return resultProjectComponentCards;
    }

    public void setResultProjectComponentCards(List<ResultProjectComponentCard> resultProjectComponentCards) {
        this.resultProjectComponentCards = resultProjectComponentCards;
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
