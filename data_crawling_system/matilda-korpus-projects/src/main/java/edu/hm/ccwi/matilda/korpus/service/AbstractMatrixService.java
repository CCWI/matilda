package edu.hm.ccwi.matilda.korpus.service;

import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledSoftwareRepository;

import javax.inject.Inject;

public class AbstractMatrixService {

    protected static final String CSV_SEPARATOR = ",";

    @Inject
    protected CategorizationService categorizationService;

    @Inject
    protected CrawledSoftwareRepository mongoRepos;

    @Inject
    protected CrawledRevisionRepository mongoRevs;

    @Inject
    protected ExportService exportService;

}
