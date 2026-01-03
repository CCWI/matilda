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
 * Class to handle all entries in a gradle-file.
 * @author Max.Auch
 */
@Service
public class GradleReader implements DMFReader {

    private static final Logger LOG = LoggerFactory.getLogger(GradleReader.class);

    private static final String[] STD_GRADLE_FILE_NAMES =
            {"build.gradle", "gradle.properties", "settings.gradle"};

    public List<File> findAllRelatedFilesInProject(String revisionDir) {
        List<File> fileList = new ArrayList<>();
        IOHandler ioHandler = new IOHandler();
        for (String gradleFileName : STD_GRADLE_FILE_NAMES) {
            fileList.addAll(ioHandler.findFile(gradleFileName, new File(revisionDir)));
        }
        return fileList;
    }

    /**
     * Extract all existing Dependencies from all dependency-management-files.
     */
    public List<CrawledProject> extractProjectDependency(List<File> gradleFilesInProject, String revisionDirPath, String repoName, String projectName) throws Exception {
        List<CrawledProject> resultCPList = new ArrayList<>();

        //TODO
        /*
        buildscript { dependencies {
            classpath 'com.android.tools.build:gradle:3.0.0'
            classpath "com.antfortune.freeline:gradle:$FREELINE_RELEASE_VERSION"
        } }
         */

        return resultCPList;
    }
}
