package edu.hm.ccwi.matilda.dataextractor.service;


public class MvnRepoPage {

    String htmlPage;
    String category;
    String[] tagMatches;

    public MvnRepoPage(String htmlPage, String category, String[] tagMatches) {
        this.htmlPage = htmlPage;
        this.category = category;
        this.tagMatches = tagMatches;
    }

    public String getHtmlPage() {
        return htmlPage;
    }

    public void setHtmlPage(String htmlPage) {
        this.htmlPage = htmlPage;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String[] getTagMatches() {
        return tagMatches;
    }

    public void setTagMatches(String[] tagMatches) {
        this.tagMatches = tagMatches;
    }
}
