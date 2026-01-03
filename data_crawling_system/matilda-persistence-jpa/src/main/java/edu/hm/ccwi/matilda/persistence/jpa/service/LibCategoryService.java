package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.base.model.library.LibraryCategory;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;

import java.util.List;

public interface LibCategoryService {

    List<LibraryCategory> getAllCategories();

    void importLibraryCategory(String matildaCategory) throws Exception;

    LibCategoryEntity getLibCategoryFromLabel(String decisionSubject) throws Exception;
}
