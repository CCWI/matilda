package edu.hm.ccwi.matilda.dataextractor;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"edu.hm.ccwi.matilda.base", "edu.hm.ccwi.matilda.dataextractor", "edu.hm.ccwi.matilda.persistence"})
@EntityScan(basePackages = {"edu.hm.ccwi.matilda.base.model", "edu.hm.ccwi.matilda.persistence"})
@EnableMongoRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.mongo.repo"})
@EnableJpaRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.jpa.repo"})
public class TestContext {
}
