package edu.hm.ccwi.matilda.crawler.rest;

import edu.hm.ccwi.matilda.crawler.service.CrawlerGLService;
import org.gitlab4j.api.GitLabApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/crawl/gl")
@Deprecated
public class CrawlGLCommandResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlGLCommandResource.class);

    private final CrawlerGLService gitlabCrawler;

    public CrawlGLCommandResource(CrawlerGLService gitlabCrawler) {
        this.gitlabCrawler = gitlabCrawler;
    }

    @GetMapping(value = "/clone/repository")
    public String cloneGLRepository(@RequestParam(value = "name") String repoName,
                                    @RequestParam(value = "targetPath") String targetPath,
                                    @RequestParam(value = "janitor") boolean runJanitor,
                                    @RequestParam(value = "since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSince) {
        return gitlabCrawler.cloneRepoStructure(repoName, targetPath, runJanitor, dateSince);
    }

    @GetMapping(value = "/reponames")
    public List<String> crawlGLRepoNames(@RequestParam(value = "searchText") String queryText,
                                         @RequestParam(value = "language") String language,
                                         @RequestParam(value = "page") Integer page,
                                         @RequestParam(value = "perPage") Integer perPage,
                                         @RequestParam(value = "sort") String sort,
                                         @RequestParam(value = "order") String order)
            throws NullPointerException, GitLabApiException {
        LOG.debug("crawlRepoNames() called for page: {}, with amount per page: {}", page, perPage);
        return gitlabCrawler.getRepoList(queryText, language, page, perPage, null, null);
    }
}