package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

public class CrawledStatistic {

    @Id
    private String statisticId; //reponame+projname+source

    private Integer numberOfBranches;
    private Integer numberOfRevisions;
    private Integer averageNumberOfMvnProjectsInRevisions;
    private Integer numberOfDependeciesInFirstRevision;
    private Integer numberOfDependeciesInLastRevision;
    private Integer numberOfRelevantDependeciesInFirstRevision;
    private Integer numberOfRelevantDependeciesInLastRevision;

    public CrawledStatistic() {
    }

    public CrawledStatistic(String reponame, String projectname, String source) {
        this.statisticId = reponame + "-" + projectname + "-" + source;
    }

    public CrawledStatistic(String statisticId, Integer numberOfBranches, Integer numberOfRevisions,
                            Integer averageNumberOfMvnProjectsInRevisions, Integer numberOfDependeciesInFirstRevision,
                            Integer numberOfDependeciesInLastRevision, Integer numberOfRelevantDependeciesInFirstRevision,
                            Integer numberOfRelevantDependeciesInLastRevision) {
        this.statisticId = statisticId;
        this.numberOfBranches = numberOfBranches;
        this.numberOfRevisions = numberOfRevisions;
        this.averageNumberOfMvnProjectsInRevisions = averageNumberOfMvnProjectsInRevisions;
        this.numberOfDependeciesInFirstRevision = numberOfDependeciesInFirstRevision;
        this.numberOfDependeciesInLastRevision = numberOfDependeciesInLastRevision;
        this.numberOfRelevantDependeciesInFirstRevision = numberOfRelevantDependeciesInFirstRevision;
        this.numberOfRelevantDependeciesInLastRevision = numberOfRelevantDependeciesInLastRevision;
    }

    public String getStatisticId() {
        return statisticId;
    }

    public void setStatisticId(String statisticId) {
        this.statisticId = statisticId;
    }

    public Integer getNumberOfBranches() {
        return numberOfBranches;
    }

    public void setNumberOfBranches(Integer numberOfBranches) {
        this.numberOfBranches = numberOfBranches;
    }

    public Integer getNumberOfRevisions() {
        return numberOfRevisions;
    }

    public void setNumberOfRevisions(Integer numberOfRevisions) {
        this.numberOfRevisions = numberOfRevisions;
    }

    public Integer getAverageNumberOfMvnProjectsInRevisions() {
        return averageNumberOfMvnProjectsInRevisions;
    }

    public void setAverageNumberOfMvnProjectsInRevisions(Integer averageNumberOfMvnProjectsInRevisions) {
        this.averageNumberOfMvnProjectsInRevisions = averageNumberOfMvnProjectsInRevisions;
    }

    public Integer getNumberOfDependeciesInFirstRevision() {
        return numberOfDependeciesInFirstRevision;
    }

    public void setNumberOfDependeciesInFirstRevision(Integer numberOfDependeciesInFirstRevision) {
        this.numberOfDependeciesInFirstRevision = numberOfDependeciesInFirstRevision;
    }

    public Integer getNumberOfDependeciesInLastRevision() {
        return numberOfDependeciesInLastRevision;
    }

    public void setNumberOfDependeciesInLastRevision(Integer numberOfDependeciesInLastRevision) {
        this.numberOfDependeciesInLastRevision = numberOfDependeciesInLastRevision;
    }

    public Integer getNumberOfRelevantDependeciesInFirstRevision() {
        return numberOfRelevantDependeciesInFirstRevision;
    }

    public void setNumberOfRelevantDependeciesInFirstRevision(Integer numberOfRelevantDependeciesInFirstRevision) {
        this.numberOfRelevantDependeciesInFirstRevision = numberOfRelevantDependeciesInFirstRevision;
    }

    public Integer getNumberOfRelevantDependeciesInLastRevision() {
        return numberOfRelevantDependeciesInLastRevision;
    }

    public void setNumberOfRelevantDependeciesInLastRevision(Integer numberOfRelevantDependeciesInLastRevision) {
        this.numberOfRelevantDependeciesInLastRevision = numberOfRelevantDependeciesInLastRevision;
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
