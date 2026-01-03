package edu.hm.ccwi.matilda.persistence.jpa.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.base.model.library.LibraryCategory;
import edu.hm.ccwi.matilda.base.util.StringHandler;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibCategoryRepository;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class LibCategoryServiceImpl implements LibCategoryService {

    private static final Logger LOG = LoggerFactory.getLogger(LibCategoryService.class);

    final LibCategoryRepository libCategoryRepository;

    public LibCategoryServiceImpl(LibCategoryRepository libCategoryRepository) {
        this.libCategoryRepository = libCategoryRepository;
    }

    @Override
    public List<LibraryCategory> getAllCategories() {
        List<LibraryCategory> categories = new ArrayList<>();
        List<LibCategoryEntity> all = libCategoryRepository.findAll();
        LOG.info("Found {} ProjectProfileEntities for request.", Iterables.size(all));
        for (LibCategoryEntity entity : all) {
            // Note: Mapping currently not implemented - requires mappingService integration
            throw new NotImplementedException();
        }
        return categories;
    }

    @Override
    @Transactional
    public void importLibraryCategory(String matildaCategory) throws Exception {
        if(StringUtils.isNotEmpty(matildaCategory)) {
            LibCategoryEntity entity = libCategoryRepository.findByLabel(matildaCategory)
                    .orElse(new LibCategoryEntity(matildaCategory));
            if(StringUtils.equals(entity.getLabel(), matildaCategory)) {
                entity.setLabel(matildaCategory);
            }
            libCategoryRepository.save(entity);
        } else {
            throw new Exception("Provided Library does not contain mandatory fields.");
        }
    }

    public LibCategoryEntity getLibCategoryFromLabel(String decisionSubject) throws Exception {
        if(StringUtils.isNotEmpty(decisionSubject)) {
            if(LibCategory.getByMatildaCategory(decisionSubject) == null) {
                decisionSubject = categoryMapping(decisionSubject);
            }
            LibCategoryEntity libCategory = libCategoryRepository.findByLabel(decisionSubject).orElseThrow(Exception::new);
            return libCategory;
        }
        return null;
    }

    private String categoryMapping(String category) {
        if(StringUtils.isNotEmpty(category)) {
            category = StringHandler.stripForCategoryEnum(category);
            return LibCategory.valueOf(category).getMatildaCategory();
        } else {
            return null;
        }
    }
}
