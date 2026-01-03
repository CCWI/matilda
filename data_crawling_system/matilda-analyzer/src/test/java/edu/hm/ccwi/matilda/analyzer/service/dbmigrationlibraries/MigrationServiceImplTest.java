package edu.hm.ccwi.matilda.analyzer.service.dbmigrationlibraries;

import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MigrationServiceImplTest {

    @Test
    void retrieveTagListFromConcatString() {
        MigrationServiceImpl migrationService = new MigrationServiceImpl(null, null,
                null, null, null, null, null,
                null, null, null);

        LibraryEntity libraryEntity = new LibraryEntity();
        libraryEntity.setTags("tag1");
        Set<String> tags = migrationService.retrieveTagListFromConcatString(libraryEntity);

        assertThat(tags).hasSize(1);
        assertThat(tags).contains("tag1");

        libraryEntity.setTags("tag1|tag2|tag3|tag4");
        tags = migrationService.retrieveTagListFromConcatString(libraryEntity);

        assertThat(tags).hasSize(4);
        assertThat(tags).contains("tag1");
        assertThat(tags).contains("tag2");
        assertThat(tags).contains("tag3");
        assertThat(tags).contains("tag4");


    }
}