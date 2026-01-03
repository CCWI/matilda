package edu.hm.ccwi.matilda.crawler.kafka;

import com.google.gson.Gson;
import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.crawler.kafka.dto.CrawlerRequestDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrawlCommandKafkaClientTest {

    CrawlCommandKafkaClient crawlerKafkaClient;

    @BeforeEach
    void init() {
        crawlerKafkaClient = new CrawlCommandKafkaClient(null);
        ReflectionTestUtils.setField(crawlerKafkaClient, "commitsSince", "2010-01-01");
        ReflectionTestUtils.setField(crawlerKafkaClient, "useJanitor", Boolean.TRUE);
    }

    @Test
    void createProcessingProjectDto_ValidGithubUrl() throws Exception {
        String testUri = "https://github.com/CodeMax/SampleGitStructureProject";
        String repoProj = "CodeMax/SampleGitStructureProject";

        // Arrange
        String uuid = UUID.randomUUID().toString();

        CrawlerRequestDto crawlerRequestDto = new CrawlerRequestDto(uuid, testUri);

        // Act
        ProcessingProjectDto result = crawlerKafkaClient.createProcessingProjectDto(crawlerRequestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatildaId()).isEqualTo(uuid);
        assertThat(result.getRepoProjName()).isEqualTo(repoProj);
    }

    @Test
    void createProcessingProjectDto_ValidGitlabUrl() throws Exception {
        String testUri = "https://gitlab.com/codemax-research/matilda-system";
        String repoProj = "codemax-research/matilda-system";

        // Arrange
        String uuid = UUID.randomUUID().toString();

        CrawlerRequestDto crawlerRequestDto = new CrawlerRequestDto(uuid, testUri);

        // Act
        ProcessingProjectDto result = crawlerKafkaClient.createProcessingProjectDto(crawlerRequestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatildaId()).isEqualTo(uuid);
        assertThat(result.getRepoProjName()).isEqualTo(repoProj);
    }

    @Test
    void createProcessingProjectDto_ValidBitbucketUrl() throws Exception {
        String testUri = "https://<your_username>@bitbucket.org/codemax/test";
        String repoProj = "codemax/test";

        // Arrange
        String uuid = UUID.randomUUID().toString();

        CrawlerRequestDto crawlerRequestDto = new CrawlerRequestDto(uuid, testUri);

        // Act
        ProcessingProjectDto result = crawlerKafkaClient.createProcessingProjectDto(crawlerRequestDto);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getMatildaId()).isEqualTo(uuid);
        assertThat(result.getRepoProjName()).isEqualTo(repoProj);
    }

    @Test
    void createProcessingProjectDto_InvalidGithubUrl() {
        String testUri = "https://gitlab.com/codemax-research";

        // Arrange
        String uuid = UUID.randomUUID().toString();

        CrawlerRequestDto crawlerRequestDto = new CrawlerRequestDto(uuid, testUri);

        // Act / Assert
        Exception exception = assertThrows(Exception.class, () -> crawlerKafkaClient.createProcessingProjectDto(crawlerRequestDto));

        assertThat(exception.getMessage()).isEqualTo("received uri seems to be wrong. found token: 2");
    }

    @Test
    void serializeProcessingProjectDto() {
        LocalDate date = LocalDate.now();
        ProcessingProjectDto processingProjectDto = new ProcessingProjectDto(UUID.randomUUID().toString(), RepoSource.github,
                "testrepo:testproj", "testrepo", "testproj", "testdir",
                "targetTestPath", true, true, date);

        String json = new CrawlCommandKafkaClient(null).serializeToJsonString(processingProjectDto);

        System.out.println(json);
    }
}