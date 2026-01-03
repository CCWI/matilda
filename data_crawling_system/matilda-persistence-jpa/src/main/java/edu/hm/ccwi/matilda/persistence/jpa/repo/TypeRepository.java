package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.TypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TypeRepository extends JpaRepository<TypeEntity, Long> {

    boolean existsByTypename(String typename);

    Optional<TypeEntity> findByTypename(String typename);
}
