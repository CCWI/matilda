package edu.hm.ccwi.matilda.korpus.libsim;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Objects;

public class LibSimAnalyzedLibrary extends LibSimResult implements Serializable {

    private String id;
    private String group;
    private String artifact;

    public LibSimAnalyzedLibrary(String id, String group, String artifact) {
        super();
        this.id = id;
        this.group = group;
        this.artifact = artifact;
    }

    public LibSimAnalyzedLibrary(String id, String group, String artifact, LibSimResult libSimResult) {
        super(libSimResult);
        this.id = id;
        this.group = group;
        this.artifact = artifact;
    }

    public LibSimAnalyzedLibrary(boolean success, String prediction, String percent, String tags, String id, String group, String artifact) {
        super(success, prediction, percent, tags);
        this.id = id;
        this.group = group;
        this.artifact = artifact;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getArtifact() {
        return artifact;
    }

    public void setArtifact(String artifact) {
        this.artifact = artifact;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibSimAnalyzedLibrary that = (LibSimAnalyzedLibrary) o;
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
