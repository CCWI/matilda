package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.korpus.libsim.LibSimClient;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimException;
import edu.hm.ccwi.matilda.korpus.libsim.LibSimResult;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibCategoryEntity;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibCategoryRepository;
import edu.hm.ccwi.matilda.persistence.jpa.repo.LibraryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class DependencyLibraryHandlerTest {

    @InjectMocks
    private DependencyLibraryHandler dependencyLibraryHandler;

    @Mock
    private MvnRepoCrawler mvnRepoCrawler;

    @Mock
    private UtilService utilService;

    @Mock
    private LibCategoryRepository libCategoryRepository;

    @Mock
    private LibraryRepository libraryRepository;

    @Test
    public void assignCategoriesAndTagsTest_LibraryUnknown_ReceivingCategoryAndTags() throws IOException, LibSimException, InterruptedException {
        // Arrange
        CrawledDependency crawledDependency = new CrawledDependency("test.test", "test", "0.0.1-SNAPSHOT");

        String[] tags = {"api", "plugin", "sonar"};
        String targetCategory = "Code Analyses / Dynamic Program Analyses";
        
        // Mock libraryRepository to return empty (library not in cache/db)
        when(libraryRepository.findByGroupArtifactId(anyString())).thenReturn(Optional.empty());
        
        // Mock mvnRepoCrawler - not used in this test path since library is unknown
        when(mvnRepoCrawler.crawlMvnRepo(crawledDependency)).thenReturn(new MvnRepoPage("html", "Code Analyzers", tags));
        when(utilService.removeDuplicatesInList(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        // Mock LibCategory repository
        LibCategoryEntity libCategoryEntity = new LibCategoryEntity(999L, targetCategory, "true");
        when(libCategoryRepository.findByLabel(targetCategory)).thenReturn(Optional.of(libCategoryEntity));
        when(libCategoryRepository.findById(999L)).thenReturn(Optional.of(libCategoryEntity));
        
        // Mock LibSim KI
        LibSimResult result = new LibSimResult(true, targetCategory, "90", "");
        LibSimClient libSimClientMock = mock(LibSimClient.class);
        dependencyLibraryHandler.setLibSimClient(libSimClientMock);
        when(libSimClientMock.requestKi(anyString(), anyString(), any())).thenReturn(result);

        // Act
        CrawledDependency crawledDependency1 = dependencyLibraryHandler.assignCategoriesAndTags(crawledDependency);

        // Assert
        assertThat(crawledDependency1).isNotNull();
        assertThat(crawledDependency1.getGroup()).isEqualTo(crawledDependency.getGroup());
        assertThat(crawledDependency1.getArtifact()).isEqualTo(crawledDependency.getArtifact());
        assertThat(crawledDependency1.getCategory()).isEqualTo(targetCategory);
    }

    @Test
    public void assignCategoriesAndTagsTest_LibraryUnknown_ReceivingOnlyTags() throws IOException, LibSimException, InterruptedException {
        // Arrange
        String targetCategory = "Code Analyses / Dynamic Program Analyses";
        CrawledDependency crawledDependency = new CrawledDependency("test.test", "test", "0.0.1-SNAPSHOT");

        String[] tags = {"api", "plugin", "sonar"};
        
        // Mock libraryRepository to return empty (library not in cache/db)
        when(libraryRepository.findByGroupArtifactId(anyString())).thenReturn(Optional.empty());
        
        when(mvnRepoCrawler.crawlMvnRepo(crawledDependency)).thenReturn(new MvnRepoPage("html", null, tags));
        when(utilService.removeDuplicatesInList(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        
        LibSimResult result = new LibSimResult(true, targetCategory, "90", "");
        LibCategoryEntity libCategoryEntity = new LibCategoryEntity(999L, targetCategory, "true");
        when(libCategoryRepository.findByLabel(targetCategory)).thenReturn(Optional.of(libCategoryEntity));
        when(libCategoryRepository.findById(999L)).thenReturn(Optional.of(libCategoryEntity));

        LibSimClient libSimClientMock = mock(LibSimClient.class);
        dependencyLibraryHandler.setLibSimClient(libSimClientMock);
        when(libSimClientMock.requestKi(anyString(), anyString(), any())).thenReturn(result);

        // Act
        CrawledDependency crawledDependency1 = dependencyLibraryHandler.assignCategoriesAndTags(crawledDependency);

        // Assert
        assertThat(crawledDependency1).isNotNull();
        assertThat(crawledDependency1.getGroup()).isEqualTo(crawledDependency.getGroup());
        assertThat(crawledDependency1.getArtifact()).isEqualTo(crawledDependency.getArtifact());
        assertThat(crawledDependency1.getCategory()).isEqualTo(targetCategory);
    }
}