package edu.hm.ccwi.matilda.dataextractor.rest;

import edu.hm.ccwi.matilda.dataextractor.service.cleaner.InconsistencyMigrationBatchCleaner;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Resource zum Verwalten der persistierten Dokumente in der MongoDB.
 * Aktuell wird diese Resource zum Aufräumen von Inkonsistenzen und migrieren von Daten(typen) verwendet!
 */
public class DatabaseResource {

    private InconsistencyMigrationBatchCleaner inconsistencyMigrationBatchCleaner;

    public DatabaseResource(InconsistencyMigrationBatchCleaner inconsistencyMigrationBatchCleaner) {
        this.inconsistencyMigrationBatchCleaner = inconsistencyMigrationBatchCleaner;
    }

    @GetMapping(value = "/cleanup/dependencies")
    public Boolean startCleanupDependenciesByImplementedRuleSet() {
        return inconsistencyMigrationBatchCleaner.startBatchProcessingByImplementedRuleset();
    }


}
