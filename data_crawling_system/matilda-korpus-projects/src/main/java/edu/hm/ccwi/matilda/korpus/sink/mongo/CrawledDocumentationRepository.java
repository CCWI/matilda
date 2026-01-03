package edu.hm.ccwi.matilda.korpus.sink.mongo;

import edu.hm.ccwi.matilda.korpus.model.CrawledDocumentation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CrawledDocumentationRepository extends MongoRepository<CrawledDocumentation, String> {

    default CrawledDocumentation getCrawledDocByRevId(String crawledRevId) {
        if (this.existsById(crawledRevId)) {
            Optional<CrawledDocumentation> crOpt = this.findById(crawledRevId);
            if (crOpt != null && crOpt.isPresent()) {
                return crOpt.get();
            }
        }
        return null;
    }
}


