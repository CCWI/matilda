package edu.hm.ccwi.matilda.korpus.rest;

import edu.hm.ccwi.matilda.korpus.service.CategorizationService;
import edu.hm.ccwi.matilda.korpus.service.ExportService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;

@RestController
@RequestMapping("/categorize")
public class CategorizationResource {

    @Inject
    CategorizationService categorizationService;

    @RequestMapping(value = "/mvnRepoCatTags", method = RequestMethod.GET)
    public void extractMvnRepoCatTags() {
        categorizationService.getCategoriesTagsOfMvnRepo();
    }


}