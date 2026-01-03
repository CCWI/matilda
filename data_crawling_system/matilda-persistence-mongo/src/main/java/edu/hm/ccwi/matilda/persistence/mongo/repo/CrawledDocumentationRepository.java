package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDocumentation;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CrawledDocumentationRepository extends MongoRepository<CrawledDocumentation, String> {

}


