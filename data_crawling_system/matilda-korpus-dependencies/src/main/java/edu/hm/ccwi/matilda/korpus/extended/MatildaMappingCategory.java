package edu.hm.ccwi.matilda.korpus.extended;

public class MatildaMappingCategory {

    private String matildaCategory;
    private String mvnRepoCategory;
    private String mvnRepoCategoryCleaned;

    public MatildaMappingCategory() {
    }

    public String getMatildaCategory() {
        return matildaCategory;
    }

    public void setMatildaCategory(String matildaCategory) {
        this.matildaCategory = matildaCategory;
    }

    public String getMvnRepoCategory() {
        return mvnRepoCategory;
    }

    public void setMvnRepoCategory(String mvnRepoCategory) {
        this.mvnRepoCategory = mvnRepoCategory;
    }

    public String getMvnRepoCategoryCleaned() {
        return mvnRepoCategoryCleaned;
    }

    public void setMvnRepoCategoryCleaned(String mvnRepoCategoryCleaned) {
        this.mvnRepoCategoryCleaned = mvnRepoCategoryCleaned;
    }
}
