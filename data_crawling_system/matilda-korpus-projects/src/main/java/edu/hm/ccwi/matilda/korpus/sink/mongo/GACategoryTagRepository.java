package edu.hm.ccwi.matilda.korpus.sink.mongo;

import edu.hm.ccwi.matilda.korpus.model.GACategoryTag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface GACategoryTagRepository extends MongoRepository<GACategoryTag, String> {
}
