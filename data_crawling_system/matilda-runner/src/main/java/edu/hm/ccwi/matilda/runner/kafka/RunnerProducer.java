package edu.hm.ccwi.matilda.runner.kafka;

import com.google.gson.Gson;
import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.runner.config.RunnerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.json.JSONException;

public class RunnerProducer {

    private KafkaProducer producer;

    public RunnerProducer() {
        this.producer = new KafkaProducer(RunnerConfig.kafkaProducerProperties);
    }

    public void sendObjectToKafka(String topic, ProcessingProjectDto triggerDto) {
        try {
            producer.send(new ProducerRecord<>(topic, new Gson().toJson(triggerDto)));
        } catch (JSONException e) {
            System.err.println(e.getMessage());
        }
    }
}
