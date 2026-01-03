package edu.hm.ccwi.matilda.analyzer.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class AnalyzeCommandKafkaSender {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeCommandKafkaSender.class);

    @Value("${matilda.kafka.analyzer.topic}")
    private String analyzerTopic;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * Send again to Analyzer-Topic to start analyzing the repository.
     */
    public void analyzeRepositoryInMatildaProcess(String extractedRepoId) {
        LOG.info("Send extractedRepoId: {} to analyzer-topic", extractedRepoId);
        kafkaTemplate.send(analyzerTopic, extractedRepoId);
    }
}