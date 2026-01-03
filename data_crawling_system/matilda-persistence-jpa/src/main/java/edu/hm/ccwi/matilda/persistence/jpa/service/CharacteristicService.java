package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.persistence.jpa.model.CharacteristicEntity;

import java.util.List;

public interface CharacteristicService {

    List<CharacteristicEntity> getAllCharacteristics();

    List<CharacteristicEntity> getCharacteristicsByLibCategoryAndType(String libCategory, String type);
}
