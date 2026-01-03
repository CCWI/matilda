package edu.hm.ccwi.matilda.dataextractor.rest;

import edu.hm.ccwi.matilda.dataextractor.service.cleaner.ArchiverService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.io.File;

@RestController
@RequestMapping("/archive")
public class ArchiveResource {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiveResource.class);

    final ArchiverService archiverService;

    public ArchiveResource(ArchiverService archiverService) {
        this.archiverService = archiverService;
    }

    @GetMapping(value = "/all")
    @ResponseStatus(HttpStatus.OK)
    public void cleanAndArchiveCrawledFolderStructure(@NotNull @RequestParam(value = "directory") String rootDir) {
        LOG.info("---- Start cleaning and archiving all crawled folders ----");
        LOG.info("---- {} ----", new File(rootDir).getAbsolutePath());
        if (StringUtils.isNotEmpty(rootDir)) {
            archiverService.archiveCrawledDirectoriesRecursively(rootDir);
        }
        LOG.info("---- Finished cleaning and archiving all crawled folders ----");
    }

    @GetMapping(value = "/reactivate/all")
    @ResponseStatus(HttpStatus.OK)
    public void reactivateArchivedFolderStructure(@NotNull @RequestParam(value = "directory") String rootDir) {
        LOG.info("---- Start reactivating the archived folder structure ----");
        if (StringUtils.isNotEmpty(rootDir)) {
            archiverService.reactivateArchivedFolderStructure(rootDir);
        }
        LOG.info("---- Finished reactivating the archived folder structure ----");
    }

}