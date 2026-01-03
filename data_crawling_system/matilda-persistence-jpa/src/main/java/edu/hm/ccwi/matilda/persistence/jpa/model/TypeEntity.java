package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "MATILDA_CHARACTERISTIC_TYPE")
public class TypeEntity implements Serializable {

    @Id
    @Column(name = "ID")
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_CHARACTERISTIC_TYPE_GENERATOR")
    @SequenceGenerator(name="MATILDA_CHARACTERISTIC_TYPE_GENERATOR",sequenceName="MMATILDA_CHARACTERISTIC_TYPE_SEQUENCE", allocationSize=1)
    private Long id;

    @Column(name = "TYPENAME")
    private String typename;

    public TypeEntity() {
    }

    public TypeEntity(String typename) {
        this.typename = typename;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTypename() {
        return typename;
    }

    public void setTypename(String typename) {
        this.typename = typename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypeEntity that = (TypeEntity) o;
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