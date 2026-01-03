package edu.hm.ccwi.matilda.state;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"edu.hm.ccwi.matilda.base", "edu.hm.ccwi.matilda.state", "edu.hm.ccwi.matilda.persistence"})
@EntityScan(basePackages = {"edu.hm.ccwi.matilda.base.model", "edu.hm.ccwi.matilda.persistence"})
@EnableJpaRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.jpa.repo"})
public class TestContext {
}
