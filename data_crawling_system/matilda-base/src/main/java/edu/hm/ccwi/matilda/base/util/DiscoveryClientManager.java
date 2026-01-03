package edu.hm.ccwi.matilda.base.util;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class DiscoveryClientManager {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryClientManager.class);
    private static final String PROTOCOL = "http://";
    private static final int RETRY_TIME = 3000;
    public static final String MATILDA_STATE_NAME = "MATILDA-STATE";

    private final EurekaClient discoveryClient;

    public DiscoveryClientManager(EurekaClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    protected String retrieveBaseUrl(String serviceName) {
        try {
            Application application = discoveryClient.getApplication(serviceName);
            if (application == null) { // retry until it is available
                LOG.error("DiscoveryClientManager was not able to retrieve base-url. Retry, since application was not found.");
                try { Thread.sleep(RETRY_TIME); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return retrieveBaseUrl(serviceName);
            }

            List<InstanceInfo> instances = application.getInstances();
            if (CollectionUtils.isEmpty(instances)) { // retry until it is available
                LOG.error("DiscoveryClientManager was not able to retrieve base-url. Retry, since no instances were found.");
                try { Thread.sleep(RETRY_TIME); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return retrieveBaseUrl(serviceName);
            }

            InstanceInfo instanceInfo = instances.get(ThreadLocalRandom.current().nextInt(0, instances.size()));
            if (instanceInfo == null || instanceInfo.getHostName() == null) { // retry until it is available
                LOG.error("DiscoveryClientManager was not able to retrieve base-url. Retry, since instanceInfo/Hostname are null.");
                try { Thread.sleep(RETRY_TIME); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                return retrieveBaseUrl(serviceName);
            }
            return PROTOCOL + instanceInfo.getHostName() + ":" + instanceInfo.getPort();
        } catch (NullPointerException e) {
            LOG.error("DiscoveryClientManager was not able to retrieve base-url. Retry because of Error: {}", e.getMessage());
            return retrieveBaseUrl(serviceName);
        }
    }

    protected void checkResponseOnErrors(ResponseEntity response) {
        if (ObjectUtils.isEmpty(response)) {
            throw new RestClientException("State service error occurred. No response received");
        } else if (response.getStatusCode().is5xxServerError()) {
            throw new RestClientException("State service error occurred. Http error code received: " + response.getStatusCode());
        }
    }
}
