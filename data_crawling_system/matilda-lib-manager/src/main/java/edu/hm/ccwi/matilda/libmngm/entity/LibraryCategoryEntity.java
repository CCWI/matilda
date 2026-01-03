package edu.hm.ccwi.matilda.libmngm.entity;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_LIBRARY_CATEGORY")
public class LibraryCategoryEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_LIBRARY_CATEGORY_GENERATOR")
    @SequenceGenerator(name="MATILDA_LIBRARY_CATEGORY_GENERATOR",sequenceName="MATILDA_LIBRARY_CATEGORY_SEQUENCE", allocationSize=1)
    @Column(name = "LIBRARY_CATEGORY_ID")
    private Long libraryCategoryId;

    @Column(name = "LIBRARY_CATEGORY_LABEL")
    private String libraryCategoryLabel;

    /**
     * Default-Constructor.
     */
    public LibraryCategoryEntity() {
    }

    public Long getLibraryCategoryId() {
        return libraryCategoryId;
    }

    public void setLibraryCategoryId(Long libraryCategoryId) {
        this.libraryCategoryId = libraryCategoryId;
    }

    public String getLibraryCategoryLabel() {
        return libraryCategoryLabel;
    }

    public void setLibraryCategoryLabel(String libraryCategoryLabel) {
        this.libraryCategoryLabel = libraryCategoryLabel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryCategoryEntity that = (LibraryCategoryEntity) o;
        return Objects.equals(libraryCategoryId, that.libraryCategoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(libraryCategoryId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}