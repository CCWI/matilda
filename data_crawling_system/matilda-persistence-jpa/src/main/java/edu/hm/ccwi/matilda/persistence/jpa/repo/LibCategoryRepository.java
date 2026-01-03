package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibCategoryRepository extends JpaRepository<LibCategoryEntity, Long> {

    Optional<LibCategoryEntity> findByLabel(String label);
}
