package edu.hm.ccwi.matilda.base.model.library;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.Objects;

public class LibraryCategory implements Serializable {

    private Long libraryCategoryId;

    private String libraryCategoryLabel;

    /**
     * Default-Constructor.
     */
    public LibraryCategory() {
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
        LibraryCategory that = (LibraryCategory) o;
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