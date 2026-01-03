package edu.hm.ccwi.matilda.gateway.service;

import edu.hm.ccwi.matilda.gateway.dto.CrawlerRequestDto;
import edu.hm.ccwi.matilda.gateway.exception.GatewayException;
import edu.hm.ccwi.matilda.gateway.kafka.GatewayCommandKafkaClient;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AnalyzeServiceImpl implements AnalyzeService {

    private static final Logger LOG = LoggerFactory.getLogger(AnalyzeServiceImpl.class);

    final GatewayCommandKafkaClient gatewayCommandKafkaClient;

    public AnalyzeServiceImpl(GatewayCommandKafkaClient gatewayCommandKafkaClient) {
        this.gatewayCommandKafkaClient = gatewayCommandKafkaClient;
    }

    /**
     * Create Matilda Request
     *
     * @param url
     * @param matildaId
     * @return
     * @throws Exception
     */
    public String analyzeProject(String url, String matildaId) throws Exception {
        if (isValidUrl(url)) {
            String matildaRequestId = matildaId != null ? UUID.fromString(matildaId).toString() : UUID.randomUUID().toString();
            gatewayCommandKafkaClient.processMessage(new CrawlerRequestDto(matildaRequestId, url));
            return matildaRequestId;
        } else {
            LOG.error("Url is not accepted as valid: {}", url);
            throw new GatewayException("No valid url");
        }
    }

    boolean isValidUrl(String url) {
        return new UrlValidator().isValid(url) && isValidUrlDomain(url);
    }

    private boolean isValidUrlDomain(String url) {
        if (url.toLowerCase().contains("github.com") ||
            url.toLowerCase().contains("gitlab.com") ||
            url.toLowerCase().contains("bitbucket.org")) {
            return true;
        }
        return false;
    }
}
