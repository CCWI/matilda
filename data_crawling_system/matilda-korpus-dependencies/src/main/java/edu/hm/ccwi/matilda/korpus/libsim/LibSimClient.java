package edu.hm.ccwi.matilda.korpus.libsim;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class LibSimClient {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10)).build();

    private String libSimUri = "http://localhost:8500/predict";

    private ObjectMapper objectMapper;

    public LibSimClient() {
        objectMapper = new ObjectMapper();
    }

    /**
     *
     * @param baseUri - default is: "http://localhost:8500/predict"
     */
    public LibSimClient(String baseUri) {
        objectMapper = new ObjectMapper();
        libSimUri = baseUri + "/predict";
    }

    public LibSimResult requestKi(GACategoryTag gaCategoryTag) throws IOException, LibSimException, InterruptedException {
        return requestKi(gaCategoryTag.getGroup(), gaCategoryTag.getArtifact(), gaCategoryTag.getTags());
    }

    public LibSimResult requestKi(String group, String artifact, List<String> tags) throws LibSimException, IOException, InterruptedException {
        String uriTags = null;
        for (String tag : tags) {
            uriTags = StringUtils.isEmpty(uriTags) ? "" + tag : uriTags + "%7C" + tag;
        }

        if(StringUtils.isEmpty(uriTags) || uriTags == "null") {
            uriTags = "";
        }

        String params = "ga=" + group + ":" + artifact + "&token=" + uriTags;

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(libSimUri + "?" + params))
                .setHeader("User-Agent", "Matilda")
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), LibSimResult.class);
        } else {
            throw new LibSimException("Request not successful: " + response.statusCode());
        }
    }
}
