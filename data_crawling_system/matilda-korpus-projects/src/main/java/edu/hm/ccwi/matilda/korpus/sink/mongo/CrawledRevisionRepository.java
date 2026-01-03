package edu.hm.ccwi.matilda.korpus.sink.mongo;

import edu.hm.ccwi.matilda.korpus.model.CrawledRevision;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CrawledRevisionRepository extends MongoRepository<CrawledRevision, String> {

    default CrawledRevision getCrawledRevById(String crawledRevId) {
        if (this.existsById(crawledRevId)) {
            Optional<CrawledRevision> crOpt = this.findById(crawledRevId);
            if (crOpt != null && crOpt.isPresent()) {
                return crOpt.get();
            }
        }
        return null;
    }
}


