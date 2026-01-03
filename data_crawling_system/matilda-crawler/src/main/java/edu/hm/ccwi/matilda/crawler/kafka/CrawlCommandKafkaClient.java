package edu.hm.ccwi.matilda.crawler.kafka;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.crawler.kafka.dto.CrawlerRequestDto;
import edu.hm.ccwi.matilda.crawler.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Component;

import javax.xml.bind.ValidationException;
import java.io.File;
import java.time.LocalDate;
import java.util.List;

@Component
public class CrawlCommandKafkaClient {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlCommandKafkaClient.class);

    private final Gson gson;

    @Value("${matilda.crawler.directory}")
    private String rootDirectoryPath;

    @Value("${matilda.crawler.commits.since}")
    private String commitsSince;

    @Value("${matilda.crawler.useJanitor}")
    private boolean useJanitor;

    @Value("${matilda.crawler.project.updates}")
    private boolean projectUpdates;
    CrawlerProcessor crawlerProcessor;

    public CrawlCommandKafkaClient(CrawlerProcessor crawlerProcessor) {
        this.crawlerProcessor = crawlerProcessor;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .create(); //.setPrettyPrinting()
    }

    // Retrieve 1 Record at a time and maximum delay between invocations of poll() (before failing) is 20h
    @KafkaListener(topics = "${matilda.kafka.gateway.topic}", properties = {"max.poll.interval.ms:72000000", "max.poll.records:1"})
    @SendTo(value = "${matilda.kafka.extractor.topic}")
    public String processGatewayRequestMessage(String requestDto, @Header(KafkaHeaders.RECEIVED_TOPIC) List<String> topics) throws Exception {
        LOG.info("");
        ProcessingProjectDto enrichedCrawlingDto = null;

        try {
            enrichedCrawlingDto = createProcessingProjectDto(new Gson().fromJson(requestDto, CrawlerRequestDto.class));
        } catch (JsonSyntaxException e) {
            LOG.error("Unable to get CrawlerRequest from json.", e);
        }

        return crawlerProcessor.processProjectDto(enrichedCrawlingDto) ? serializeToJsonString(enrichedCrawlingDto) : null;
    }

    String serializeToJsonString(ProcessingProjectDto enrichedCrawlingDto) {
        return gson.toJson(enrichedCrawlingDto);
    }

    ProcessingProjectDto createProcessingProjectDto(CrawlerRequestDto tcDto) throws Exception {
        ProcessingProjectDto processingDto = null;
        if (tcDto != null) {
            String[] uriToken = tcDto.getUri().replace("https://", "").replace("http://", "").split("/");

            if (uriToken.length < 3) {
                LOG.error("received uri seems to be wrong. found token: {}", uriToken.length);
                throw new Exception("received uri seems to be wrong. found token: " + uriToken.length);
            }

            String repoName = uriToken[1];
            String projectName = uriToken[2];

            if (StringUtils.isEmpty(repoName) || StringUtils.isEmpty(projectName)) {
                throw new ValidationException("Received repository-name or project-name is empty. STOP processing...");
            }

            processingDto = new ProcessingProjectDto();
            processingDto.setMatildaId(tcDto.getMatildaId());
            processingDto.setRepoProjName(repoName + "/" + projectName);
            processingDto.setProjectName(projectName);
            processingDto.setRepoName(repoName);
            processingDto.setRepoSource(getRepoSourceFromUrl(tcDto.getUri()));
            processingDto.setTargetPath(rootDirectoryPath);
            processingDto.setProjectDir(rootDirectoryPath + File.separator + repoName + File.separator + projectName);
            processingDto.setSince(LocalDate.parse(commitsSince));
            processingDto.setJanitor(useJanitor);
            processingDto.setRepoUpdateable(projectUpdates);
            processingDto.setUri(tcDto.getUri());
        }
        return processingDto;
    }

    private RepoSource getRepoSourceFromUrl(String url) {
        if (url.toLowerCase().contains("github.com")) {
            return RepoSource.github;
        }
        if (url.toLowerCase().contains("gitlab.com")) {
            return RepoSource.gitlab;
        }
        if (url.toLowerCase().contains("bitbucket.org")) {
            return RepoSource.bitbucket;
        }
        return null;
    }
}