package edu.hm.ccwi.matilda.analyzer.rest;

import edu.hm.ccwi.matilda.analyzer.service.dbmigrationlibraries.MigrationService;
import edu.hm.ccwi.matilda.base.model.library.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import edu.hm.ccwi.matilda.persistence.jpa.service.LibCategoryService;
import edu.hm.ccwi.matilda.persistence.jpa.service.LibraryService;

import java.util.List;

@RestController
public class LibraryResource {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryResource.class);

    final LibraryService libraryService;
    final MigrationService migrationService;
    final LibCategoryService categoryService;

    public LibraryResource(LibraryService libraryService, MigrationService migrationService, LibCategoryService categoryService) {
        this.libraryService = libraryService;
        this.migrationService = migrationService;
        this.categoryService = categoryService;
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

    @GetMapping(value = "/technology/import")
    public ResponseEntity crawlTechnologies(@RequestParam(value = "categoryId") String categoryId) {
        LOG.info("Incoming request to load libraries for categoryId: {}", categoryId);
        try {
            migrationService.importTechnologiesByImplementedList();
            return ResponseEntity.ok().build();
        } catch(Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping(value = "/category/import")
    public ResponseEntity importCategory(@RequestParam(value = "matildaCategory") String matildaCategory) {
        LOG.info("Incoming request to import matildaCategory: {}", matildaCategory);
        try {
            categoryService.importLibraryCategory(matildaCategory);
            return ResponseEntity.ok().build();
        } catch(Exception e) {
            LOG.error("Failed to import category {} -> {}", matildaCategory, e.getMessage());
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
