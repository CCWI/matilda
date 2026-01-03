package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;

/**
 *
 * @author Max.Auch
 */
public interface DataExtractorService {

	String extractorProcessor(String repoName, String projectName, String projDir, RepoSource source) throws Exception;

}