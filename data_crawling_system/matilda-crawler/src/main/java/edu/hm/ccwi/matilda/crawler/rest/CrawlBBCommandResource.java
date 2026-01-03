package edu.hm.ccwi.matilda.crawler.rest;

import edu.hm.ccwi.matilda.crawler.service.CrawlerBBService;
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
@RequestMapping("/crawl/bb")
@Deprecated
public class CrawlBBCommandResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlBBCommandResource.class);

    private CrawlerBBService bitbucketCrawler;

    public CrawlBBCommandResource(CrawlerBBService bitbucketCrawler) {
        this.bitbucketCrawler = bitbucketCrawler;
    }

    @GetMapping(value = "/clone/repository")
    public String cloneBBRepository(@RequestParam(value = "name") String repoName,
                                    @RequestParam(value = "targetPath") String targetPath,
                                    @RequestParam(value = "janitor") boolean runJanitor,
                                    @RequestParam(value = "since")  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateSince) {
        return bitbucketCrawler.cloneRepoStructure(repoName, targetPath, runJanitor, dateSince);
    }

    @GetMapping(value = "/reponames")
    public List<String> crawlBBRepoNames(@RequestParam(value = "searchText") String queryText,
                                       @RequestParam(value = "language") String language,
                                       @RequestParam(value = "page") Integer page,
                                       @RequestParam(value = "perPage") Integer perPage,
                                       @RequestParam(value = "sort") String sort,
                                       @RequestParam(value = "order") String order)
            throws IOException, NullPointerException {
        LOG.debug("crawlRepoNames() called for page: {}, with amount per page: {}", page, perPage);
        return bitbucketCrawler.getRepoList(queryText, language, page, perPage, sort, order);
    }
}