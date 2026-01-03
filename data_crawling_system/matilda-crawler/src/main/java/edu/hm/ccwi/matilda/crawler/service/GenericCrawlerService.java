package edu.hm.ccwi.matilda.crawler.service;

import com.google.common.collect.Iterables;
import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.base.util.GitCommons;
import edu.hm.ccwi.matilda.crawler.exception.CrawlerException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.validation.constraints.NotEmpty;
import java.io.File;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Max.Auch
 */
@Service
public class GenericCrawlerService {

    private static final Logger LOG = LoggerFactory.getLogger(GenericCrawlerService.class);

    private final JanitorService janitorService;
    private final List<String> filterFileNames;
    private final List<String> dmFFilterFileNames;
    private final List<String> documentationFilterFileNames;

    @Value("${matilda.crawler.project.size.limit}")
    private int projectSizeLimit;

    public GenericCrawlerService(JanitorService janitorService) {
        this.janitorService = janitorService;

        List<String> filterAllFiles = Stream.of(FileFilter.dMFNameFilter(), FileFilter.documentationNameFilter())
                .flatMap(Collection::stream).collect(Collectors.toList());
        for (String filename : filterAllFiles) {
            filterAllFiles.set(filterAllFiles.indexOf(filename), filename.toLowerCase());
        }
        this.filterFileNames = filterAllFiles;

        List<String> filterForDmfFiles = Stream.of(FileFilter.dMFNameFilter())
                .flatMap(Collection::stream).collect(Collectors.toList());
        for (String filename : filterForDmfFiles) {
            filterForDmfFiles.set(filterForDmfFiles.indexOf(filename), filename.toLowerCase());
        }
        this.dmFFilterFileNames = filterForDmfFiles;

        List<String> filterForDocFiles = Stream.of(FileFilter.documentationNameFilter())
                .flatMap(Collection::stream).collect(Collectors.toList());
        for (String filename : filterForDocFiles) {
            filterForDocFiles.set(filterForDocFiles.indexOf(filename), filename.toLowerCase());
        }
        this.documentationFilterFileNames = filterForDocFiles;
    }

    protected String crawlRemoteRepo(@NotEmpty String repoProjName, @NotEmpty String rootPath,
                                     boolean runJanitor, LocalDate dateSince, @NotEmpty String gitSource,
                                     @NotEmpty String repoHttpUrl) {
        LOG.info("start initial clone and crawling of: " + repoProjName + "---" + rootPath);

        String repoPath = rootPath + File.separator + repoProjName;
        File projectDir = new File(repoPath);
        Path clonePath = Paths.get(repoPath + File.separator + "clone");
        File commitDir = new File(repoPath + File.separator + "commits");

        try (Git git = Git.cloneRepository().setURI(repoHttpUrl).setDirectory(Files.createDirectories(clonePath).toFile())
                .setCloneAllBranches(true).setTimeout(300).call()) {
            List<String> branchList = fetchAllRemoteGitBranches(repoHttpUrl);
            LOG.info("fetchedBranches for crawling: {}", branchList);
            crawlRepo(repoProjName, rootPath, runJanitor, dateSince, gitSource, git, branchList, false);
        } catch (TransportException e) {
            LOG.error("TransportException: Authentication is required but no CredentialsProvider has been registered.");
        } catch (AccessDeniedException e) {
            LOG.error("AccessDeniedException: Wasn't able to access repo {}.", repoProjName);
        } catch (Exception e) {
            LOG.error("An unspecific exception occurred: ", e);
        }

        //clean up cloned project if needed (if commits are empty)
        return janitorService.cleanUpClonedProject(projectDir, commitDir) ? rootPath + File.separator + repoProjName : null;
    }

    protected void crawlLocalRepo(ProcessingProjectDto tcDto, Git git) throws GitAPIException {
        List<String> branchList = new ArrayList<>();
        for (Ref ref : git.branchList().call()) {
            branchList.add(ref.getName().substring(ref.getName().lastIndexOf("/") + 1));
        }
        Collections.sort(branchList);
        LOG.info("fetchedBranches for crawling: {}", branchList);
        crawlRepo(tcDto.getRepoProjName(), tcDto.getTargetPath(), true, LocalDate.now().minusYears(10),
                null, git, branchList, true);
    }

    /**
     * iterate each Branch in cloned repository
     */
    private void crawlRepo(String repoProjName, String rootPath, boolean runJanitor, LocalDate dateSince, String sourceRepo,
                           Git git, List<String> branchList, boolean localCrawling) {
        int notRelevantCounter = 0;
        for (int branchCrawlCounter = 0; branchCrawlCounter < branchList.size(); branchCrawlCounter++) {
            try {
                String branchName = branchList.get(branchCrawlCounter);
                LOG.info("[" + (branchCrawlCounter + 1) + "/" + branchList.size() + "] Crawl branch {} for commits", branchName);
                boolean isRelevant = crawlBranch(repoProjName, rootPath, runJanitor, dateSince, sourceRepo, git, branchName, localCrawling);
                if (isRelevant) {
                    notRelevantCounter = 0;
                } else {
                    notRelevantCounter++;
                    if (notRelevantCounter >= 10) {
                        LOG.info("    No relevant branches found in last " + notRelevantCounter + " branches. Stop crawling.");
                        break;
                    }
                }
            } catch (Exception e) {
                LOG.error("    An error occurred while crawling branch: {} -> continue with next branch", e.getMessage());
            }
        }
    }

    private boolean crawlBranch(String repoProjName, String rootPath, boolean runJanitor, LocalDate dateSince, String sourceRepo,
                                Git git, String branchName, boolean localCrawling) throws IOException, GitAPIException,
                                InterruptedException, CrawlerException {
        try {
            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK)
                    .setStartPoint("origin/" + branchName)
                    .setForced(true)
                    .call();
            Thread.sleep(5000);
            if(localCrawling) {
                git.pull().call();
                Thread.sleep(2000);
            }
        } catch (RefAlreadyExistsException e) {
            LOG.debug("    Ref already exists in local repo {} and cannot be checked out: {}", repoProjName, branchName);
        } catch (RefNotFoundException e) {
            LOG.warn("    Branch-Ref {} not found somehow in repo: {}", branchName, repoProjName);
            throw new CrawlerException("No Branch-reference found");
        }

        boolean isRelevant = crawlCloneForRelevantCommits(git, repoProjName, rootPath, runJanitor, dateSince);
        writeSourceRepoToFilesystem(repoProjName, rootPath, sourceRepo);

        return isRelevant;
    }

    private void writeSourceRepoToFilesystem(String repoProjName, String rootPath, String sourceRepo) throws IOException {
        if(StringUtils.isNotEmpty(sourceRepo)) {
            FileUtils.writeByteArrayToFile(
                    new File(rootPath + File.separator + repoProjName + File.separator + sourceRepo), "".getBytes());
        }
    }

    private boolean crawlCloneForRelevantCommits(Git git, String repoProjName, String rootPath, boolean runJanitor,
                                                LocalDate dateSince) throws IOException, GitAPIException {
        LOG.info("    Start crawling clone for commits");
        Map<String, File> crawledRepoDirectories = new HashMap<>();

        // check single commits first if repo is bigger
        boolean isRelevant = true;
        if (git.log().all().call() != null && Iterables.size(git.log().all().call()) > this.projectSizeLimit) {
            isRelevant = isLargeRepositoryRelevant(git, repoProjName, rootPath, crawledRepoDirectories);
        }

        if (isRelevant) {
            crawlCloneForCommits(git, repoProjName, rootPath, dateSince, crawledRepoDirectories);
        } else {
            LOG.warn("    Large project branch is considered to be not relevant -> skip!");
        }
        if (runJanitor) {
            crawledRepoDirectories.forEach((crawledName, crawledDir) ->
                    janitorService.cleanUpCommit(crawledDir, FileFilter.dMFNameFilter(), FileFilter.documentationNameFilter()));
        }

        return isRelevant;
    }

    private List<String> fetchAllRemoteGitBranches(String gitUrl) {
        List<String> branches = new ArrayList<>();
        try {
            for (Ref ref : Git.lsRemoteRepository().setHeads(true).setRemote(gitUrl).call()) {
                branches.add(ref.getName().substring(ref.getName().lastIndexOf("/") + 1));
            }
            Collections.sort(branches);
        } catch (InvalidRemoteException e) {
            LOG.error("InvalidRemoteException occurred in fetchGitBranches of {}.", gitUrl);
        } catch (TransportException e) {
            LOG.error("TransportException occurred while fetching GitBranches of {}.", gitUrl);
        } catch (GitAPIException e) {
            LOG.error("GitAPIException occurred in fetchGitBranches of {}.", gitUrl);
        }
        return branches;
    }

    private void crawlCloneForCommits(Git git, String repoProjName, String rootPath, LocalDate dateSince, Map<String, File> crawledRepoDirectories)
            throws GitAPIException, IOException {
        LOG.info("    {} commits of project are considered as relevant - commits are going to be extracted", Iterables.size(git.log().all().call()));
        int createCommitDirCounter = 0;
        int dateOfCommitBeforeConfiguredDateCounter = 0;
        for (RevCommit revC : git.log().all().call()) {
            if (getLocalDateOfCommit(revC).isAfter(dateSince)) {
                String rootCommitDir = rootPath + File.separator + repoProjName + File.separator + "commits"
                        + File.separator + GitCommons.createCommitName(revC);
                createCommitDirectory(git, crawledRepoDirectories, this.filterFileNames, revC, rootCommitDir);
                createCommitDirCounter++;
            } else {
                dateOfCommitBeforeConfiguredDateCounter++;
            }
        }
        LOG.info("    --> {} commit-dirs created", createCommitDirCounter);
        LOG.info("    --> {} commit-dirs before {}", dateOfCommitBeforeConfiguredDateCounter, dateSince.toString());
    }

    private LocalDate getLocalDateOfCommit(RevCommit revC) {
        return Instant.ofEpochSecond(revC.getCommitTime()).atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private boolean isLargeRepositoryRelevant(Git git, String repoProjName, String rootPath, Map<String, File> crawledRepoDirectories) {
        LOG.info("    LARGE REPOSITORY DETECTED. Check large project on relevant files .....");
        boolean isRelevant;
        isRelevant = false;
        try {
            RevCommit latestCommit = git.log().setMaxCount(1).call().iterator().next();
            String rootCommitDir = rootPath + File.separator + repoProjName + File.separator + "commits"
                    + File.separator + GitCommons.createCommitName(latestCommit);
            createCommitDirectory(git, crawledRepoDirectories, this.filterFileNames, latestCommit, rootCommitDir);
            checkRelevanceOfCommit(Files.walk(Paths.get(new File(rootCommitDir).toURI())).filter(Files::isRegularFile).collect(Collectors.toList()));
            LOG.warn("      NO relevant commits are found in example commit dir of large project. Project is NOT relevant");
        } catch (Exception e) {
            LOG.info("      {} --> continue check all commits now.", e.getMessage());
            isRelevant = true;
        }
        return isRelevant;
    }

    private void checkRelevanceOfCommit(List<Path> filePathList) throws Exception {
        if (!CollectionUtils.isEmpty(filePathList)) {
            LOG.info("    ... Found {} files in commit directory to check whether relevant file is included. ", filePathList.size());
            boolean isDmfFileAvailable = false;
            boolean isDocumentationAvailable = false;
            for (Path filePath : filePathList) {
                if (isRelevantFileInPath(this.dmFFilterFileNames, filePath)) {
                    isDmfFileAvailable = true;
                }
                if (isRelevantFileInPath(this.documentationFilterFileNames, filePath)) {
                    isDocumentationAvailable = true;
                }
                if (isDmfFileAvailable && isDocumentationAvailable) {
                    LOG.info("    ++++ Found relevant file in initial search: " + filePath);
                    throw new Exception("Found relevant file during initial search in large repository");
                }
            }
        }
    }

    private boolean isRelevantFileInPath(List<String> filterFileNames, Path filePath) {
        return filePath != null && filterFileNames.stream().anyMatch(d -> d.equalsIgnoreCase(filePath.toFile().getName()));
    }

    private void createCommitDirectory(Git git, Map<String, File> crawledRepoDirectories, List<String> filterFileNames,
                                       RevCommit revC, String rootCommitDir) {
        //crawl commit if it does not exist already!
        File commitDir = new File(rootCommitDir);
        if (!commitDir.exists()) {
            try (TreeWalk treeWalk = new TreeWalk(git.getRepository())) {
                treeWalk.addTree(revC.getTree());
                treeWalk.setRecursive(false);
                try (ObjectReader objectReader = git.getRepository().newObjectReader()) {
                    while (treeWalk.next()) {
                        if (treeWalk.isSubtree()) {
                            treeWalk.enterSubtree();
                        } else if (filterFileNames.contains(treeWalk.getNameString().toLowerCase())) {
                            handleRelevantFileInCommit(crawledRepoDirectories, revC, rootCommitDir, commitDir, treeWalk, objectReader);
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Exception was thrown while crawling commit: " + e.getMessage());
            }
        }
    }

    private void handleRelevantFileInCommit(Map<String, File> crawledRepoDirectories, RevCommit revC, String rootCommitDir,
                                            File commitDir, TreeWalk treeWalk, ObjectReader objectReader) throws IOException {
        if (!commitDir.exists()) {
            Files.createDirectories(commitDir.toPath());
        }
        File file = new File(rootCommitDir + File.separator + treeWalk.getPathString());
        if (file.isDirectory()) {
            Files.createDirectories(file.toPath());
        } else {
            try {
                ObjectLoader objectLoader = objectReader.open(treeWalk.getObjectId(0));
                if (objectLoader != null) {
                    FileUtils.writeByteArrayToFile(file, objectLoader.getBytes());
                }
            } catch (IOException e) {
                LOG.error("Error while writeByteArrayToFile: " + e.getMessage());
            }
            crawledRepoDirectories.put(revC.getId().toString(), new File(rootCommitDir));
        }
    }
}