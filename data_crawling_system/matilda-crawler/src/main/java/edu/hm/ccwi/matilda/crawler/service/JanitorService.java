package edu.hm.ccwi.matilda.crawler.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.base.util.IOHandler;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledStatisticRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static java.util.stream.Collectors.toList;

@Service
public class JanitorService {

    private static final Logger LOG = LoggerFactory.getLogger(JanitorService.class);

    private final CrawledSoftwareRepository crawledSoftwareRepository;
    private final CrawledRevisionRepository crawledRevisionRepository;
    private final CrawledDocumentationRepository crawledDocumentationRepository;
    private final CrawledStatisticRepository crawledStatisticRepository;

    public JanitorService(CrawledSoftwareRepository crawledSoftwareRepository, CrawledRevisionRepository crawledRevisionRepository,
                          CrawledDocumentationRepository crawledDocumentationRepository, CrawledStatisticRepository crawledStatisticRepository) {
        this.crawledSoftwareRepository = crawledSoftwareRepository;
        this.crawledRevisionRepository = crawledRevisionRepository;
        this.crawledDocumentationRepository = crawledDocumentationRepository;
        this.crawledStatisticRepository = crawledStatisticRepository;
    }

    public void cleanUpCommit(File crawledDir, List<String> dMFNameFilterList, List<String> docNameFilterList) {
        boolean containsDMF = false;
        boolean containsDoc = false;
        File[] fileList = crawledDir.listFiles();
        if (fileList != null) {
            try (Stream<Path> walk = Files.walk(Paths.get(crawledDir.getPath()))) {
                List<Path> pathList = walk.filter(Files::isRegularFile).collect(toList());
                for (Path path : pathList) {
                    if (fileAvailable(path, dMFNameFilterList)) { containsDMF = true; }
                    if (fileAvailable(path, docNameFilterList)) { containsDoc = true; }
                }
            } catch (IOException e) {
                LOG.warn("Hi! Here the Janitor. I had an error while cleaning up a commit.", e);
            }
            if (importantFilesMissing(containsDMF, containsDoc)) {
                FileSystemUtils.deleteRecursively(crawledDir);
            }
        }
    }

    private boolean importantFilesMissing(boolean containsDMF, boolean containsDoc) {
        return !containsDMF || !containsDoc;
    }

    public void cleanUpProjectInfoFromDatabases(String repoProjName, String repoName, String projectName, String repoSource) {
        LOG.info("Cleanup project info from database (mongo)...");
        String repoProjId = repoProjName.replace("/", ":").replace("\\", ":").trim();

        Optional<CrawledRepository> crawledRepositoryOpt = crawledSoftwareRepository.findById(repoProjId);

        if (crawledRepositoryOpt.isPresent()) {
            LOG.info("  Cleanup: Found crawled repository for id: {}", repoProjId);
            CrawledRepository crawledRepository = crawledRepositoryOpt.get();
            LOG.info("  Cleanup: Crawled Repository: repo:{} / proj:{} / revs:{}", crawledRepository.getRepositoryName(),
                    crawledRepository.getProjectName(), crawledRepository.getRevisionCommitIdList().size());
            for (String revCommitId : crawledRepository.getRevisionCommitIdList()) {
                if (crawledRevisionRepository.existsById(revCommitId)) { crawledRevisionRepository.deleteById(revCommitId); }
                if (crawledDocumentationRepository.existsById(revCommitId)) { crawledDocumentationRepository.deleteById(revCommitId); }
                String statisticId = repoName + "-" + projectName + "-" + repoSource;
                if(crawledStatisticRepository.existsById(statisticId)) { crawledStatisticRepository.deleteById(statisticId); }
            }
            LOG.info("  Cleanup: Finish cleanup by deleting crawled Repository itself.");
            crawledSoftwareRepository.delete(crawledRepository);
        } else {
            LOG.error(  "Cleanup: NO RepoProject found for id: {}", repoProjId);
        }
    }

    public boolean cleanUpClonedProject(File projectDir, File commitDir) {
        try {
            try (Stream<Path> list = Files.list(Paths.get(commitDir.toURI()))) {
                if (!commitDir.exists() || !commitDir.isDirectory() || list.findAny().isEmpty()) {
                    LOG.info("cleanUpClonedProject() is executed, since it is not relevant: {}.", projectDir.getName());
                    IOHandler.removeDirSafely(projectDir);
                    return false;
                }
            }
        } catch (IOException e) {
            LOG.warn("IOExpection while listing files in commitDir or repoDir: " + e.getMessage());
            return false;
        }
        return true;
    }

    public void reactivateArchivedFolderStructure(String projectDir) {
        String zipFileName = "clone.zip";
        String projDir = null;

        File rootProjectDirectory = new File(projectDir);

        try {
            projDir = rootProjectDirectory.getAbsolutePath();
            if (rootProjectDirectory.exists() && new File(projDir + File.separator + zipFileName).exists()) {
                LOG.info("  Unzip Project: {}", projDir + File.separator + zipFileName);
                unzipIO(new File(projDir + File.separator + zipFileName).toPath(), Charset.defaultCharset());

                // remove clone.zip
                FileUtils.forceDelete(new File(projDir + File.separator + zipFileName));
            } else {
                LOG.warn("  Project does not contain a {} folders/files -> {}", zipFileName, projDir);
            }
        } catch (IOException e) {
            LOG.error("  Error while unzipping file in: {} --> {}", projDir, e.getMessage());
        }
    }

    private void unzipIO(Path path, Charset charset) throws IOException {
        String fileBaseName = FilenameUtils.getBaseName(path.getFileName().toString());
        Path destFolderPath = Paths.get(path.getParent().toString(), fileBaseName);

        try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ, charset)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destFolderPath.resolve(entry.getName());
                entryPath = new File(entryPath.toString().replace("clone" + File.separator + "clone", "clone"))
                        .toPath();
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)){
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }

    private boolean fileAvailable(Path path, List<String> searchList) {
        for (String dMFName : searchList) {
            if (path.getFileName().toString().trim().equalsIgnoreCase(dMFName.trim())) {
                return true;
            }
        }
        return false;
    }
}