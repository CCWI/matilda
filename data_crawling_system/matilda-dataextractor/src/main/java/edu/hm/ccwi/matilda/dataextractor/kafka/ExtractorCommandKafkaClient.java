package edu.hm.ccwi.matilda.dataextractor.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.dataextractor.service.DataExtractorService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.List;

@Component
public class ExtractorCommandKafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractorCommandKafkaClient.class);

    final DataExtractorService extractorService;

    public ExtractorCommandKafkaClient(DataExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    // Retrieve 1 Record (earlier 2) at a time and maximum delay between invocations of poll() (before failing) is 2d [earlier: 6h (20000000)]
    @KafkaListener(topics = "${matilda.kafka.extractor.topic}", properties= {"max.poll.interval.ms:259200000", "max.poll.records:1"})
    @SendTo(value = "${matilda.kafka.analyzer.topic}")
    public String processMessage(String triggerCrawlingDto, @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics) {
        if(StringUtils.isNotEmpty(triggerCrawlingDto)) {
            try {
                ProcessingProjectDto tcDto = deserializeDtoJson(triggerCrawlingDto);
                if (StringUtils.isNotEmpty(tcDto.getRepoProjName())) {
                    return extractorService.extractorProcessor(tcDto.getRepoName(), tcDto.getProjectName(),
                            tcDto.getProjectDir(), tcDto.getRepoSource());
                }
            } catch (Exception e) {
                LOG.error("Exception while processing Extraction - no analyses are executed!", e);
            }
        }
        return null;
    }

    ProcessingProjectDto deserializeDtoJson(String triggerCrawlingDto) {
        Gson gson = new GsonBuilder().registerTypeAdapter(LocalDate.class,
                (JsonDeserializer<LocalDate>) (JsonElement json, Type typeOfT, JsonDeserializationContext context) ->
                LocalDate.of(2010, 01, 01)).create(); //TODO Since wird jetzt fix ersetzt um deserialisierungsprobleme durch java 17 zu beheben. (im Betrieb kommt LocalDate komischerweise in unterschiedlichen Formaten an dieser Schnittstelle an.)
        return gson.fromJson(triggerCrawlingDto, ProcessingProjectDto.class);
    }
}