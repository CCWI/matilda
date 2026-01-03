package edu.hm.ccwi.matilda.base.model.library;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

public class TechnologyCategory implements Serializable {

    private Long technologyCategoryId;

    private String technologyCategoryName;

    private String source;

    private List<LibraryTechnology> libraryTechnology;

    public TechnologyCategory() {
    }

    public Long getTechnologyCategoryId() {
        return technologyCategoryId;
    }

    public void setTechnologyCategoryId(Long technologyCategoryId) {
        this.technologyCategoryId = technologyCategoryId;
    }

    public String getTechnologyCategoryName() {
        return technologyCategoryName;
    }

    public void setTechnologyCategoryName(String technologyCategoryName) {
        this.technologyCategoryName = technologyCategoryName;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<LibraryTechnology> getLibraryTechnology() {
        return libraryTechnology;
    }

    public void setLibraryTechnology(List<LibraryTechnology> libraryTechnology) {
        this.libraryTechnology = libraryTechnology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnologyCategory that = (TechnologyCategory) o;
        return Objects.equals(technologyCategoryId, that.technologyCategoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technologyCategoryId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}