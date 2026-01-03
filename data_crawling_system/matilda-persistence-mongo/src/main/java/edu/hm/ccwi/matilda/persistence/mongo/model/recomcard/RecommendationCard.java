package edu.hm.ccwi.matilda.persistence.mongo.model.recomcard;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

public class RecommendationCard {

    private double ranking;

    private String recommendedLibrary;

    private String basedOnUsedLib;

    private int amountMigratedProjectsTo;

    private int amountMigratedProjectsFrom;

    private List<DesignDecisionProjectDetailCard> detailsProjectsMigratedTowardsRecom;

    private List<DesignDecisionProjectDetailCard> detailsProjectsMigratedAwayFromRecom;

    public RecommendationCard(){
    }

    public RecommendationCard(double ranking, String recommendedLibrary, String basedOnUsedLib,
                              int amountMigratedProjectsTo, int amountMigratedProjectsFrom,
                              List<DesignDecisionProjectDetailCard> detailsProjectsMigratedTowardsRecom,
                              List<DesignDecisionProjectDetailCard> detailsProjectsMigratedAwayFromRecom) {
        this.ranking = ranking;
        this.recommendedLibrary = recommendedLibrary;
        this.basedOnUsedLib = basedOnUsedLib;
        this.amountMigratedProjectsTo = amountMigratedProjectsTo;
        this.amountMigratedProjectsFrom = amountMigratedProjectsFrom;
        this.detailsProjectsMigratedTowardsRecom = detailsProjectsMigratedTowardsRecom;
        this.detailsProjectsMigratedAwayFromRecom = detailsProjectsMigratedAwayFromRecom;
    }

    public double getRanking() {
        return ranking;
    }

    public void setRanking(double ranking) {
        this.ranking = ranking;
    }

    public String getRecommendedLibrary() {
        return recommendedLibrary;
    }

    public void setRecommendedLibrary(String recommendedLibrary) {
        this.recommendedLibrary = recommendedLibrary;
    }

    public String getBasedOnUsedLib() {
        return basedOnUsedLib;
    }

    public void setBasedOnUsedLib(String basedOnUsedLib) {
        this.basedOnUsedLib = basedOnUsedLib;
    }

    public int getAmountMigratedProjectsTo() {
        return amountMigratedProjectsTo;
    }

    public void setAmountMigratedProjectsTo(int amountMigratedProjectsTo) {
        this.amountMigratedProjectsTo = amountMigratedProjectsTo;
    }

    public int getAmountMigratedProjectsFrom() {
        return amountMigratedProjectsFrom;
    }

    public void setAmountMigratedProjectsFrom(int amountMigratedProjectsFrom) {
        this.amountMigratedProjectsFrom = amountMigratedProjectsFrom;
    }

    public List<DesignDecisionProjectDetailCard> getDetailsProjectsMigratedTowardsRecom() {
        return detailsProjectsMigratedTowardsRecom;
    }

    public void setDetailsProjectsMigratedTowardsRecom(List<DesignDecisionProjectDetailCard> detailsProjectsMigratedTowardsRecom) {
        this.detailsProjectsMigratedTowardsRecom = detailsProjectsMigratedTowardsRecom;
    }

    public List<DesignDecisionProjectDetailCard> getDetailsProjectsMigratedAwayFromRecom() {
        return detailsProjectsMigratedAwayFromRecom;
    }

    public void setDetailsProjectsMigratedAwayFromRecom(List<DesignDecisionProjectDetailCard> detailsProjectsMigratedAwayFromRecom) {
        this.detailsProjectsMigratedAwayFromRecom = detailsProjectsMigratedAwayFromRecom;
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
