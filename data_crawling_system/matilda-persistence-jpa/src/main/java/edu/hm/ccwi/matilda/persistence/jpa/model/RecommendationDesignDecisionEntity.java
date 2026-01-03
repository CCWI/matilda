package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_RECOMMENDATION_DESIGN_DECISION")
public class RecommendationDesignDecisionEntity implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_RECOMMENDATION_DD_GENERATOR")
    @SequenceGenerator(name="MATILDA_RECOMMENDATION_DD_GENERATOR",sequenceName="MATILDA_RECOMMENDATION_DD_SEQUENCE", allocationSize=1)
    private Long id;

    /**
     * FK to reference Recommendations
     */
    @Column(name = "RECOMMENDATION_ID")
    private Long recommendationId;

    @ManyToOne
    @JoinColumn(name = "EXTRACTED_DESIGN_DECISION_ID", referencedColumnName = "id", insertable = false, updatable = false)
    private ExtractedDesignDecisionEntity extractedDesignDecisionId;

    @Column(name = "TOWARDS_RECOMMENDATION")
    private Boolean towardsRecommendation;


    public RecommendationDesignDecisionEntity() {
    }

    public RecommendationDesignDecisionEntity(Long recommendationId, ExtractedDesignDecisionEntity extractedDesignDecisionId,
                                              Boolean towardsRecommendation){
        this.recommendationId = recommendationId;
        this.extractedDesignDecisionId = extractedDesignDecisionId;
        this.towardsRecommendation = towardsRecommendation;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRecommendationId() {
        return recommendationId;
    }

    public void setRecommendationId(Long recommendationId) {
        this.recommendationId = recommendationId;
    }

    public ExtractedDesignDecisionEntity getExtractedDesignDecisionId() {
        return extractedDesignDecisionId;
    }

    public void setExtractedDesignDecisionId(ExtractedDesignDecisionEntity extractedDesignDecisionId) {
        this.extractedDesignDecisionId = extractedDesignDecisionId;
    }

    public Boolean getTowardsRecommendation() {
        return towardsRecommendation;
    }

    public void setTowardsRecommendation(Boolean towardsRecommendation) {
        this.towardsRecommendation = towardsRecommendation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendationDesignDecisionEntity that = (RecommendationDesignDecisionEntity) o;
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