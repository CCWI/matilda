package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_PROJECT_PROFILE")
public class ProjectProfileEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_PROJECT_PROFILE_GENERATOR")
    @SequenceGenerator(name="MATILDA_PROJECT_PROFILE_GENERATOR",sequenceName="MATILDA_PROJECT_PROFILE_SEQUENCE", allocationSize=1)
    @Column(name = "MATILDA_PROJECT_PROFILE_ID")
    private Long matildaProjectProfileId;

    @Column(name = "MATILDA_REQUEST_ID")
    private String matildaRequestId;

    @Column(name = "MATILDA_STATUS")
    private Integer status;

    @Column(name = "MATILDA_PROJECT_NAME")
    private String projectName;

    @Column(name = "MATILDA_REPOSITORY_NAME")
    private String repositoryName;

    @Column(name = "MATILDA_URI", unique = true)
    private String uri;

    @Column(name = "MATILDA_DATE_OF_CLONE")
    private String dateOfClone;

    @Column(name = "MATILDA_AMOUNT_ANALYZED_REVISIONS")
    private Integer amountAnalyzedRevisions;

    @Column(name = "MATILDA_AMOUNT_ANALYZED_DOCUMENTATIONS")
    private Integer amountAnalyzedDocumentations;

    @Column(name = "MATILDA_AMOUNT_ANALYZED_DEPENDENCIES")
    private Integer amountAnalyzedDependencies;

    @OneToMany(targetEntity = SimilarProjectEntity.class, mappedBy = "id", cascade = CascadeType.ALL)
    private List<SimilarProjectEntity> similarProjectEntityList;

    /**
     * Default-Constructor.
     */
    public ProjectProfileEntity() {
    }

    public ProjectProfileEntity(String matildaRequestId, Integer status, String projectName, String repositoryName,
                                String uri, String dateOfClone) {
        this.matildaRequestId = matildaRequestId;
        this.status = status;
        this.projectName = projectName;
        this.repositoryName = repositoryName;
        this.uri = uri;
        this.dateOfClone = dateOfClone;
    }

    public Long getMatildaProjectProfileId() {
        return matildaProjectProfileId;
    }

    public void setMatildaProjectProfileId(Long matildaProjectProfileId) {
        this.matildaProjectProfileId = matildaProjectProfileId;
    }

    public String getMatildaRequestId() {
        return matildaRequestId;
    }

    public void setMatildaRequestId(String matildaRequestId) {
        this.matildaRequestId = matildaRequestId;
    }

    public Integer getStatus() { return status; }

    public void setStatus(Integer status) { this.status = status; }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDateOfClone() {
        return dateOfClone;
    }

    public void setDateOfClone(String dateOfClone) {
        this.dateOfClone = dateOfClone;
    }

    public Integer getAmountAnalyzedRevisions() {
        return amountAnalyzedRevisions;
    }

    public void setAmountAnalyzedRevisions(Integer amountAnalyzedRevisions) {
        this.amountAnalyzedRevisions = amountAnalyzedRevisions;
    }

    public Integer getAmountAnalyzedDocumentations() {
        return amountAnalyzedDocumentations;
    }

    public void setAmountAnalyzedDocumentations(Integer amountAnalyzedDocumentations) {
        this.amountAnalyzedDocumentations = amountAnalyzedDocumentations;
    }

    public Integer getAmountAnalyzedDependencies() {
        return amountAnalyzedDependencies;
    }

    public void setAmountAnalyzedDependencies(Integer amountAnalyzedDependencies) {
        this.amountAnalyzedDependencies = amountAnalyzedDependencies;
    }

    public List<SimilarProjectEntity> getSimilarProjectEntityList() {
        return similarProjectEntityList;
    }

    public void setSimilarProjectEntityList(List<SimilarProjectEntity> similarProjectEntityList) {
        this.similarProjectEntityList = similarProjectEntityList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProjectProfileEntity that = (ProjectProfileEntity) o;
        return Objects.equals(projectName, that.projectName) && Objects.equals(repositoryName, that.repositoryName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(projectName, repositoryName);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}