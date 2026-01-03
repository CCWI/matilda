package edu.hm.ccwi.matilda.persistence.jpa.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.model.library.Library;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.util.MappingLibraryEntityService;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibraryRepository;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LibraryServiceImpl implements LibraryService {

    private static final Logger LOG = LoggerFactory.getLogger(LibraryService.class);

    final LibraryRepository libraryRepository;
    final LibCategoryService libCategoryService;
    final MappingLibraryEntityService mappingService;

    public LibraryServiceImpl(LibraryRepository libraryRepository, LibCategoryService libCategoryService,
                              MappingLibraryEntityService mappingService) {
        this.libraryRepository = libraryRepository;
        this.libCategoryService = libCategoryService;
        this.mappingService = mappingService;
    }

    @Override
    public List<Library> getLibrariesOfCategory(String categoryId) {
        List<Library> libraries = new ArrayList<>();
        List<LibraryEntity> all = libraryRepository.findAll();
        LOG.info("Found {} ProjectProfileEntities for request.", Iterables.size(all));
        for (LibraryEntity projectProfileEntity : all) {
            libraries.add(mappingService.mapLibraryEntityToLibrary(projectProfileEntity));
        }
        return libraries;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public String saveOrUpdateLibrary(Library library) throws Exception {
        if(library != null && StringUtils.isNotEmpty(library.getGroupArtifactId()) &&
                StringUtils.isNotEmpty(library.getGroupId()) && StringUtils.isNotEmpty(library.getArtifactId())) {
            LibraryEntity ppEntity = libraryRepository.findByGroupArtifactId(library.getGroupArtifactId())
                    .orElse(mappingService.mapLibraryToLibraryEntity(library));
            if(ppEntity.getCategory() != library.getCategory()) {
                ppEntity.setCategory(library.getCategory());
            }
            return libraryRepository.save(ppEntity).getGroupArtifactId();
        }

        throw new Exception("Provided Library does not contain mandatory fields.");
    }

    @Override
    public LibraryEntity saveOrUpdateLibrary(String gaId, String libCategoryLabel, String tags) throws IllegalArgumentException {
        LibCategoryEntity libCategory = null;
        try {
            libCategory = libCategoryService.getLibCategoryFromLabel(libCategoryLabel);
        } catch (Exception e) {
            // UNKNOWN INVALID LIB-CATEGORY-LABEL!!!!
            LOG.error("INVALID LABEL FOUND: '{}' - ignore and continue saveOrUpdate without Category-Link", libCategoryLabel);
        }

        return saveOrUpdateLibrary(gaId, libCategory, tags);
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public LibraryEntity saveOrUpdateLibrary(String gaId, LibCategoryEntity libCategory, String tags) throws IllegalArgumentException {
        String[] ga = gaId.split(":");
        if(ga.length < 2) {
            throw new IllegalArgumentException();
        } else {
            String groupId = ga[0];
            String artifactId = ga[1];
            Long libCategoryId = libCategory == null ? null : libCategory.getId();
            LibraryEntity entity = libraryRepository.findByGroupArtifactId(gaId)
                    .orElse(new LibraryEntity(gaId, groupId, artifactId, libCategoryId, tags));
            if(entity.getCategory() == null) {
                entity.setCategory(libCategoryId);
            }
            if(tags != null && !StringUtils.equals(entity.getTags(), tags)) {
                entity.setTags(tags);
            }
            return libraryRepository.save(entity);
        }
    }
}
