package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.persistence.jpa.repo.CharacteristicRepository;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibCategoryRepository;
import edu.hm.ccwi.matilda.persistence.jpa.repo.TypeRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.CharacteristicEntity;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.model.TypeEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class CharacteristicServiceImpl implements CharacteristicService {

    private static final Logger LOG = LoggerFactory.getLogger(CharacteristicService.class);

    final LibCategoryRepository libCategoryRepository;
    final TypeRepository typeRepository;
    final CharacteristicRepository characteristicRepository;

    public CharacteristicServiceImpl(LibCategoryRepository libCategoryRepository, TypeRepository typeRepository,
                                     CharacteristicRepository characteristicRepository) {
        this.libCategoryRepository = libCategoryRepository;
        this.typeRepository = typeRepository;
        this.characteristicRepository = characteristicRepository;
    }


    @Override
    public List<CharacteristicEntity> getAllCharacteristics() {
        return null;
    }

    @Override
    public List<CharacteristicEntity> getCharacteristicsByLibCategoryAndType(String libCategory, String type) {
        TypeEntity typeEntity = typeRepository.findByTypename(type).get();
        LibCategoryEntity libCategoryEntity = libCategoryRepository.findByLabel(libCategory).get();

        List<CharacteristicEntity> typeFilteredCharacteristicEntities = new ArrayList<>();
        for (CharacteristicEntity characteristic : libCategoryEntity.getCharacteristics()) {
            if(Objects.equals(typeEntity.getId(), characteristic.getType_id())) {
                typeFilteredCharacteristicEntities.add(characteristic);
            }
        }

        return typeFilteredCharacteristicEntities;
    }
}
