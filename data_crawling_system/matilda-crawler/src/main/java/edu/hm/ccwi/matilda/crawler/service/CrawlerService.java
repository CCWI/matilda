package edu.hm.ccwi.matilda.crawler.service;

import javax.validation.constraints.NotEmpty;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface CrawlerService {

    String cloneRepoStructure(@NotEmpty String repoProjName, @NotEmpty String rootPath, boolean runJanitor, LocalDate dateSince);

    List<String> getRepoList(String queryText, String language, Integer page, Integer perPage, String sort, String order) throws IOException;

    String updateExistingRepoStructure(@NotEmpty String repoProjName, @NotEmpty String rootPath,
                                       boolean runJanitor, LocalDate dateSince);
}
