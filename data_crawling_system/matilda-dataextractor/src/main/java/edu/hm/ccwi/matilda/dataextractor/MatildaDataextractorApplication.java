package edu.hm.ccwi.matilda.dataextractor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(basePackages = {"edu.hm.ccwi.matilda.base", "edu.hm.ccwi.matilda.dataextractor", "edu.hm.ccwi.matilda.persistence"})
@EntityScan(basePackages = {"edu.hm.ccwi.matilda.base.model", "edu.hm.ccwi.matilda.persistence"})
@EnableMongoRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.mongo.repo"})
@EnableJpaRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.jpa.repo"})
public class MatildaDataextractorApplication {

    public static void main(String[] args) {
        SpringApplication.run(MatildaDataextractorApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}