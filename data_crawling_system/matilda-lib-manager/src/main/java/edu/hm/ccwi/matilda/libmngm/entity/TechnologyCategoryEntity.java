package edu.hm.ccwi.matilda.libmngm.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_TECHNOLOGY_CATEGORY")
public class TechnologyCategoryEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_TECHNOLOGY_CATEGORY_GENERATOR")
    @SequenceGenerator(name="MATILDA_TECHNOLOGY_CATEGORY_GENERATOR",sequenceName="MATILDA_TECHNOLOGY_CATEGORY_SEQUENCE", allocationSize=1)
    private Long technologyCategoryId;

    @Column(name = "TECHNOLOGY_NAME")
    private String technologyCategoryName;

    @Column(name = "TECHNOLOGY_SOURCE")
    private String source;

    @ManyToMany(targetEntity = LibraryTechnologyEntity.class, mappedBy = "technologyCategories", cascade = CascadeType.ALL)
    @Column(name = "TECHNOLOGIES")
    private List<LibraryTechnologyEntity> libraryTechnology;

    public TechnologyCategoryEntity() {
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

    public List<LibraryTechnologyEntity> getLibraryTechnology() {
        return libraryTechnology;
    }

    public void setLibraryTechnology(List<LibraryTechnologyEntity> libraryTechnology) {
        this.libraryTechnology = libraryTechnology;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TechnologyCategoryEntity that = (TechnologyCategoryEntity) o;
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