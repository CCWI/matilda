package edu.hm.ccwi.matilda.persistence.mongo.model;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

public class UnknownCategoryTag {

    @Id
    private String name;

    private boolean isCategory;

    private String foundInDependency;

    public UnknownCategoryTag(String name, boolean isCategory, String foundInDependency) {
        this.name = name;
        this.isCategory = isCategory;
        this.foundInDependency = foundInDependency;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCategory() {
        return isCategory;
    }

    public void setCategory(boolean category) {
        isCategory = category;
    }

    public String getFoundInDependency() {
        return foundInDependency;
    }

    public void setFoundInDependency(String foundInDependency) {
        this.foundInDependency = foundInDependency;
    }

    @Override
    public boolean equals(final Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
