package edu.hm.ccwi.matilda.persistence.jpa.repo;

import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExtractedDesignDecisionRepository extends JpaRepository<ExtractedDesignDecisionEntity, Integer> {

    boolean existsByDecisionCommitTimeAndAndRepositoryAndProjectAndInitialAndTarget(LocalDateTime decisionCommitTime,
                                                                                    String repository,
                                                                                    String project,
                                                                                    String initial,
                                                                                    String target);

    List<ExtractedDesignDecisionEntity> findByDecisionSubject(String decisionSubject);

    List<ExtractedDesignDecisionEntity> findAllByInitial(String initial);

    List<ExtractedDesignDecisionEntity> findAllByTarget(String target);
}
