package edu.hm.ccwi.matilda.persistence.jpa.model;

import javax.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_DESIGN_DECISION")
public class ExtractedDesignDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Instant timestamp;

    @Column
    private String repository;

    @Column
    private String project;

    @Column
    private String initialCommitId;

    @Column
    private String initial;

    @Column
    private String decisionCommitId;

    @Column
    private String target;

    @Column
    private String decisionSubject;

    @Column
    private LocalDateTime decisionCommitTime;

    public ExtractedDesignDecisionEntity() {
        this.timestamp = Instant.now();
    }

    public ExtractedDesignDecisionEntity(String repositoryName, String projectName) {
        this.timestamp = Instant.now();
        this.repository = repositoryName;
        this.project = projectName;
    }

    public ExtractedDesignDecisionEntity(String repository, String project, String initial, String target) {
        this.timestamp = Instant.now();
        this.repository = repository;
        this.project = project;
        this.initial = initial;
        this.target = target;
    }

    public ExtractedDesignDecisionEntity(String repository, String project, String initialCommitId, String initial,
                                         String decisionCommitId, String target, String decisionSubject, LocalDateTime decisionCommitTime) {
        this.timestamp = Instant.now();
        this.repository = repository;
        this.project = project;
        this.initialCommitId = initialCommitId;
        this.initial = initial;
        this.decisionCommitId = decisionCommitId;
        this.target = target;
        this.decisionSubject = decisionSubject;
        this.decisionCommitTime = decisionCommitTime;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
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

    public String getDecisionSubject() {
        return decisionSubject;
    }

    public void setDecisionSubject(String decisionSubject) {
        this.decisionSubject = decisionSubject;
    }

    public LocalDateTime getDecisionCommitTime() {
        return decisionCommitTime;
    }

    public void setDecisionCommitTime(LocalDateTime committime) {
        this.decisionCommitTime = committime;
    }

    public String getInitialCommitId() {
        return initialCommitId;
    }

    public void setInitialCommitId(String initialCommitId) {
        this.initialCommitId = initialCommitId;
    }

    public String getDecisionCommitId() {
        return decisionCommitId;
    }

    public void setDecisionCommitId(String subsequentCommitId) {
        this.decisionCommitId = subsequentCommitId;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtractedDesignDecisionEntity that = (ExtractedDesignDecisionEntity) o;
        return Objects.equals(decisionCommitId, that.decisionCommitId) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(project, that.project) &&
                Objects.equals(initial, that.initial) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(decisionCommitId, repository, project, initial, target);
    }

    @Override
    public String toString() {
        return "ExtractedDesignDecision{" +
                "id=" + id +
                ", timestamp=" + timestamp +
                ", repository='" + repository + '\'' +
                ", project='" + project + '\'' +
                ", initialCommitId='" + initialCommitId + '\'' +
                ", initial='" + initial + '\'' +
                ", decisionCommitId='" + decisionCommitId + '\'' +
                ", target='" + target + '\'' +
                ", decisionSubject='" + decisionSubject + '\'' +
                ", decisionCommitTime=" + decisionCommitTime +
                '}';
    }
}
