package edu.hm.ccwi.matilda.korpus.sink.mongo;

import edu.hm.ccwi.matilda.korpus.model.CrawledRepository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface CrawledSoftwareRepository extends MongoRepository<CrawledRepository, String> {

    List<CrawledRepository> findByRepositoryName(String repositoryName);

    /**
     * Get CrawledRepository by id from MongoDB.
     *
     * @param crawledRepositoryId
     * @return CrawledRepository if exists, else null.
     */
    default CrawledRepository getCrawledRepositoryById(String crawledRepositoryId) {
        if (this.existsById(crawledRepositoryId)) {
            Optional<CrawledRepository> crOpt = this.findById(crawledRepositoryId);
            if (crOpt != null && crOpt.isPresent()) {
                return crOpt.get();
            }
        }
        return null;
    }

}


