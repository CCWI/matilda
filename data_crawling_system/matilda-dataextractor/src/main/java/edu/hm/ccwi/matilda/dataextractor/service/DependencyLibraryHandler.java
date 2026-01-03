package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.util.LibraryUtilsAdapter;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimClient;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimException;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimResult;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.UnknownCategoryTag;
import edu.hm.ccwi.matilda.persistence.mongo.repo.UnknownCategoryTagRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibCategoryRepository;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibraryRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handle and process dependency and library objects.
 *
 * @author Max.Auch
 */
@Service
public class DependencyLibraryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyLibraryHandler.class);

    private final UtilService utilService;
    private final UnknownCategoryTagRepository unknownCategoryTagRepository;
    private final MvnRepoCrawler mvnRepoCrawler;
    private final LibraryRepository libraryRepository;
    private final LibCategoryRepository libCategoryRepository;
    private LibSimClient libSimClient;

    /**
     * local representation of table MATILDA_LIBRARY -> cache.
     * increases performance when iterating over thousands of commits and their dependencies in each project.
     * when db is updated, also this cache is updated. Also if not found in Map, the db is queried.
     * disadvantage: DB-Changes of an Extractor-Instance is not synchronized to the other Instances. Quick-Solution:
     * Restart the Instances here and there.
     */
    private HashMap<String, LibraryEntity> cachedlibraryEntityHashMap;

    @Autowired
    public DependencyLibraryHandler(UnknownCategoryTagRepository unknownCategoryTagRepository, UtilService utilService,
                                    MvnRepoCrawler mvnRepoCrawler, LibraryRepository libraryRepository, LibCategoryRepository libCategoryRepository) {
        this.unknownCategoryTagRepository = unknownCategoryTagRepository;
        this.utilService = utilService;
        this.mvnRepoCrawler = mvnRepoCrawler;
        this.libraryRepository = libraryRepository;
        this.libCategoryRepository = libCategoryRepository;
        this.libSimClient = new LibSimClient("http://matilda-libsim-ki:5000"); 
        this.cachedlibraryEntityHashMap = new HashMap<>();
    }

    @PostConstruct
    public void initializer() {
        this.cachedlibraryEntityHashMap = libraryRepository.findAll().stream()
                .collect(Collectors.toMap(LibraryEntity::getGroupArtifactId, libraryEntity -> libraryEntity, (a, b) -> b, HashMap::new));
    }

    public void setLibSimClient(LibSimClient libSimClient) {
        this.libSimClient = libSimClient;
    }

    /**
     * Check local-MongoDB for categories/tags or crawl MvnRepository.com instead.
     */
    public CrawledDependency assignCategoriesAndTags(CrawledDependency dep) {
        LibraryEntity libCatTag = this.cachedlibraryEntityHashMap.get(dep.getGroup() + ":" + dep.getArtifact());
        if(libCatTag == null) { // check db and update cache if found in db
            libCatTag = libraryRepository.findByGroupArtifactId(dep.getGroup() + ":" + dep.getArtifact()).orElse(null);
            if(libCatTag != null) {
                cachedlibraryEntityHashMap.put(dep.getGroup() + ":" + dep.getArtifact(), libCatTag);
            }
        }

        // new approach:
        // process if available in db or if not available (and save in this case to db)
        if(libCatTag == null) {
            LOG.info("No library in categoryTag-Database found for: {}:{}", dep.getGroup(), dep.getArtifact());
            // 1) get new label category and save + cache
            LibCategoryEntity libCategory = determineNewCategoryByLibsimKiAndPersistAndCache(dep, null);

            // 2) process
            dep = precessCrawledDependencyToEnrich(dep, null, libCategory);
        } else {
            // process libCatTag
            String[] tagArray = StringUtils.isNotEmpty(libCatTag.getTags()) ? libCatTag.getTags().split("\\|") : new String[0];
            LibCategoryEntity libCategory = null;
            if(libCatTag.getCategory() != null) {
                libCategory = libCategoryRepository.findById(libCatTag.getCategory()).orElse(null);
            } else if(StringUtils.isNotEmpty(libCatTag.getTags())){
                // try to determine empty category by getting prediction from LibSim-KI
                libCategory = determineNewCategoryByLibsimKiAndPersistAndCache(dep, tagArray);
            }
            dep = precessCrawledDependencyToEnrich(dep, tagArray, libCategory);
        }

        return dep;
    }

    /**
     * Retrieve Category from LibSim and Persist to DB. Also write to cache for performance optimization.
     *
     * @param dep
     * @return
     */
    private LibCategoryEntity determineNewCategoryByLibsimKiAndPersistAndCache(CrawledDependency dep, String[] tagArray) {
        // 1) find category
        LibCategoryEntity libCategory = reqLibSimForLibCategoryByTags(dep.getGroup(), dep.getArtifact(), tagArray);

        // 2) save lib with new category
        if(libCategory != null && libCategory.getLabel() != null) {
            LibCategoryEntity libCategoryEntity = libCategoryRepository.findByLabel(libCategory.getLabel()).orElse(null);
            LibraryEntity libraryEntity = new LibraryEntity(dep.getGroup() + ":" + dep.getArtifact(), dep.getGroup(),
                    dep.getArtifact(), libCategoryEntity != null ? libCategoryEntity.getId() : null,
                    dep.getTagList() != null ? String.join("|", dep.getTagList()) : null);
            libraryRepository.save(libraryEntity);
            cachedlibraryEntityHashMap.put(dep.getGroup() + ":" + dep.getArtifact(), libraryEntity);
        } else {
            LOG.warn("LibSim-Prediction is null. Either service not reached or an error occurred or was not able to predict. Check!");
        }
        return libCategory;
    }

    private CrawledDependency precessCrawledDependencyToEnrich(CrawledDependency dep, String[] tagArray,
                                                               LibCategoryEntity libCategory) {
        if(libCategory != null) {
            dep = enrichCrawledDependency(dep, LibCategory.getByMatildaCategory(libCategory.getLabel()), tagArray);
        } else {
            LOG.debug("Could ne enrich crawledDependency, since no libCategory found for libCatTag: {}:{}",
                    dep.getGroup(), dep.getArtifact());
        }
        return dep;
    }


    private boolean isAvailableCategoryOrTags(LibraryEntity libCatTag) {
        return libCatTag != null && (libCatTag.getCategory() != null || StringUtils.isNotEmpty(libCatTag.getTags()));
    }

    public CrawledDependency enrichCrawledDependency(CrawledDependency cDependency, LibCategory lbc, String[] tagList) {
        if(lbc != null) {
            cDependency.setCategory(lbc.getMatildaCategory());
            cDependency.setRelevant(lbc.isRelevant()); //dependency-relevance found and set (else always relevant)
        } else {
            cDependency.setRelevant(true); //always relevant if it is unknown
        }

        //assign tags and check relevance by tags if category is unknown/missing
        if (ArrayUtils.isNotEmpty(tagList)) {
            cDependency.setTagList(utilService.removeDuplicatesInList(Arrays.asList(tagList)));
        }

        return cDependency;
    }

    public LibCategory createLibCategoryFromMvnRepoPage(CrawledDependency dep, MvnRepoPage mvnRepoPage) {
        LibCategory libCategory = null;

        // 3) If Found: If category is available, try to map to MATILDA-CATEGORY
        if(StringUtils.isNotEmpty(mvnRepoPage.getCategory())) {
            libCategory = findLibCategoryByString(dep, mvnRepoPage.getCategory());
        }

        // 4) If failed or category not available -> check if tags are available and categorize by tags
        // If only tags are available, use libsim-ki to retrieve MATILDA-CATEGORY
        if(libCategory == null && ArrayUtils.isNotEmpty(mvnRepoPage.getTagMatches())) {
            LibCategoryEntity libcatEntity = reqLibSimForLibCategoryByTags(dep.getGroup(), dep.getArtifact(), mvnRepoPage.getTagMatches());
            libCategory = LibCategory.getByMatildaCategory(libcatEntity.getLabel());
        }
        return libCategory;
    }

    /**
     * Find a Matilda-Category by a category-string and handle unknown categories by saving to mongo-collection.
     */
    @Deprecated
    public LibCategory findLibCategoryByString(CrawledDependency cDependency, String category) {
        LibCategory libCategory = null;
        try {
            if (StringUtils.isNotEmpty(category)) {
                libCategory = LibraryUtilsAdapter.resolveLibCategoryByString(category);
            }
        } catch (IllegalArgumentException | MatildaMappingException e) {
            LOG.warn("Category of library is yet unknown and will be saved for review: " + e.getMessage());
            // DO NOT LOG TO PREVENT SPAM - even debug-level is to much spam
            // -> Original: LOG.error("Category for dependency " + cDependency.getGroup() + "/" + cDependency.getArtifact() + " is empty");
            if (!unknownCategoryTagRepository.existsById(category)) {
                // log category as unknown to add for later run
                unknownCategoryTagRepository.save(new UnknownCategoryTag(category, true,
                        cDependency.getGroup() + ":" + cDependency.getArtifact()));
            } else {
                LOG.error("UnknownCategoryTagRepository already includes category: " + category + ", but it is still unknown -> check!");
            }
        }

        return libCategory;
    }

    /**
     * Find a Matilda-Category by tags using libsim.
     */
    public LibCategoryEntity reqLibSimForLibCategoryByTags(String group, String artifact, String[] tags) {
        LibCategoryEntity libCategory = null;
        try {
            LibSimResult libSimResult = libSimClient.requestKi(group, artifact, tags != null ? Arrays.asList(tags) : new ArrayList<>());
            String prediction = libSimResult.getPrediction();
            if(StringUtils.isNotEmpty(prediction)) {
                LibCategory libCatEnum = LibraryUtilsAdapter.resolveLibCategoryByString(prediction);
                libCategory = libCategoryRepository.findByLabel(libCatEnum.getMatildaCategory()).orElse(null);
            }
        } catch (LibSimException | IOException | InterruptedException | MatildaMappingException e) {
            LOG.debug("Not able to find Lib-Category by Tags: {}", e.getMessage());
        }
        return libCategory;
    }

    public HashMap<String, LibraryEntity> getCachedlibraryEntityHashMap() {
        return cachedlibraryEntityHashMap;
    }
}
