package edu.hm.ccwi.matilda.gateway.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
@ConfigurationProperties(prefix="kafka")
public class GatewayCommandKafkaConfig {

    private String topic;

    private String bootstrapServers;

    private String retries;

    private String keySerializer;

    private String valueSerializer;

    public Properties getProducerProperties() {
        Properties kafkaProducerProperties = new Properties();
        kafkaProducerProperties.put("bootstrap.servers", bootstrapServers);
        kafkaProducerProperties.put("retries", retries);
        kafkaProducerProperties.put("key.serializer", keySerializer);
        kafkaProducerProperties.put("value.serializer", valueSerializer);

        return kafkaProducerProperties;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getRetries() {
        return retries;
    }

    public void setRetries(String retries) {
        this.retries = retries;
    }

    public String getKeySerializer() {
        return keySerializer;
    }

    public void setKeySerializer(String keySerializer) {
        this.keySerializer = keySerializer;
    }

    public String getValueSerializer() {
        return valueSerializer;
    }

    public void setValueSerializer(String valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
}