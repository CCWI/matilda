package edu.hm.ccwi.matilda.dataextractor.kafka;

import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractorCommandKafkaClientTest {

    @Test
    void deserializeDtoJson() {
        String json = "{\n" +
                "  \"matildaId\": \"580dc91a-cbdc-4865-a2cb-7af72a27b9cb\",\n" +
                "  \"repoSource\": \"github\",\n" +
                "  \"repoProjName\": \"testrepo:testproj\",\n" +
                "  \"repoName\": \"testrepo\",\n" +
                "  \"projectName\": \"testproj\",\n" +
                "  \"projectDir\": \"testdir\",\n" +
                "  \"targetPath\": \"targetTestPath\",\n" +
                "  \"janitor\": true,\n" +
                "  \"since\": \"2010-01-01\",\n" +
                "  \"repoUpdateable\": true\n" +
                "}";

        ProcessingProjectDto processingProjectDto = new ExtractorCommandKafkaClient(null).deserializeDtoJson(json);

        System.out.println(processingProjectDto.toString());
        assertThat(processingProjectDto.getSince().toString()).isEqualTo("2010-01-01");
    }

    @Test
    void deserializeDtoJson_otherSinceRepresentation() {
        String json = "{\"matildaId\":\"6091e015-8e64-4a72-b104-ce99a89c3e30\"," +
                "\"repoSource\":\"github\"," +
                "\"repoProjName\":\"navikt/pam-eures-stilling-import\"," +
                "\"repoName\":\"navikt\"," +
                "\"projectName\":\"pam-eures-stilling-import\"," +
                "\"projectDir\":\"/home/max/matilda-test3-repos/crawled/navikt/pam-eures-stilling-import\"," +
                "\"targetPath\":\"/home/max/matilda-test3-repos/crawled\"," +
                "\"uri\":\"https://github.com/navikt/pam-eures-stilling-import\"," +
                "\"janitor\":true," +
                "\"since\":{\"year\":2010,\"month\":1,\"day\":1}," +
                "\"repoUpdateable\":false\n" +
                "}";

        ProcessingProjectDto processingProjectDto = new ExtractorCommandKafkaClient(null).deserializeDtoJson(json);

        System.out.println(processingProjectDto.toString());
        assertThat(processingProjectDto.getSince().toString()).isEqualTo("2010-01-01");
    }
}