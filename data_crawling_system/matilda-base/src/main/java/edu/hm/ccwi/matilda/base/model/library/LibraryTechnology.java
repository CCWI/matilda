package edu.hm.ccwi.matilda.base.model.library;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class LibraryTechnology implements Serializable {

    private Long technologyId;

    private String technologyName;

    private LocalDate modifiedEntry;

    private String source;

    private List<Library> libraries;

    private List<TechnologyCategory> technologyCategories;

    public LibraryTechnology() {
    }

    public LibraryTechnology(String technologyName, LocalDate modifiedEntry, String source,
                             List<Library> libraries, List<TechnologyCategory> technologyCategories) {
        this.technologyName = technologyName;
        this.modifiedEntry = modifiedEntry;
        this.source = source;
        this.libraries = libraries;
        this.technologyCategories = technologyCategories;
    }

    public Long getTechnologyId() {
        return technologyId;
    }

    public void setTechnologyId(Long technologyId) {
        this.technologyId = technologyId;
    }

    public String getTechnologyName() {
        return technologyName;
    }

    public void setTechnologyName(String technologyName) {
        this.technologyName = technologyName;
    }

    public LocalDate getModifiedEntry() {
        return modifiedEntry;
    }

    public void setModifiedEntry(LocalDate modifiedEntry) {
        this.modifiedEntry = modifiedEntry;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<Library> libraries) {
        this.libraries = libraries;
    }

    public List<TechnologyCategory> getTechnologyCategories() {
        return technologyCategories;
    }

    public void setTechnologyCategories(List<TechnologyCategory> technologyCategories) {
        this.technologyCategories = technologyCategories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryTechnology that = (LibraryTechnology) o;
        return Objects.equals(technologyId, that.technologyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technologyId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}