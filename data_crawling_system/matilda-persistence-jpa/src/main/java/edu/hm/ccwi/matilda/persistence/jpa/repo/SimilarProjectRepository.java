package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.SimilarProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimilarProjectRepository extends JpaRepository<SimilarProjectEntity, Long> {

    List<SimilarProjectEntity> findAllBySourceProject_MatildaRequestId(String matildaRequestId);
}
