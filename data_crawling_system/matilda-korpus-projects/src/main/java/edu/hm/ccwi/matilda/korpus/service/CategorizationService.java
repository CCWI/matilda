package edu.hm.ccwi.matilda.korpus.service;

import edu.hm.ccwi.matilda.korpus.service.model.Domain;

import java.io.IOException;
import java.util.Map;

public interface CategorizationService {

    public Map<String, Domain> getCategoriesTagsOfMvnRepo();
}
