package edu.hm.ccwi.matilda.persistence.mongo.model.recomcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class SimilarProjectCard {

    private double similarity;

    private String reponame;

    private String projektname;

    private String sourceLink;

    private String similarRevisionId;

    private LocalDateTime commitDate;

    private LocalDateTime dateOfAnalyzation;

    public SimilarProjectCard(){
    }

    public SimilarProjectCard(double similarity, String reponame, String projektname, String sourceLink,
                              String similarRevisionId, LocalDateTime commitDate, LocalDateTime dateOfAnalyzation) {
        this.similarity = similarity;
        this.reponame = reponame;
        this.projektname = projektname;
        this.sourceLink = sourceLink;
        this.similarRevisionId = similarRevisionId;
        this.commitDate = commitDate;
        this.dateOfAnalyzation = dateOfAnalyzation;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }

    public String getReponame() {
        return reponame;
    }

    public void setReponame(String reponame) {
        this.reponame = reponame;
    }

    public String getProjektname() {
        return projektname;
    }

    public void setProjektname(String projektname) {
        this.projektname = projektname;
    }

    public String getSourceLink() {
        return sourceLink;
    }

    public void setSourceLink(String sourceLink) {
        this.sourceLink = sourceLink;
    }

    public String getSimilarRevisionId() {
        return similarRevisionId;
    }

    public void setSimilarRevisionId(String similarRevisionId) {
        this.similarRevisionId = similarRevisionId;
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
