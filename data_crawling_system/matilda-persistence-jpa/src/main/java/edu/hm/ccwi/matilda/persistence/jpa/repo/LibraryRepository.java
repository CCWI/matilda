package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibraryRepository extends JpaRepository<LibraryEntity, Long> {

        Optional<LibraryEntity> findByGroupArtifactId(String groupArtifactId);

}
