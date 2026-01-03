package edu.hm.ccwi.matilda.analyzer.service.dbmigrationlibraries;

import edu.hm.ccwi.matilda.analyzer.MatildaAnalyzerApplication;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaClient;
import edu.hm.ccwi.matilda.analyzer.kafka.AnalyzeCommandKafkaSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;

/**
 * NO TEST!!!
 *
 */
@SpringBootTest(classes = MatildaAnalyzerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
class TechnologyAndCategoryMigrator {

    @Inject
    MigrationService migrationService;

    @MockBean
    AnalyzeCommandKafkaClient analyzeCommandKafkaClient;

    @MockBean
    AnalyzeCommandKafkaSender analyzeCommandKafkaSender;

    @Test
    void importTechnologiesByImplementedList() throws Exception {
        migrationService.importTechnologiesByImplementedList();
    }
}