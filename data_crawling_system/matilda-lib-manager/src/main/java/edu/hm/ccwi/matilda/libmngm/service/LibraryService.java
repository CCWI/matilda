package edu.hm.ccwi.matilda.libmngm.service;

import edu.hm.ccwi.matilda.base.model.library.Library;

import java.util.List;

public interface LibraryService {

    List<Library> getLibrariesOfCategory(String categoryId);

    String saveOrUpdateLibrary(Library library) throws Exception;
}
