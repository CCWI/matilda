package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.RecommendationDesignDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecommendationDesignDecisionRepository extends JpaRepository<RecommendationDesignDecisionEntity, Long> {

    List<RecommendationDesignDecisionEntity> findAllByRecommendationId(Long recommendationId);
}