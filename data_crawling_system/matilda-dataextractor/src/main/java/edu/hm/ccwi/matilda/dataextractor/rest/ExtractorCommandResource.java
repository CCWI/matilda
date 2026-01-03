package edu.hm.ccwi.matilda.dataextractor.rest;

import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.dataextractor.service.DataExtractorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/extract")
public class ExtractorCommandResource {

    final DataExtractorService extractorService;

    public ExtractorCommandResource(DataExtractorService extractorService) {
        this.extractorService = extractorService;
    }

    /**
     * Process a complete extraction of data.
     */
    @GetMapping(value = "/repository")
    public String extractRepo(@RequestParam(value = "reponame") String repoName,
                              @RequestParam(value = "projectname") String projectName,
                              @RequestParam(value = "directory") String projectDir,
                              @RequestParam(value = "source") RepoSource source) throws Exception {
        return extractorService.extractorProcessor(repoName, projectName, projectDir, source);
    }
}