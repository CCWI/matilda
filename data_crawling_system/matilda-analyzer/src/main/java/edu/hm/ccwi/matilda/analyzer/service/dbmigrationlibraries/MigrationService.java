package edu.hm.ccwi.matilda.analyzer.service.dbmigrationlibraries;

public interface MigrationService {

    void importTechnologiesByImplementedList() throws Exception;

    void enrichMatildaLibraryDBByExistingGACategoryTagFromTrainingsdataset(boolean writeMode, String[] datasetV5);

    void enrichMatildaLibraryDBByExistingGACategoryTagFromManuallyLabeledDS(boolean writeMode, String[] manualDataset);
}
