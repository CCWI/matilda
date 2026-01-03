package edu.hm.ccwi.matilda.libmngm.repository;

import edu.hm.ccwi.matilda.libmngm.entity.LibraryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LibraryRepository extends JpaRepository<LibraryEntity, String> {

        Optional<LibraryEntity> findByGroupArtifactId(String groupArtifactId);

}
