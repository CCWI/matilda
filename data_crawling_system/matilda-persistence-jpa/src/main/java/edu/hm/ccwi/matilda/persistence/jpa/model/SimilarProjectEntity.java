package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_SIMILAR_PROJECT")
public class SimilarProjectEntity implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_SIMILAR_PROJECT_GENERATOR")
    @SequenceGenerator(name="MATILDA_SIMILAR_PROJECT_GENERATOR",sequenceName="MATILDA_SIMILAR_PROJECT_SEQUENCE", allocationSize=1)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "source_project_id", referencedColumnName = "MATILDA_PROJECT_PROFILE_ID", insertable = false, updatable = false)
    private ProjectProfileEntity sourceProject;

    @ManyToOne
    @JoinColumn(name = "similar_project_id", referencedColumnName = "MATILDA_PROJECT_PROFILE_ID", insertable = false, updatable = false)
    private ProjectProfileEntity similarProject;

    @Column(name = "MOST_SIMILAR_REVISION")
    private String mostSimilarRevision;

    @Column(name = "COMMIT_DATE_OF_REVISION")
    private LocalDateTime commitDateOfRevision;

    @Column(name = "SIMILARITY")
    private Double similarity;

    @Column(name = "CALCULATION_TIME")
    private LocalDateTime calculationTime;

    public SimilarProjectEntity() {
    }

    public SimilarProjectEntity(ProjectProfileEntity sourceProject, ProjectProfileEntity similarProject, String mostSimilarRevision,
                                LocalDateTime commitDateOfRevision, Double similarity, LocalDateTime calculationTime) {
        this.sourceProject = sourceProject;
        this.similarProject = similarProject;
        this.mostSimilarRevision = mostSimilarRevision;
        this.commitDateOfRevision = commitDateOfRevision;
        this.similarity = similarity;
        this.calculationTime = calculationTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectProfileEntity getSourceProject() {
        return sourceProject;
    }

    public void setSourceProject(ProjectProfileEntity sourceProject) {
        this.sourceProject = sourceProject;
    }

    public ProjectProfileEntity getSimilarProject() {
        return similarProject;
    }

    public void setSimilarProject(ProjectProfileEntity similarProject) {
        this.similarProject = similarProject;
    }

    public String getMostSimilarRevision() {
        return mostSimilarRevision;
    }

    public void setMostSimilarRevision(String mostSimilarRevision) {
        this.mostSimilarRevision = mostSimilarRevision;
    }

    public LocalDateTime getCommitDateOfRevision() {
        return commitDateOfRevision;
    }

    public void setCommitDateOfRevision(LocalDateTime commitDateOfRevision) {
        this.commitDateOfRevision = commitDateOfRevision;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public LocalDateTime getCalculationTime() {
        return calculationTime;
    }

    public void setCalculationTime(LocalDateTime calculationTime) {
        this.calculationTime = calculationTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimilarProjectEntity that = (SimilarProjectEntity) o;
        return Objects.equals(id, that.id);
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