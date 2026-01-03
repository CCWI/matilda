package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_RECOMMENDATION")
public class RecommendationEntity implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_RECOMMENDATION_GENERATOR")
    @SequenceGenerator(name="MATILDA_RECOMMENDATION_GENERATOR",sequenceName="MATILDA_RECOMMENDATION_SEQUENCE", allocationSize=1)
    private Long id;

    /**
     * FK to reference ProjectProfile
     */
    @Column(name = "REQUEST_ID")
    private String requestId;

    @Column(name = "RECOMMENDATION_RANK")
    private Double recomRank;

    @ManyToOne
    @JoinColumn(name = "BASED_ON_LIBRARY_ID", referencedColumnName = "id", insertable = false, updatable = false)
    private LibraryEntity basedOnLibrary;

    @ManyToOne
    @JoinColumn(name = "RECOMMENDED_LIBRARY_ID", referencedColumnName = "id", insertable = false, updatable = false)
    private LibraryEntity recommendedLibrary;

    @Column(name = "CREATE_TIME")
    private LocalDateTime createTime;

    public RecommendationEntity() {
    }

    public RecommendationEntity(String requestId, Double recomRank, LibraryEntity basedOnLibrary,
                                LibraryEntity recommendedLibrary, LocalDateTime createTime) {
        this.requestId = requestId;
        this.recomRank = recomRank;
        this.basedOnLibrary = basedOnLibrary;
        this.recommendedLibrary = recommendedLibrary;
        this.createTime = createTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Double getRecomRank() {
        return recomRank;
    }

    public void setRecomRank(Double recomRank) {
        this.recomRank = recomRank;
    }

    public LibraryEntity getBasedOnLibrary() {
        return basedOnLibrary;
    }

    public void setBasedOnLibrary(LibraryEntity basedOnLibrary) {
        this.basedOnLibrary = basedOnLibrary;
    }

    public LibraryEntity getRecommendedLibrary() {
        return recommendedLibrary;
    }

    public void setRecommendedLibrary(LibraryEntity recommendedLibrary) {
        this.recommendedLibrary = recommendedLibrary;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RecommendationEntity that = (RecommendationEntity) o;
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