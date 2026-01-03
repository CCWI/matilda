package edu.hm.ccwi.matilda.persistence.mongo.model.recomcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

import java.util.List;

public class ResultProjectComponentCard {

    private String group;
    private String artifact;
    private String version;
    private String description;

    private List<SimilarProjectCard> similarProjects;
    private List<DependencyCard> techStack;
    private List<RecommendationCard> recommendations;

    public ResultProjectComponentCard() {
    }

    public ResultProjectComponentCard(String group, String artifact, String version, String description) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.description = description;
    }

    public ResultProjectComponentCard(String group, String artifact, String version, String description,
                                      List<DependencyCard> techStack, List<RecommendationCard> recommendations,
                                      List<SimilarProjectCard> similarProjects) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
        this.description = description;
        this.techStack = techStack;
        this.recommendations = recommendations;
        this.similarProjects = similarProjects;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<DependencyCard> getTechStack() {
        return techStack;
    }

    public void setTechStack(List<DependencyCard> techStack) {
        this.techStack = techStack;
    }

    public List<RecommendationCard> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationCard> recommendations) {
        this.recommendations = recommendations;
    }

    public List<SimilarProjectCard> getSimilarProjects() {
        return similarProjects;
    }

    public void setSimilarProjects(List<SimilarProjectCard> similarProjects) {
        this.similarProjects = similarProjects;
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
