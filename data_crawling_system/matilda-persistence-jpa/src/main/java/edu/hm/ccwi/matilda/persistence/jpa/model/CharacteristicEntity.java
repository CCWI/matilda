package edu.hm.ccwi.matilda.persistence.jpa.model;

import org.apache.commons.lang3.builder.ToStringBuilder;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "MATILDA_CHARACTERISTIC")
public class CharacteristicEntity implements Serializable {

    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE,generator="MATILDA_CHARACTERISTIC_GENERATOR")
    @SequenceGenerator(name="MATILDA_CHARACTERISTIC_GENERATOR",sequenceName="MATILDA_CHARACTERISTIC_SEQUENCE", allocationSize=1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "SOURCE")
    private String source;

    @Column(name = "TYPE_ID")
    private Long type_id;

    @ManyToMany(targetEntity = LibraryEntity.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @Column(name = "LIBRARIES")
    private Set<LibraryEntity> libraries;

    @ManyToMany(targetEntity = LibCategoryEntity.class, cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @Column(name = "LIBCATEGORIES")
    private Set<LibCategoryEntity> libCategories;

    @ManyToMany
    @Column(name = "CHARACTERISTIC_REFERENCES_TO")
    private Set<CharacteristicEntity> characteristicReferencesTo;

    @ManyToMany(mappedBy="characteristicReferencesTo")
    @Column(name = "CHARACTERISTIC_REFERENCES_FROM")
    private Set<CharacteristicEntity> characteristicReferencesFrom;

    public CharacteristicEntity() {
    }

    public CharacteristicEntity(String name, Long type_id, Set<LibraryEntity> libraries,
                                Set<LibCategoryEntity> libCategories) {
        this.name = name;
        this.type_id = type_id;
        this.libraries = libraries;
        this.libCategories = libCategories;
    }

    public CharacteristicEntity(String name, String source, Long type_id, Set<LibraryEntity> libraries,
                                Set<LibCategoryEntity> libCategories, Set<CharacteristicEntity> characteristicReferencesTo,
                                Set<CharacteristicEntity> characteristicReferencesFrom) {
        this.name = name;
        this.source = source;
        this.type_id = type_id;
        this.libraries = libraries;
        this.libCategories = libCategories;
        this.characteristicReferencesTo = characteristicReferencesTo;
        this.characteristicReferencesFrom = characteristicReferencesFrom;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getType_id() {
        return type_id;
    }

    public void setType_id(Long type_id) {
        this.type_id = type_id;
    }

    public Set<LibraryEntity> getLibraries() {
        return libraries;
    }

    public void setLibraries(Set<LibraryEntity> libraries) {
        this.libraries = libraries;
    }

    public Set<LibCategoryEntity> getLibCategories() {
        return libCategories;
    }

    public void setLibCategories(Set<LibCategoryEntity> libCategories) {
        this.libCategories = libCategories;
    }

    public Set<CharacteristicEntity> getCharacteristicReferencesTo() {
        return characteristicReferencesTo;
    }

    public void setCharacteristicReferencesTo(Set<CharacteristicEntity> characteristicReferencesTo) {
        this.characteristicReferencesTo = characteristicReferencesTo;
    }

    public Set<CharacteristicEntity> getCharacteristicReferencesFrom() {
        return characteristicReferencesFrom;
    }

    public void setCharacteristicReferencesFrom(Set<CharacteristicEntity> characteristicReferencesFrom) {
        this.characteristicReferencesFrom = characteristicReferencesFrom;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CharacteristicEntity that = (CharacteristicEntity) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}