package edu.hm.ccwi.matilda.libmngm.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_LIBRARY_TECHNOLOGY")
public class LibraryTechnologyEntity implements Serializable {

    @Id
    @Column(name = "TECHNOLOGY_ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_TECHNOLOGY_GENERATOR")
    @SequenceGenerator(name="MATILDA_TECHNOLOGY_GENERATOR",sequenceName="MATILDA_TECHNOLOGY_SEQUENCE", allocationSize=1)
    private Long technologyId;

    @Column(name = "TECHNOLOGY_NAME")
    private String technologyName;

    @Column(name = "TECHNOLOGY_LABELED_MANUALLY")
    private boolean labeledManually;

    @Column(name = "TECHNOLOGY_MODIFIED")
    private LocalDate modifiedEntry;

    @Column(name = "TECHNOLOGY_SOURCE")
    private String source;

    @ManyToMany(targetEntity = LibraryEntity.class, cascade = CascadeType.ALL)
    @Column(name = "LIBRARIES")
    private List<LibraryEntity> libraries;

    @ManyToMany(targetEntity = TechnologyCategoryEntity.class, cascade = CascadeType.ALL)
    @Column(name = "TECHNOLOGY_CATEGORY")
    private List<TechnologyCategoryEntity> technologyCategories;

    public LibraryTechnologyEntity() {
    }

    public LibraryTechnologyEntity(boolean labeledManually, LocalDate modifiedEntry, String source, List<LibraryEntity> libraries) {
        this.labeledManually = labeledManually;
        this.modifiedEntry = modifiedEntry;
        this.source = source;
        this.libraries = libraries;
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

    public boolean isLabeledManually() {
        return labeledManually;
    }

    public void setLabeledManually(boolean labeledManually) {
        this.labeledManually = labeledManually;
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

    public List<LibraryEntity> getLibraries() {
        return libraries;
    }

    public void setLibraries(List<LibraryEntity> libraries) {
        this.libraries = libraries;
    }

    public List<TechnologyCategoryEntity> getTechnologyCategories() {
        return technologyCategories;
    }

    public void setTechnologyCategories(List<TechnologyCategoryEntity> technologyCategories) {
        this.technologyCategories = technologyCategories;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryTechnologyEntity that = (LibraryTechnologyEntity) o;
        return Objects.equals(this.technologyId, that.technologyId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.technologyId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}