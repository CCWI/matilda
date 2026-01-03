package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTagTotal;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GACategoryTagTotalRepository extends MongoRepository<GACategoryTagTotal, String> {
}
