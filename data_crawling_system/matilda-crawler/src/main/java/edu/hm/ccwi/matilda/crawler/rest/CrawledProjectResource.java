package edu.hm.ccwi.matilda.crawler.rest;

import edu.hm.ccwi.matilda.crawler.service.GenericCrawlerService;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;

@RestController
@RequestMapping("/recrawl")
@Deprecated
public class CrawledProjectResource {

    private static final Logger LOG = LoggerFactory.getLogger(CrawledProjectResource.class);

    private final GenericCrawlerService genericCrawlerService;

    public CrawledProjectResource(GenericCrawlerService genericCrawlerService) {
        this.genericCrawlerService = genericCrawlerService;
    }

}