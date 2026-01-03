package edu.hm.ccwi.matilda.analyzer.kafka;

import edu.hm.ccwi.matilda.analyzer.service.AnalyzeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnalyzeCommandKafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeCommandKafkaClient.class);

    private final AnalyzeService analyzeService;

    public AnalyzeCommandKafkaClient(AnalyzeService analyzeService) {
        this.analyzeService = analyzeService;
    }

    // Retrieve 1 Record at a time and maximum delay between invocations of poll() (before failing) is 12h
    @KafkaListener(topics = "${matilda.kafka.analyzer.topic}", properties = {"max.poll.interval.ms:43200000", "max.poll.records:1"})
    public String processMessage(String extractedRepoId, @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics) {
        if (extractedRepoId != null) {
            return analyzeService.analyzeProject(extractedRepoId);
        } else {
            // null value means a crawled project, which was not relevant or an exception occurred. Count
            LOG.warn("ExtractedRepoId is null -> Skip, since it is not relevant or an exception occurred...");
        }
        return null;
    }
}