package edu.hm.ccwi.matilda.dataextractor.service.reader;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;

import java.io.File;
import java.util.List;

public interface DMFReader {

    List<File> findAllRelatedFilesInProject(String projectUri);

    List<CrawledProject> extractProjectDependency(List<File> filesInProject, String revisionDirPath,
                                                  String repoName, String projectName) throws Exception;

}
