package edu.hm.ccwi.matilda.crawler.rest;

import edu.hm.ccwi.matilda.crawler.service.CrawlerGHService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/crawl/gh")
@Deprecated
public class CrawlGHCommandResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlGHCommandResource.class);

    private final CrawlerGHService githubCrawler;

    public CrawlGHCommandResource(CrawlerGHService githubCrawler) {
        this.githubCrawler = githubCrawler;
    }

    @GetMapping(value = "/clone/repository")
    public String cloneGHRepository(@RequestParam(value = "name") String repoName,
                                    @RequestParam(value = "targetPath") String targetPath,
                                    @RequestParam(value = "janitor") boolean runJanitor,
                                    @RequestParam(value = "since") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSince) {
        LOG.debug("cloneGHRepository() called for: {}", repoName);
        return githubCrawler.cloneRepoStructure(repoName, targetPath, runJanitor, dateSince);
    }

    /**
     * crawl github for repository-names.
     *
     * @param queryText - e.g. "tetris"
     * @param language  - e.g. "java"
     * @param page      - e.g. 2
     * @param perPage   - e.g. 100
     * @param sort      - e.g. "stars"
     * @param order     - e.g. "desc"
     * @return
     */
    @GetMapping(value = "/reponames")
    public List<String> crawlGHRepoNames(@RequestParam(value = "searchText") String queryText,
                                       @RequestParam(value = "language") String language,
                                       @RequestParam(value = "page") Integer page,
                                       @RequestParam(value = "perPage") Integer perPage,
                                       @RequestParam(value = "sort") String sort,
                                       @RequestParam(value = "order") String order)
            throws IOException, NullPointerException {
        LOG.debug("crawlRepoNames() called for page: {}, with amount per page: {}", page, perPage);
        return githubCrawler.getRepoList(queryText, language, page, perPage, sort, order);
    }
}