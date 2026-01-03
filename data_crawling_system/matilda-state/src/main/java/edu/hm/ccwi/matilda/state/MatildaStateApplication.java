package edu.hm.ccwi.matilda.state;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableEurekaClient
@ComponentScan(basePackages = {"edu.hm.ccwi.matilda.base", "edu.hm.ccwi.matilda.state", "edu.hm.ccwi.matilda.persistence"})
@EntityScan(basePackages = {"edu.hm.ccwi.matilda.base.model", "edu.hm.ccwi.matilda.persistence"})
@EnableJpaRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.jpa.repo"})
public class MatildaStateApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatildaStateApplication.class, args);
    }

}