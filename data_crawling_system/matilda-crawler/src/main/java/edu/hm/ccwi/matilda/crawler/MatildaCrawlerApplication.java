package edu.hm.ccwi.matilda.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoRepositories(basePackages = {"edu.hm.ccwi.matilda.persistence.mongo.repo"})
public class MatildaCrawlerApplication {

    /**
     * [Q] Branches können unabhängig voneinander entwickelt werden:
     * - Verschiedene Projekte/Branch. -> Jeden Branch als eigenes Projekt für similarity-Analyse betrachten.
     * - Weiterentwicklungen, so dass ein Branch auf einem anderen aufbaut. -> Den neuesten finden.
     *
     * [Q] Welche Dokumentation soll herangezogen werden, wenn MD in verschiedenen Versionen und Branches vorliegen?
     * - Immer die neuste des jeweiligen Branches
     */
    public static void main(String[] args) {
        SpringApplication.run(MatildaCrawlerApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }
}