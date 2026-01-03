package edu.hm.ccwi.matilda.runner.config;

import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Properties;

public class RunnerConfig {

    public static String targetedPath;

    // Security
    public static HttpAuthenticationFeature feature;
    public static String username;
    public static String password;

    // Crawler config
    public static String[] crawlerSearchText;
    public static int crawlerAmountOfRepos;
    public static String crawlerResultSort;
    public static String[] crawlerOrder; // asc - smallest first | desc - most relevant / biggest first
    public static int pageAmount;
    public static String crawlerProgrammingLanguage;
    public static LocalDate commitsSince;

    // Crawler config - urls
    public static String crawlerServiceUrl;
    public static String dataExtractorServiceUrl;
    public static String analyzerServiceUrl;
    public static String githubRepoUrl;
    public static String githubRepoNamesUrl;
    public static String gitlabRepoUrl;
    public static String gitlabRepoNamesUrl;
    public static String bitbucketRepoUrl;
    public static String bitbucketRepoNamesUrl;
    public static String analyzeProjectUrl;

    // Kafka kafka config
    public static Properties kafkaProducerProperties = new Properties();

    public RunnerConfig(String propertiesFile) {
        Properties prop = new Properties();
        try (InputStream input = ClassLoader.getSystemResourceAsStream(propertiesFile)) {
            prop.load(input);
            RunnerConfig.loadProperties(prop);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    public static void loadProperties(Properties prop) {
        RunnerConfig.targetedPath = prop.getProperty("matilda.target.path");
        RunnerConfig.username = prop.getProperty("matilda.auth.username");
        RunnerConfig.password = prop.getProperty("matilda.auth.password");
        RunnerConfig.feature = HttpAuthenticationFeature.basicBuilder().nonPreemptive()
                .credentials(RunnerConfig.username, RunnerConfig.password).build();

        RunnerConfig.crawlerSearchText = getStringList(prop.getProperty("matilda.crawler.searchText"));
        RunnerConfig.crawlerProgrammingLanguage = prop.getProperty("matilda.crawler.programming.language");
        RunnerConfig.crawlerAmountOfRepos = Integer.parseInt(prop.getProperty("matilda.crawler.amountOfRepos"));
        RunnerConfig.crawlerResultSort = prop.getProperty("matilda.crawler.sort");
        RunnerConfig.crawlerOrder = getStringList(prop.getProperty("matilda.crawler.order"));
        RunnerConfig.pageAmount = Integer.parseInt(prop.getProperty("matilda.crawler.pageAmount"));
        RunnerConfig.commitsSince = LocalDate.parse(prop.getProperty("matilda.crawler.commits.since"));

        // URIs & APIs
        RunnerConfig.crawlerServiceUrl = prop.getProperty("matilda.api.crawler.url");
        RunnerConfig.dataExtractorServiceUrl = prop.getProperty("matilda.api.dataextractor.url");
        RunnerConfig.analyzerServiceUrl = prop.getProperty("matilda.api.analyzer.url");
        RunnerConfig.githubRepoUrl = prop.getProperty("matilda.api.crawler.github.url.repository");
        RunnerConfig.githubRepoNamesUrl = prop.getProperty("matilda.api.crawler.github.url.reponames");
        RunnerConfig.gitlabRepoUrl = prop.getProperty("matilda.api.crawler.gitlab.url.repository");
        RunnerConfig.gitlabRepoNamesUrl = prop.getProperty("matilda.api.crawler.gitlab.url.reponames");
        RunnerConfig.bitbucketRepoUrl = prop.getProperty("matilda.api.crawler.bitbucket.url.repository");
        RunnerConfig.bitbucketRepoNamesUrl = prop.getProperty("matilda.api.crawler.bitbucket.url.reponames");
        RunnerConfig.analyzeProjectUrl = prop.getProperty("matilda.api.analyzer.url.analyze.project");
    }

    private static String[] getStringList(String listString) {
        return tokenizeStringList(listString);
    }

    private static String[] tokenizeStringList(String listString) {
        return listString.split("\\s*,\\s*");
    }

    public static String getRepoUrl(RepoSource crawlerSource) {
        if(crawlerSource.equals(RepoSource.github)) {
            return githubRepoUrl;
        }
        if(crawlerSource.equals(RepoSource.gitlab)) {
            return gitlabRepoUrl;
        }
        if(crawlerSource.equals(RepoSource.bitbucket)) {
            return bitbucketRepoUrl;
        }
        return null;
    }

    public static String getRepoNamesUrl(RepoSource crawlerSource) {
        if(crawlerSource.equals(RepoSource.github)) {
            return githubRepoNamesUrl;
        }
        if(crawlerSource.equals(RepoSource.gitlab)) {
            return gitlabRepoNamesUrl;
        }
        if(crawlerSource.equals(RepoSource.bitbucket)) {
            return bitbucketRepoNamesUrl;
        }
        return null;
    }
}
