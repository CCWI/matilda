package edu.hm.ccwi.matilda.korpus.service.impl;

import edu.hm.ccwi.matilda.korpus.service.CategorizationService;
import edu.hm.ccwi.matilda.korpus.service.model.Domain;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategorizationServiceImpl implements CategorizationService {

    String csvFile = "korpus/mvnRepoCatTags.csv";
    String line = "";
    String cvsSplitBy = ";";

    public Map<String, Domain> getCategoriesTagsOfMvnRepo() {

        Map<String, Domain> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            while ((line = br.readLine()) != null) {
                String[] mvnRepoCatTagsOfCSV = line.split(cvsSplitBy);

                //check if libraryGA already used -> else new:
                Domain domainObject;
                if(map.containsKey(mvnRepoCatTagsOfCSV[0])) {
                    domainObject = map.get(mvnRepoCatTagsOfCSV[0]);
                    if(!mvnRepoCatTagsOfCSV[1].isEmpty()) { domainObject.setMvnCategory(mvnRepoCatTagsOfCSV[1]); }
                    if(mvnRepoCatTagsOfCSV.length>2 && !mvnRepoCatTagsOfCSV[2].isEmpty()) { domainObject.addMvnTag(mvnRepoCatTagsOfCSV[2]); }
                } else {
                    domainObject = new Domain();
                    if(!mvnRepoCatTagsOfCSV[1].isEmpty()) { domainObject.setMvnCategory(mvnRepoCatTagsOfCSV[1]); }
                    if(mvnRepoCatTagsOfCSV.length>2 && !mvnRepoCatTagsOfCSV[2].isEmpty()) { domainObject.addMvnTag(mvnRepoCatTagsOfCSV[2]); }
                    map.put(mvnRepoCatTagsOfCSV[0], domainObject);
                }
            }
        } catch (IOException e) {
            LOG.error("Error during categorization", e);
        }

        return map;
    }
}
