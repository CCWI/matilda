package edu.hm.ccwi.matilda.korpus.service.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Object to create Matrix.
 */
public class MatrixKorpusRow {

    public String id;

    public String rp;

    public String ga;

    public List<String> dependencyGA;

    public MatrixKorpusRow(String id, String rp, String ga) {
        this.id = id;
        this.rp = rp;
        this.ga = ga;
        this.dependencyGA = new ArrayList<>();
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRp() {
        return rp;
    }

    public void setRp(String rp) {
        this.rp = rp;
    }

    public String getGa() {
        return ga;
    }

    public void setGa(String ga) {
        this.ga = ga;
    }

    public List<String> getDependencyGA() {
        return dependencyGA;
    }

    public void setDependencyGA(List<String> dependencyGA) {
        this.dependencyGA = dependencyGA;
    }
}
