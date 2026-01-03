package edu.hm.ccwi.matilda.analyzer.service.dbmigrationlibraries;

import edu.hm.ccwi.matilda.analyzer.exception.AnalyzerException;
import edu.hm.ccwi.matilda.analyzer.utils.CsvUtils;
import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.util.LibraryUtilsAdapter;
import edu.hm.ccwi.matilda.persistence.jpa.model.*;
import edu.hm.ccwi.matilda.persistence.jpa.repo.*;
import edu.hm.ccwi.matilda.persistence.mongo.repo.GACategoryTagAddManualTagsRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.GACategoryTagRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.GACategoryTagTotalRepository;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import edu.hm.ccwi.matilda.persistence.jpa.service.LibCategoryService;
import edu.hm.ccwi.matilda.persistence.jpa.service.LibraryService;

import java.util.*;

@Service
public class MigrationServiceImpl implements MigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(MigrationService.class);

    final LibraryRepository libraryRepository;
    final CharacteristicRepository characteristicRepository;
    final ExtractedDesignDecisionRepository extractedDesignDecisionRepository;
    final LibCategoryRepository libCategoryRepository;
    final TypeRepository typeRepository;
    final LibCategoryService libCategoryService;
    final LibraryService libraryService;

    final GACategoryTagTotalRepository gaCategoryTagTotalRepository;
    final GACategoryTagRepository gaCategoryTagRepository;
    final GACategoryTagAddManualTagsRepository gaCategoryTagAddManualTagsRepository;

    public MigrationServiceImpl(LibraryRepository libraryRepository, CharacteristicRepository characteristicRepository,
                                ExtractedDesignDecisionRepository eddRepository, LibCategoryRepository libCategoryRepository,
                                TypeRepository typeRepository, LibCategoryService libCategoryService, LibraryService libraryService,
                                GACategoryTagTotalRepository gaCategoryTagTotalRepository, GACategoryTagRepository gaCategoryTagRepository,
                                GACategoryTagAddManualTagsRepository gaCategoryTagAddManualTagsRepository) {
        this.libraryRepository = libraryRepository;
        this.characteristicRepository = characteristicRepository;
        this.extractedDesignDecisionRepository = eddRepository;
        this.libCategoryService = libCategoryService;
        this.libCategoryRepository = libCategoryRepository;
        this.typeRepository = typeRepository;
        this.libraryService = libraryService;
        this.gaCategoryTagTotalRepository = gaCategoryTagTotalRepository;
        this.gaCategoryTagRepository = gaCategoryTagRepository;
        this.gaCategoryTagAddManualTagsRepository = gaCategoryTagAddManualTagsRepository;
    }

    private Map<String, LibraryEntity> loadMapOfAllLibrariesInRelationalDatabase() {
        LOG.info("Start loading all MATILDA_LIBRARY entries from relational table to enrich data");
        List<LibraryEntity> allRelationalSavedLibs = libraryRepository.findAll();
        Collections.shuffle(allRelationalSavedLibs);
        Map<String, LibraryEntity> relationalLibEntityMap = new HashMap<>();
        for (LibraryEntity relationalSavedLib : allRelationalSavedLibs) {
            relationalLibEntityMap.put(relationalSavedLib.getGroupArtifactId(), relationalSavedLib);
        }
        return relationalLibEntityMap;
    }

    @Override
    public void enrichMatildaLibraryDBByExistingGACategoryTagFromTrainingsdataset(boolean writeMode, String[] datasetV5) {
        // Prio d) merge new info of current trainingsset of libsim-ki
        Map<String, LibraryEntity> relationalLibEntityMap = loadMapOfAllLibrariesInRelationalDatabase();

        int updateCounter = 0;
        int progress = 0;
        LOG.info("Step 4 - START recategorizing libraries by the new hybrid-approach");

        for (String entry : datasetV5) {
            progress += 1;

            String[] entryArray = entry.split(";");
            String id = entryArray[0];
            String group = entryArray[1];
            String artifact = entryArray[2];
            String category = entryArray[3];
            String tags = entryArray[4];

            if(StringUtils.isNotEmpty(id) && StringUtils.isNotEmpty(group) && StringUtils.isNotEmpty(artifact)) {

                List<String> tagList = new ArrayList<>();
                if (StringUtils.isNotEmpty(tags)) {
                    tagList = new ArrayList<>(Arrays.asList(tags.split("\\|")));
                }

                if (analyseAndMergeMongoLibraryIntoPostgresLibraries(writeMode, relationalLibEntityMap, id, group, artifact, category, tagList)) {
                    updateCounter += 1;
                }
            }
            if (progress % 1000 == 0) {
                LOG.info("Step 3 - Progress: {}/{} - changes: {}", progress, datasetV5.length, updateCounter);
            }
        }
    }

    @Override
    public void enrichMatildaLibraryDBByExistingGACategoryTagFromManuallyLabeledDS(boolean writeMode, String[] manualDataset) {
        Map<String, LibraryEntity> relationalLibEntityMap = loadMapOfAllLibrariesInRelationalDatabase();

        int updateCounter = 0;
        int progress = 0;
        LOG.info("Step 5 - START recategorizing libraries by the manually labeled dataset");

        for (String entry : manualDataset) {
            progress += 1;

            String[] entryArray = entry.split(";");
            String groupArtifact = entryArray[0];
            String category = entryArray[1];

            if(StringUtils.isNotEmpty(groupArtifact) && StringUtils.isNotEmpty(category)) {
                if (updateLibraryInPostgresIfNecessary(writeMode, relationalLibEntityMap, groupArtifact, category, null)) {
                    updateCounter += 1;
                }
            }
            if (progress % 1000 == 0) {
                //LOG.info("Step 3 - Progress: {}/{} - changes: {}", progress, datasetV5.length, updateCounter);
            }
        }
    }


    private boolean analyseAndMergeMongoLibraryIntoPostgresLibraries(boolean writeMode,
                                                                  Map<String, LibraryEntity> allRelationalSavedLibs,
                                                                  String groupArtifact, String group, String artifact,
                                                                  String category, List<String> tags) {
        boolean updatedOrInserted = false;
        if(StringUtils.isNotEmpty(category)) {
            category = replaceWrongCategoryByCorrectLabelToAvoidInconsistentExeptions(category);
        }

        try {
            if (allRelationalSavedLibs.containsKey(groupArtifact)) {
                updatedOrInserted = updateLibraryInPostgresIfNecessary(writeMode, allRelationalSavedLibs, groupArtifact, category, tags);
            } else {
                // INSERT NEW LIB
                Long libCategoryId = null;
                if(StringUtils.isNotEmpty(category)) {
                    LibCategoryEntity libCategoryEntity = libCategoryRepository.findByLabel(category).orElseThrow(AnalyzerException::new);
                    libCategoryId = libCategoryEntity.getId();
                }
                updatedOrInserted = true;
                if(writeMode) {
                    libraryRepository.save(new LibraryEntity(groupArtifact, group, artifact, libCategoryId, String.join("|", tags)));
                }
            }
        } catch (AnalyzerException e) {
            LOG.error("Did not find category {} of library {} in relational DB. TODO: Update inconsistency!", category, groupArtifact);
        }

        return updatedOrInserted;
    }

    private boolean updateLibraryInPostgresIfNecessary(boolean writeMode, Map<String, LibraryEntity> allRelationalSavedLibs,
                                                       String groupArtifact, String category, List<String> tags) {
        boolean updatedOrInserted = false;
        LibraryEntity libraryEntity = allRelationalSavedLibs.get(groupArtifact);
        // UPDATE IF NECESSARY
        if(StringUtils.isNotEmpty(category)) {
            LibCategoryEntity libCategoryEntity = libCategoryRepository.findByLabel(category).orElse(null);
            if (libraryEntity.getCategory() == null && libCategoryEntity != null && libCategoryEntity.getId() != null) {
                //UPDATE
                libraryEntity.setCategory(libCategoryEntity.getId());
                if(writeMode) {
                    libraryRepository.save(libraryEntity);
                }
                updatedOrInserted = true;
            } else if(libraryEntity.getCategory() != null && libCategoryEntity != null && libCategoryEntity.getId() != null &&
            libraryEntity.getCategory().intValue() != libCategoryEntity.getId().intValue()) {
                String newCategory = libCategoryRepository.findById(libraryEntity.getCategory()).get().getLabel();
                LOG.warn("Unequal cat for:; {}; {}; vs; {}", groupArtifact, libCategoryEntity.getLabel(), newCategory);
            }
        }
        if(CollectionUtils.isNotEmpty(tags)) {
            // 1) add all tags if relationalTagList is empty
            if(StringUtils.isEmpty(libraryEntity.getTags())) {
                libraryEntity.setTags(String.join("|", tags));
                if(writeMode) {
                    libraryRepository.save(libraryEntity);
                }
                updatedOrInserted = true;
            } else {
                String originTags = libraryEntity.getTags();
                Set<String> relationalTagSet = retrieveTagListFromConcatString(libraryEntity);
                if (CollectionUtils.isNotEmpty(relationalTagSet)) {
                    // 2) extend if there are tags not known yet in relational db
                    int initialSize = relationalTagSet.size();
                    for (String tag : tags) {
                        if(tag != null && tag.length() > 1) {
                            relationalTagSet.add(tag);
                        }
                    }
                    if(initialSize < relationalTagSet.size()) {

                        libraryEntity.setTags(String.join("|", relationalTagSet));
                        if (writeMode) {
                            libraryRepository.save(libraryEntity);
                        }
                        updatedOrInserted = true;
                        LOG.info("Extended Tags from: {} -to-> {} for lib: {}", originTags, libraryEntity.getTags(), libraryEntity.getGroupArtifactId());
                    }
                }
            }
        }
        return updatedOrInserted;
    }

    Set<String> retrieveTagListFromConcatString(LibraryEntity libraryEntity) {
        Set<String> relationalTagSet = new HashSet<>(Arrays.asList(libraryEntity.getTags().split("\\|")));
        return relationalTagSet;
    }

    private String replaceWrongCategoryByCorrectLabelToAvoidInconsistentExeptions(String category) {
        // PREVENT INCONSISTENCY-ERRORS:
        if(StringUtils.equals(category, "Security / Cryptography / Authentification")) {
            category = "Security / Cryptography / Authentication";
        } else if(StringUtils.equals(category, "Cluster Managment")) {
            category = "Cluster Management";
        } else if(StringUtils.equals(category, "Managing / Monitoring")) {
            category = "Logging / Tracing / Monitoring / Administration";
        } else if(StringUtils.equals(category, "Example")) {
            category = "Example / Documentation";
        } else if(StringUtils.equals(category, "Maven Plugins")) {
            category = "Build / Deployment";
        } else if(StringUtils.equals(category, "s3clients")) {
            category = "File System";
        }

        try {
            category = LibraryUtilsAdapter.resolveLibCategoryByString(category).getMatildaCategory();
        } catch (MatildaMappingException e) {
            LOG.error("An error occured while resolving category: {}", e.getMessage());
        }

        return category;
    }

    @Override
    public void importTechnologiesByImplementedList() throws Exception {
        LOG.info("Start MigrationService");

        // 1) Speichere alle LibCategories
        LOG.info("Start saving all LibCategories");
        for (LibCategory value : LibCategory.values()) {
            libCategoryService.importLibraryCategory(value.getMatildaCategory());
        }
        libCategoryRepository.flush();

        // 2) Speichere CharacteristicType
        LOG.info("Start saving characteristicType");
        String typename = "Technology";
        TypeEntity technologyTypeEntity;
        if(typeRepository.existsByTypename(typename)) {
            technologyTypeEntity = typeRepository.findByTypename(typename).get();
        } else {
            technologyTypeEntity = typeRepository.save(new TypeEntity(typename));
        }

        //3) Speichere ALLE (aus DD-Tabelle init/target) Libs in die DB mit Verlinkung auf Kategorie
        LOG.info("Start saving/updating Libraries");
        List<ExtractedDesignDecisionEntity> eddList = extractedDesignDecisionRepository.findAll();
        for (ExtractedDesignDecisionEntity eddEntity : eddList) {

            String initial = eddEntity.getInitial();
            String target = eddEntity.getTarget();
            try {
                if (StringUtils.isNotEmpty(initial) && !StringUtils.equals(initial, "NULL")) { //NOT "NULL"
                    libraryService.saveOrUpdateLibrary(initial, eddEntity.getDecisionSubject(), null);
                }
                if (StringUtils.isNotEmpty(target) && !StringUtils.equals(target, "NULL")) { //NOT "NULL"
                    libraryService.saveOrUpdateLibrary(target, eddEntity.getDecisionSubject(), null);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("Found Library-GA which has no common ID-Structure: {}/{}, skipped...", initial, target);
                continue;
            } catch (Exception e) {
                // UNKNOWN INVALID LIB-CATEGORY-LABEL!!!!
                LOG.warn("MAYBE INVALID LABEL FOUND IN eDD-Table: '{}'", eddEntity.getDecisionSubject());
            }
        }
        libraryRepository.flush();

        // 4) Speichere alle Characteristics in die DB mit Verlinkung zur passenden LibCategory and Type Technology
        LOG.info("Start saving all Database Characteristics linked to LibCategory, Type and Libraries");
        List<List<String>> libTechTuples = CsvUtils.readCsv("lib-database-characteristics-corpus-labeled-2021-11-15.csv", ";");
        for (List<String> libTechTuple : libTechTuples) {
            //LOG.info("Found LibTechTuple: {} -- {}", libTechTuple.get(0), libTechTuple.get(1));
            saveOrUpdateCharacteristic(technologyTypeEntity, libCategoryRepository.findByLabel("Database").get(),
                    libTechTuple.get(0), libTechTuple.get(1));
        }
        characteristicRepository.flush();

    }

    private void saveOrUpdateCharacteristic(TypeEntity typeEntity, LibCategoryEntity libCategory, String libraryGaId, String characteristicLabel) {
        LibraryEntity libraryEntity = libraryRepository.findByGroupArtifactId(libraryGaId)
                .orElse(libraryService.saveOrUpdateLibrary(libraryGaId, libCategory, null));
        Set<LibraryEntity> libraries = new HashSet<>();
        libraries.add(libraryEntity);
        Set<LibCategoryEntity> libCategories = new HashSet<>();
        libCategories.add(libCategory);
        CharacteristicEntity characteristicEntity = characteristicRepository.findByName(characteristicLabel)
                .orElse(new CharacteristicEntity(characteristicLabel, typeEntity.getId(), libraries, libCategories));

        characteristicEntity.getLibraries().add(libraryEntity);
        characteristicEntity.getLibCategories().add(libCategory);

        characteristicRepository.save(characteristicEntity);
    }
}
