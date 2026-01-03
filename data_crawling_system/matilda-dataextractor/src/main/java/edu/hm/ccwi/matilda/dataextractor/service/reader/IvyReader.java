package edu.hm.ccwi.matilda.dataextractor.service.reader;

import edu.hm.ccwi.matilda.base.util.IOHandler;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to handle all entries in a ivy.xml
 * @author Max.Auch
 */
@Service
public class IvyReader implements DMFReader {

    private static final Logger LOG = LoggerFactory.getLogger(IvyReader.class);

    private static final String STD_IVY_FILE_NAME = "ivy.xml";

    public List<File> findAllRelatedFilesInProject(String projectUri) {
        return new IOHandler().findFile(STD_IVY_FILE_NAME, new File(projectUri));
    }

    /**
     * Extract all existing Dependencies from all dependency-management-files.
     *
     * @param revisionDirPath
     * @param repoName
     * @param projectName
     * @return
     * @throws Exception
     */
    public List<CrawledProject> extractProjectDependency(List<File> ivyFilesInProject, String revisionDirPath, String repoName, String projectName) throws Exception {
        List<CrawledProject> resultCPList = new ArrayList<>();

        for (File ivyFile : ivyFilesInProject) {
            // NOT IMPLEMENTED YET
        }
        return resultCPList;
    }

}