package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CrawledRevisionRepository extends MongoRepository<CrawledRevision, String> {

    default CrawledRevision getCrawledRevById(String crawledRevId) {
        if (this.existsById(crawledRevId)) {
            Optional<CrawledRevision> crOpt = this.findById(crawledRevId);
            if (crOpt.isPresent()) {
                return crOpt.get();
            }
        }
        return null;
    }

    CrawledRevision getCrawledRevisionByCommitId(String commitId);

    @Query(value="{}",fields="{ 'commitId' : 1, 'commitDate' : 1}")
    List<CrawledRevision> findAllByCommitId();

}


