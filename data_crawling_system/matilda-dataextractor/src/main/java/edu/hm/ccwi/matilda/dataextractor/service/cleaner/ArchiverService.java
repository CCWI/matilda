package edu.hm.ccwi.matilda.dataextractor.service.cleaner;

/**
 *
 * @author Max.Auch
 */
public interface ArchiverService {

	void archiveCrawledProjectDirectory(String dir);

	void archiveCrawledDirectoriesRecursively(String rootDir);

	void reactivateArchivedFolderStructure(String rootDir);
}