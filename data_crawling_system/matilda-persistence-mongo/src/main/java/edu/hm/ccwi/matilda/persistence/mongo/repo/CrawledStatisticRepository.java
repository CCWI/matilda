package edu.hm.ccwi.matilda.persistence.mongo.repo;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledStatistic;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CrawledStatisticRepository extends MongoRepository<CrawledStatistic, String> {

}


