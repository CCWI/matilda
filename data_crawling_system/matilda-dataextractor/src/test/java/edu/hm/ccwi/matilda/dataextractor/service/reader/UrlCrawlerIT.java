package edu.hm.ccwi.matilda.dataextractor.service.reader;

import edu.hm.ccwi.matilda.base.exception.MatildaMappingException;
import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.dataextractor.service.DependencyLibraryHandler;
import edu.hm.ccwi.matilda.dataextractor.service.MvnRepoCrawler;
import edu.hm.ccwi.matilda.dataextractor.service.MvnRepoCrawlerImpl;
import edu.hm.ccwi.matilda.dataextractor.service.MvnRepoPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class UrlCrawlerIT {

    MvnRepoCrawler mvnRepoCrawler = new MvnRepoCrawlerImpl();
    DependencyLibraryHandler dependencyLibraryHandler = new DependencyLibraryHandler(
            null, null, mvnRepoCrawler, null, null);

    private CrawledDependency crawledDependency1;
    private CrawledDependency crawledDependency2;

    @BeforeEach
    public void initTest() {

        // First Dependency
        crawledDependency1 = new CrawledDependency();
        crawledDependency1.setGroup("org.javassist");
        crawledDependency1.setArtifact("javassist");

        //Second Dependency
        crawledDependency2 = new CrawledDependency();
        crawledDependency2.setGroup("org.springframework");
        crawledDependency2.setArtifact("spring-aop");

    }

    /**
     *
     * @throws InterruptedException
     * @throws MatildaMappingException
     */
    @Test
    public void mvnRepoCrawlerMvnRepositoryComIntegrationTest() {
        MvnRepoPage mvnRepoPage1 = mvnRepoCrawler.crawlMvnRepo(crawledDependency1);
        //LibCategory libCategory1 = LibraryUtilsAdapter.getLibCategoryByString(mvnRepoPage1.getCategory());
        LibCategory libCategoryFromMvnRepoPage = dependencyLibraryHandler.createLibCategoryFromMvnRepoPage(crawledDependency1, mvnRepoPage1);

        assertThat(libCategoryFromMvnRepoPage).isEqualTo(LibCategory.BYTECODE_LIBRARIES);

        assertThat(1).isEqualTo(2);
/*        CrawledDependency resultDependency1 = mvnRepoCrawler.categorizeTagRelevanceExaminer(crawledDependency1, libCategory1, mvnRepoPage1.getTagMatches());
        printCrawledMvnRepoResult(resultDependency1);
        assertThat(resultDependency1.getCategory()).equals(LibCategory.BYTECODE_LIBRARIES.getName());
        assertTrue(resultDependency1.getTagList().contains("bytecode"));

        Thread.sleep(1000); // Prevent blocking from mvnrepository

        MvnRepoPage mvnRepoPage2 = mvnRepoCrawler.crawlMvnRepo(crawledDependency2);
        LibCategory libCategory2 = LibraryUtilsAdapter.getLibCategoryByString(mvnRepoPage2.getCategory());
        CrawledDependency resultDependency2 = mvnRepoCrawler.categorizeTagRelevanceExaminer(crawledDependency2, libCategory2, mvnRepoPage2.getTagMatches());

        printCrawledMvnRepoResult(resultDependency2);

        assertTrue(resultDependency2.getCategory().equals(LibCategory.ASPECT_ORIENTED.getName()));
        assertTrue(resultDependency2.getTagList().contains("spring"));
        assertTrue(resultDependency2.getTagList().contains("aspect"));
        assertTrue(resultDependency2.getTagList().contains("aop"));*/
    }
}