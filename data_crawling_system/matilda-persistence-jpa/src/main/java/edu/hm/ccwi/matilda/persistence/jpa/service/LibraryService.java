package edu.hm.ccwi.matilda.persistence.jpa.service;

import edu.hm.ccwi.matilda.base.model.library.Library;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;

import java.util.List;

public interface LibraryService {

    List<Library> getLibrariesOfCategory(String categoryId);

    String saveOrUpdateLibrary(Library library) throws Exception;

    LibraryEntity saveOrUpdateLibrary(String gaId, String libCategoryLabel, String tags) throws IllegalArgumentException;

    LibraryEntity saveOrUpdateLibrary(String gaId, LibCategoryEntity libCategoryEntity, String tags) throws IllegalArgumentException;
}
