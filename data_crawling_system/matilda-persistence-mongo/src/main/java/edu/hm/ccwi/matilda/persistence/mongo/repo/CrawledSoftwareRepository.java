package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CrawledSoftwareRepository extends MongoRepository<CrawledRepository, String> {

    /**
     * Get CrawledRepository by id from MongoDB.
     *
     * @param crawledRepositoryId
     * @return CrawledRepository if exists, else null.
     */
    default CrawledRepository getCrawledRepositoryById(String crawledRepositoryId) {
        if (this.existsById(crawledRepositoryId)) {
            Optional<CrawledRepository> crOpt = this.findById(crawledRepositoryId);
            if (crOpt.isPresent()) {
                return crOpt.get();
            }
        }
        return null;
    }
}