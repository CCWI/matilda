package edu.hm.ccwi.matilda.gateway.kafka;

import com.google.gson.Gson;
import edu.hm.ccwi.matilda.gateway.dto.CrawlerRequestDto;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GatewayCommandKafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(GatewayCommandKafkaClient.class);

    private final GatewayCommandKafkaConfig config;

    public GatewayCommandKafkaClient(GatewayCommandKafkaConfig config) {
        this.config = config;
    }

    public void processMessage(CrawlerRequestDto request) throws KafkaException {
        LOG.info("Send new matilda-request: {} ", request);
        KafkaProducer producer = new KafkaProducer(config.getProducerProperties());
        producer.send(new ProducerRecord<>(config.getTopic(), new Gson().toJson(request)), (recordMetadata, e) -> {
            if (e != null) {
                throw new KafkaException("Asynchronous send failed: ", e);
            }
        });
        producer.close();
    }
}