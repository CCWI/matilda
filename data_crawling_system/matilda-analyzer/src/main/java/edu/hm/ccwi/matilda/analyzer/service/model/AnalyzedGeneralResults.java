package edu.hm.ccwi.matilda.analyzer.service.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Map;

public class AnalyzedGeneralResults {

    /**
     * repository/project-related
     */
    private int numberAnalyzedRepositories;
    private int numberAnalyzedProjects;

    // AnalyzedRevisionsPerRepository
    private int totalNumberOfCommits;
    private Map<String, Double> additionsToTotalNumberOfCommits;

    private int totalNumberOfRelevantCommits;
    private Map<String, Double> additionsToTotalNumberOfRelevantCommits;

    private int numberOfAnalyzedVersionsPerRepository; //tags+heads
    private int averageAmountOfProjectsPerCrawledRepository;
    private int averageAmountOfTagsPerRepository;
    private double portionProjectsUsingReleaseTags;

    /**
     * dependency-related.
     */
    private double meanAmountOfDependenciesPerProjectInRepoProject;
    private Map<String, Double> additionsToAmountOfDependenciesPerProjectInRepoProject;

    private int proportionCommonUtilLibrariesToDependencies;
    private int proportionNonCommonUtilLibrariesToDependencies;

    public double getMeanAmountOfDependenciesPerProjectInRepoProject() {
        return meanAmountOfDependenciesPerProjectInRepoProject;
    }

    public Map<String, Double> getAdditionsToAmountOfDependenciesPerProjectInRepoProject() {
        return additionsToAmountOfDependenciesPerProjectInRepoProject;
    }

    public void setAdditionsToAmountOfDependenciesPerProjectInRepoProject(Map<String, Double> additionsToAmountOfDependenciesPerProjectInRepoProject) {
        this.additionsToAmountOfDependenciesPerProjectInRepoProject = additionsToAmountOfDependenciesPerProjectInRepoProject;
    }
    // private int numberIvyProjects;

    /**
     * documentation-related.
     */
    private int numberOfReadmesIncluded;
    private int numberOfEnglishDocumentations;

    // duration of projects
    private double meanDaysOfProjectDurationByCommit;
    private double stdDaysOfProjectDurationByCommit;
    private double medianDaysOfProjectDurationByCommit;
    private double percentile60DaysOfProjectDurationByCommit;
    private double percentile90DaysOfProjectDurationByCommit;
    private double percentile95DaysOfProjectDurationByCommit;
    private double percentile99DaysOfProjectDurationByCommit;
    private double maxDaysOfProjectDurationByCommit;


    public int getTotalNumberOfCommits() {
        return totalNumberOfCommits;
    }

    public void setTotalNumberOfCommits(int totalNumberOfCommits) {
        this.totalNumberOfCommits = totalNumberOfCommits;
    }

    public Map<String, Double> getAdditionsToTotalNumberOfCommits() {
        return additionsToTotalNumberOfCommits;
    }

    public void setAdditionsToTotalNumberOfCommits(Map<String, Double> additionsToTotalNumberOfCommits) {
        this.additionsToTotalNumberOfCommits = additionsToTotalNumberOfCommits;
    }

    public int getTotalNumberOfRelevantCommits() {
        return totalNumberOfRelevantCommits;
    }

    public void setTotalNumberOfRelevantCommits(int totalNumberOfRelevantCommits) {
        this.totalNumberOfRelevantCommits = totalNumberOfRelevantCommits;
    }

    public int getNumberAnalyzedRepositories() {
        return numberAnalyzedRepositories;
    }

    public void setNumberAnalyzedRepositories(int numberAnalyzedRepositories) {
        this.numberAnalyzedRepositories = numberAnalyzedRepositories;
    }

    public int getNumberAnalyzedProjects() {
        return numberAnalyzedProjects;
    }

    public void setNumberAnalyzedProjects(int numberAnalyzedProjects) {
        this.numberAnalyzedProjects = numberAnalyzedProjects;
    }

    public Map<String, Double> getAdditionsToTotalNumberOfRelevantCommits() {
        return additionsToTotalNumberOfRelevantCommits;
    }

    public void setAdditionsToTotalNumberOfRelevantCommits(Map<String, Double> additionsToTotalNumberOfRelevantCommits) {
        this.additionsToTotalNumberOfRelevantCommits = additionsToTotalNumberOfRelevantCommits;
    }

    public int getNumberOfAnalyzedVersionsPerRepository() {
        return numberOfAnalyzedVersionsPerRepository;
    }

    public void setNumberOfAnalyzedVersionsPerRepository(int numberOfAnalyzedVersionsPerRepository) {
        this.numberOfAnalyzedVersionsPerRepository = numberOfAnalyzedVersionsPerRepository;
    }

    public int getAverageAmountOfProjectsPerCrawledRepository() {
        return averageAmountOfProjectsPerCrawledRepository;
    }

    public void setAverageAmountOfProjectsPerCrawledRepository(int averageAmountOfProjectsPerCrawledRepository) {
        this.averageAmountOfProjectsPerCrawledRepository = averageAmountOfProjectsPerCrawledRepository;
    }

    public int getAverageAmountOfTagsPerRepository() {
        return averageAmountOfTagsPerRepository;
    }

    public void setAverageAmountOfTagsPerRepository(int averageAmountOfTagsPerRepository) {
        this.averageAmountOfTagsPerRepository = averageAmountOfTagsPerRepository;
    }

    public int getProportionCommonUtilLibrariesToDependencies() {
        return proportionCommonUtilLibrariesToDependencies;
    }

    public void setProportionCommonUtilLibrariesToDependencies(int proportionCommonUtilLibrariesToDependencies) {
        this.proportionCommonUtilLibrariesToDependencies = proportionCommonUtilLibrariesToDependencies;
    }

    public int getProportionNonCommonUtilLibrariesToDependencies() {
        return proportionNonCommonUtilLibrariesToDependencies;
    }

    public void setProportionNonCommonUtilLibrariesToDependencies(int proportionNonCommonUtilLibrariesToDependencies) {
        this.proportionNonCommonUtilLibrariesToDependencies = proportionNonCommonUtilLibrariesToDependencies;
    }

    public int getNumberOfReadmesIncluded() {
        return numberOfReadmesIncluded;
    }

    public void setNumberOfReadmesIncluded(int numberOfReadmesIncluded) {
        this.numberOfReadmesIncluded = numberOfReadmesIncluded;
    }

    public int getNumberOfEnglishDocumentations() {
        return numberOfEnglishDocumentations;
    }

    public void setNumberOfEnglishDocumentations(int numberOfEnglishDocumentations) {
        this.numberOfEnglishDocumentations = numberOfEnglishDocumentations;
    }

    public double getPortionProjectsUsingReleaseTags() {
        return portionProjectsUsingReleaseTags;
    }

    public void setPortionProjectsUsingReleaseTags(double portionProjectsUsingReleaseTags) {
        this.portionProjectsUsingReleaseTags = portionProjectsUsingReleaseTags;
    }

    public void setMeanAmountOfDependenciesPerProjectInRepoProject(double meanAmountOfDependenciesPerProjectInRepoProject) {
        this.meanAmountOfDependenciesPerProjectInRepoProject = meanAmountOfDependenciesPerProjectInRepoProject;
    }

    public double getMeanDaysOfProjectDurationByCommit() {
        return meanDaysOfProjectDurationByCommit;
    }

    public void setMeanDaysOfProjectDurationByCommit(double meanDaysOfProjectDurationByCommit) {
        this.meanDaysOfProjectDurationByCommit = meanDaysOfProjectDurationByCommit;
    }

    public double getStdDaysOfProjectDurationByCommit() {
        return stdDaysOfProjectDurationByCommit;
    }

    public void setStdDaysOfProjectDurationByCommit(double stdDaysOfProjectDurationByCommit) {
        this.stdDaysOfProjectDurationByCommit = stdDaysOfProjectDurationByCommit;
    }

    public double getMedianDaysOfProjectDurationByCommit() {
        return medianDaysOfProjectDurationByCommit;
    }

    public void setMedianDaysOfProjectDurationByCommit(double medianDaysOfProjectDurationByCommit) {
        this.medianDaysOfProjectDurationByCommit = medianDaysOfProjectDurationByCommit;
    }

    public double getPercentile60DaysOfProjectDurationByCommit() {
        return percentile60DaysOfProjectDurationByCommit;
    }

    public void setPercentile60DaysOfProjectDurationByCommit(double percentile60DaysOfProjectDurationByCommit) {
        this.percentile60DaysOfProjectDurationByCommit = percentile60DaysOfProjectDurationByCommit;
    }

    public double getPercentile90DaysOfProjectDurationByCommit() {
        return percentile90DaysOfProjectDurationByCommit;
    }

    public void setPercentile90DaysOfProjectDurationByCommit(double percentile90DaysOfProjectDurationByCommit) {
        this.percentile90DaysOfProjectDurationByCommit = percentile90DaysOfProjectDurationByCommit;
    }

    public double getPercentile95DaysOfProjectDurationByCommit() {
        return percentile95DaysOfProjectDurationByCommit;
    }

    public void setPercentile95DaysOfProjectDurationByCommit(double percentile95DaysOfProjectDurationByCommit) {
        this.percentile95DaysOfProjectDurationByCommit = percentile95DaysOfProjectDurationByCommit;
    }

    public double getPercentile99DaysOfProjectDurationByCommit() {
        return percentile99DaysOfProjectDurationByCommit;
    }

    public void setPercentile99DaysOfProjectDurationByCommit(double percentile99DaysOfProjectDurationByCommit) {
        this.percentile99DaysOfProjectDurationByCommit = percentile99DaysOfProjectDurationByCommit;
    }

    public double getMaxDaysOfProjectDurationByCommit() {
        return maxDaysOfProjectDurationByCommit;
    }

    public void setMaxDaysOfProjectDurationByCommit(double maxDaysOfProjectDurationByCommit) {
        this.maxDaysOfProjectDurationByCommit = maxDaysOfProjectDurationByCommit;
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