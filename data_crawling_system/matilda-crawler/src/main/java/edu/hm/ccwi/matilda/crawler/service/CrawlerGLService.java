package edu.hm.ccwi.matilda.crawler.service;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.validation.constraints.NotEmpty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Max.Auch
 */
@Service
public class CrawlerGLService extends GenericCrawlerService {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerGLService.class);
    private static final String GL_SOURCE = "gitlab.source";

    private GitLabApi gitLabApi;

    @Value("${api.gitlab.uri}")
    private String gitlabUri;

    @Value("${api.gitlab.auth.username}")
    private String gitlabUsername;

    @Value("${api.gitlab.auth.password}")
    private String gitlabPassword;

    public CrawlerGLService(JanitorService janitorService) {
        super(janitorService);
    }

    @PostConstruct
    public void init() {
        try {
            this.gitLabApi = GitLabApi.oauth2Login(gitlabUri, gitlabUsername, gitlabPassword.toCharArray());
        } catch (GitLabApiException e) {
            LOG.error("Error while auth to gitlab somehow:", e);
        }
    }

    public String cloneRepoStructure(@NotEmpty String repoProjName, @NotEmpty String rootPath, boolean runJanitor, LocalDate dateSince) {
        LOG.debug("Clone repository structure {} since date: {}", repoProjName, dateSince);
        String namespace = repoProjName.substring(0, repoProjName.indexOf("/"));
        try {
            Project project = gitLabApi.getProjectApi().getProject(namespace, repoProjName.substring(repoProjName.indexOf("/") + 1));
            return crawlRemoteRepo(repoProjName, rootPath, runJanitor, dateSince, GL_SOURCE, project.getHttpUrlToRepo());
        } catch (GitLabApiException e) {
            LOG.error("Exception for crawling gitlab-repo {}", repoProjName);
        }
        return null;
    }

    public List<String> getRepoList(String queryText, String language, Integer page, Integer perPage, String sort,
                                    String order) throws NullPointerException, GitLabApiException {
        List<Project> pList = gitLabApi.getProjectApi().getProjects(queryText, page, perPage);
        List<String> repoList = new ArrayList<>();
        pList.forEach(project -> repoList.add(project.getNamespace().getName() + "/" + project.getName()));
        return repoList;
    }
}