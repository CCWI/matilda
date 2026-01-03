package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.UnknownCategoryTag;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UnknownCategoryTagRepository extends MongoRepository<UnknownCategoryTag, String> {

}


