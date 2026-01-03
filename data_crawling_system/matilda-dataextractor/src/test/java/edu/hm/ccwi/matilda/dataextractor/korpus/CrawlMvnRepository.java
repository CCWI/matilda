package edu.hm.ccwi.matilda.dataextractor.korpus;

import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.dataextractor.MatildaDataextractorApplication;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.base.util.LibraryUtilsAdapter;
import edu.hm.ccwi.matilda.dataextractor.service.*;
import edu.hm.ccwi.matilda.persistence.mongo.repo.GACategoryTagRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * NO TEST!
 */
@SpringBootTest(classes = MatildaDataextractorApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
public class CrawlMvnRepository {

    @Inject
    private GACategoryTagRepository gaCatTagRepo;

    @Inject
    private UtilService utilService; //TODO UNTESTED INJECTION, SINCE IT WAS NECESSARY AFTER REFACTORING

    /**
     * STEP #3
     */
    @Test
    public void queryMvnRepoAndWriteToCsvAndDB() {
        MvnRepoCrawler mvnRepoCrawler = new MvnRepoCrawlerImpl();
        DependencyLibraryHandler libHandler = new DependencyLibraryHandler(null, utilService,
                null, null, null);

        String inputCsv = "gaEntryList-GEO-attempt2.csv";
        String[] mcDependencyList = readStateOfLastStep(inputCsv);

        int stateCounter = 0;
        int totalCounter = 1535;

        for (String s : mcDependencyList) {
            try {
                System.out.println("[ " + stateCounter + "/" + totalCounter + " ] Crawl MvnRepo on GA: " + s);
                String[] depParts = s.split("/");

                if (gaCatTagRepo.findById(depParts[0] + ":" + depParts[1]).isEmpty()) {
                    CrawledDependency dependency = new CrawledDependency();
                    dependency.setGroup(depParts[0]);
                    dependency.setArtifact(depParts[1]);
                    MvnRepoPage mvnRepoPage = mvnRepoCrawler.crawlMvnRepo(dependency);
                    LibCategory libCategory = LibraryUtilsAdapter.resolveLibCategoryByString(mvnRepoPage.getCategory());
                    dependency = libHandler.enrichCrawledDependency(dependency, libCategory, mvnRepoPage.getTagMatches());
                    dependency.setTagList(removeDuplicatesInList(dependency.getTagList()));
                    gaCatTagRepo.save(new GACategoryTag(dependency.getGroup(), dependency.getArtifact(), dependency.getCategory(), dependency.getTagList(), true));
                } else {
                    System.out.println("  ---> Library already crawled, skip...");
                }
            } catch (Throwable e) {
                System.err.println("Exception thrown for: " + s + " --> Msg: " + e.getMessage());
            }
            stateCounter++;
        }
    }

    @Test
    public void updateDbByRecrawlMvnRepo() {
        MvnRepoCrawler mvnRepoCrawler = new MvnRepoCrawlerImpl();
        DependencyLibraryHandler libHandler = new DependencyLibraryHandler(null, utilService,
                null, null, null);

        String inputCsv = "recrawlAccidentiallyOverwrittenByInitProcessLibs-attempt5.csv";
        String[] mcDependencyList = readStateOfLastStep(inputCsv);

        int stateCounter = 0;
        int totalCounter = 3684;

        for (String s : mcDependencyList) {
            try {
                System.out.println("[ " + stateCounter + "/" + totalCounter + " ] Crawl MvnRepo on GA: " + s);
                String[] depParts = s.split(":");

                GACategoryTag gaCategoryTag = gaCatTagRepo.findById(depParts[0] + ":" + depParts[1]).orElse(null);

                if (gaCategoryTag != null) {
                    CrawledDependency dependency = new CrawledDependency();
                    dependency.setGroup(depParts[0]);
                    dependency.setArtifact(depParts[1]);
                    MvnRepoPage mvnRepoPage = mvnRepoCrawler.crawlMvnRepo(dependency);
                    LibCategory libCategory = LibraryUtilsAdapter.resolveLibCategoryByString(mvnRepoPage.getCategory());
                    dependency = libHandler.enrichCrawledDependency(dependency, libCategory, mvnRepoPage.getTagMatches());
                    dependency.setTagList(removeDuplicatesInList(dependency.getTagList()));

                    System.out.println("   ---> FOUND TAGS: " + Arrays.toString(dependency.getTagList().toArray()));

                    gaCategoryTag.setCategory(dependency.getCategory());
                    gaCategoryTag.setTags(dependency.getTagList());

                    gaCatTagRepo.save(gaCategoryTag);
                } else {
                    System.out.println("  ---> Cannot find Entry in DB - NO RECRAWL SO SKIP!");
                }
            } catch (Throwable e) {
                System.err.println("Exception thrown for: " + s + " --> Msg: " + e.getMessage());
            }
            stateCounter++;
        }
    }

    public List<String> removeDuplicatesInList(List<String> l) {
        if(l != null && !l.isEmpty()) {
            Set<String> s = new TreeSet<>(l);
            return Arrays.asList(s.toArray(String[]::new));
        } else {
            return l;
        }
    }

    public String[] readStateOfLastStep(String fileName) {
        List<String> crawlableUris = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("/" + fileName);
        InputStreamReader streamReader = null;
        if (is != null) {
            streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
        }

        if (streamReader != null) {
            try (BufferedReader br = new BufferedReader(streamReader)) {
                String line = br.readLine();
                while (line != null) {
                    crawlableUris.add(line);
                    line = br.readLine();
                }
            } catch (IOException e) {
                System.err.println("Error on initializing category and tag lists for dependencies! Not able to load init-csv.");
            }
        }

        return crawlableUris.toArray(String[]::new);
    }
}
