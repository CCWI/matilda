package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.CharacteristicEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CharacteristicRepository extends JpaRepository<CharacteristicEntity, Long> {

    Optional<CharacteristicEntity> findByName(String name);

}
