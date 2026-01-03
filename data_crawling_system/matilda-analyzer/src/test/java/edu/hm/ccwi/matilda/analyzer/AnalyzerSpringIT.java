package edu.hm.ccwi.matilda.analyzer;

import edu.hm.ccwi.matilda.analyzer.service.AnalyzeService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = MatildaAnalyzerApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Disabled("Integration test requires MongoDB and Kafka infrastructure")
public class AnalyzerSpringIT {

    @Autowired(required = false)
    AnalyzeService analyzeService;

    @Test
    public void startSpringContext() {
        // DO NOTHING - Just start spring context
    }
}
