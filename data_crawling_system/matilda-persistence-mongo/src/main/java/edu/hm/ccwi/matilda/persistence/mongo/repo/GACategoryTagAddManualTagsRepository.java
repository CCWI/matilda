package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTagAddManualTags;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GACategoryTagAddManualTagsRepository extends MongoRepository<GACategoryTagAddManualTags, String> {
}
