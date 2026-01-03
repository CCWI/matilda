package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_LIBCATEGORY")
public class LibCategoryEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_LIBCATEGORY_GENERATOR")
    @SequenceGenerator(name="MATILDA_LIBCATEGORY_GENERATOR",sequenceName="MATILDA_LIBCATEGORY_SEQUENCE", allocationSize=1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "LABEL")
    private String label;

    @Column(name = "RELEVANT")
    private String relevant;

    @ManyToMany(targetEntity = CharacteristicEntity.class, mappedBy = "libCategories", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Column(name = "CHARACTERISTICS")
    private List<CharacteristicEntity> characteristics;

    /**
     * Default-Constructor.
     */
    public LibCategoryEntity() {
    }

    public LibCategoryEntity(Long id, String label, String relevant) {
        this.id = id;
        this.label = label;
        this.relevant = relevant;
    }

    public LibCategoryEntity(String label) {
        this.label = label;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRelevant() {
        return relevant;
    }

    public void setRelevant(String relevant) {
        this.relevant = relevant;
    }

    public List<CharacteristicEntity> getCharacteristics() {
        return characteristics;
    }

    public void setCharacteristics(List<CharacteristicEntity> characteristics) {
        this.characteristics = characteristics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibCategoryEntity that = (LibCategoryEntity) o;
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