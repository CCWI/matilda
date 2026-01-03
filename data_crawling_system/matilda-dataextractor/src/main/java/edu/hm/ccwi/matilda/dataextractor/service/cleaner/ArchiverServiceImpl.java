package edu.hm.ccwi.matilda.dataextractor.service.cleaner;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;

/**
 * Extract data from project including dependency-details and documentation.
 *
 * @author Max.Auch
 */
@Service
public class ArchiverServiceImpl implements ArchiverService {

    private static final Logger LOG = LoggerFactory.getLogger(ArchiverServiceImpl.class);

    private final JanitorService janitorService;

    public ArchiverServiceImpl(JanitorService janitorService) {
        this.janitorService = janitorService;
    }

    @Override
    public void archiveCrawledDirectoriesRecursively(String rootDir) {
        long start = System.nanoTime();  //----------------------------------------------------------------------------
        for (File repositoryFile : new File(rootDir).listFiles()) {
            if (repositoryFile == null || repositoryFile.listFiles() == null) {
                LOG.warn("Repository {} does not contain the required folders/files -> Is ignored!", repositoryFile);
                continue;
            }
            for (File dir : repositoryFile.listFiles()) {
                archiveCrawledProjectDirectory(dir.getAbsolutePath());
            }
        }
        LOG.info("    ### End cleanup and archive Project - it took {} seconds.", ((System.nanoTime() - start) / 1000000000));
    }

    @Override
    public void archiveCrawledProjectDirectory(String projDir) {
        try {
            // 1) check if folder (deep: 2) contains commit + clone folder
            File commitDirFile = new File(projDir + File.separator + "commits");
            File cloneDirFile = new File(projDir + File.separator + "clone");

            if (commitDirFile.exists() && commitDirFile.isDirectory()
                    && cloneDirFile.exists() && cloneDirFile.isDirectory()) {
                // 2) zip clone folder
                janitorService.archiveCrawledProject(projDir, "clone");
                // 3) wait and remove commit + clone folder
                Thread.sleep(2000);
                if (new File(projDir + File.separator + "clone.zip").exists()) {
                    janitorService.cleanUpFolder(new File(projDir + File.separator + "commits"));
                    janitorService.cleanUpFolder(new File(projDir + File.separator + "clone"));
                }
            } else {
                LOG.warn("Directory {} does not contain the required folders/files -> Is ignored!", projDir);
            }
        } catch (IOException | InterruptedException e) {
            LOG.error("Error on archiving Project: {} ---> {}", projDir, e.getMessage());
        }
    }

    @Override
    public void reactivateArchivedFolderStructure(String rootDir) {
        String zipFileName = "clone.zip";
        String projDir = null;

        File rootDirectory = new File(rootDir);
        for (File repositoryFile : rootDirectory.listFiles()) {
            if (repositoryFile == null || repositoryFile.listFiles() == null) {
                LOG.warn("Repository {} does not contain the required folders/files -> Is ignored!", repositoryFile);
                continue;
            }

            try {
                for (File dir : repositoryFile.listFiles()) {
                    projDir = dir.getAbsolutePath();
                    if (new File(projDir + File.separator + zipFileName).exists()) {
                        LOG.info("  Unzip Project: {}", projDir + File.separator + zipFileName);
                        // unzip clone
                        janitorService.unzipIO(new File(projDir + File.separator + zipFileName).toPath(), Charset.defaultCharset());

                        // remove clone.zip
                        FileUtils.forceDelete(new File(projDir + File.separator + zipFileName));
                    }
                }
            } catch (IOException e) {
                LOG.error("  Error while unzipping file in: {} --> {}", projDir, e.getMessage());
            }
        }
    }
}