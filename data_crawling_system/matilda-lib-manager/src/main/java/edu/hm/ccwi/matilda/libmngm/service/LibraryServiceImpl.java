package edu.hm.ccwi.matilda.libmngm.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.model.library.Library;
import edu.hm.ccwi.matilda.libmngm.entity.LibraryEntity;
import edu.hm.ccwi.matilda.libmngm.repository.LibraryRepository;
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
    final MappingService mappingService;

    public LibraryServiceImpl(LibraryRepository libraryRepository, MappingService mappingService) {
        this.libraryRepository = libraryRepository;
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
            if(!StringUtils.equals(ppEntity.getTags(), library.getTags())) {
                ppEntity.setTags(library.getTags());
            }
            return libraryRepository.save(ppEntity).getGroupArtifactId();
        }

        throw new Exception("Provided Library does not contain mandatory fields.");
    }
}
