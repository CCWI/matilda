package edu.hm.ccwi.matilda.libmngm.rest;

import edu.hm.ccwi.matilda.base.model.library.Library;
import edu.hm.ccwi.matilda.libmngm.service.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LibraryResource {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryResource.class);

    final LibraryService libraryService;

    public LibraryResource(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping(value = "/libraries")
    public ResponseEntity<List<Library>> getLibrariesOfCategoryId(@RequestParam(value = "categoryId") String categoryId) {
        LOG.info("Incoming request to load libraries for categoryId: {}", categoryId);
        try {
            return ResponseEntity.ok(libraryService.getLibrariesOfCategory(categoryId));
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping(value = "/libraries")
    public ResponseEntity<String> saveOrUpdateLibrary(@RequestBody Library library) {
        try {
            LOG.trace("Request to create new or update library: {}", library.getGroupArtifactId());
            String gaId = libraryService.saveOrUpdateLibrary(library);
            return ResponseEntity.ok(gaId);
        } catch(Exception e) {
            LOG.error("An error occurred: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
