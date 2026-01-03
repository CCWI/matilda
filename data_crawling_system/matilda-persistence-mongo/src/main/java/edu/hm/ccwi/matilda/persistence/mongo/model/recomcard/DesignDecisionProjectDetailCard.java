package edu.hm.ccwi.matilda.persistence.mongo.model.recomcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;

public class DesignDecisionProjectDetailCard {

    private String repositoryName;
    private String projectName;
    private String decisionSubject;
    private String initial;
    private String target;
    private LocalDateTime decisionDate;
    private String url;

    public DesignDecisionProjectDetailCard(){
    }

    public DesignDecisionProjectDetailCard(String repositoryName, String projectName, String decisionSubject, String initial,
                                           String target, LocalDateTime decisionDate, String url) {
        this.repositoryName = repositoryName;
        this.projectName = projectName;
        this.decisionSubject = decisionSubject;
        this.initial = initial;
        this.target = target;
        this.decisionDate = decisionDate;
        this.url = url;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDecisionSubject() {
        return decisionSubject;
    }

    public void setDecisionSubject(String decisionSubject) {
        this.decisionSubject = decisionSubject;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public LocalDateTime getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDateTime decisionDate) {
        this.decisionDate = decisionDate;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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
