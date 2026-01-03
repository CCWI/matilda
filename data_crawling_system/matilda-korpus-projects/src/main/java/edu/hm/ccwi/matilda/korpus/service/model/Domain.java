package edu.hm.ccwi.matilda.korpus.service.model;

import java.util.ArrayList;
import java.util.List;

public class Domain {

    String mvnCategory;

    List<String> mvnTags;

    public Domain() {
        mvnTags = new ArrayList<>();
    }

    public synchronized void addMvnTag(String tag) {
        mvnTags.add(tag);
    }

    public String getMvnCategory() {
        return mvnCategory;
    }

    public void setMvnCategory(String mvnCategory) {
        this.mvnCategory = mvnCategory;
    }

    public List<String> getMvnTags() {
        return mvnTags;
    }

    public void setMvnTags(List<String> mvnTags) {
        this.mvnTags = mvnTags;
    }

    @Override
    public String toString() {
        return "Domain{" +
                "mvnCategory='" + mvnCategory + '\'' +
                ", mvnTags=" + mvnTags +
                '}';
    }
}
