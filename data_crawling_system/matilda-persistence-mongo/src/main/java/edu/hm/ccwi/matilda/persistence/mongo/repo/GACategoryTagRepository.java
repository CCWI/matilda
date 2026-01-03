package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GACategoryTagRepository extends MongoRepository<GACategoryTag, String> {
}
