package edu.hm.ccwi.matilda.crawler.service;

import edu.hm.ccwi.matilda.base.model.ProcessingProjectDto;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.crawler.client.CrawlerStateHandler;
import edu.hm.ccwi.matilda.crawler.exception.CrawlerException;
import edu.hm.ccwi.matilda.crawler.exception.InvalidCrawlerRequestException;
import edu.hm.ccwi.matilda.crawler.exception.RepositorySourceException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Service
public class CrawlerProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CrawlerProcessor.class);
    private final GenericCrawlerService genericCrawlerService;
    private final CrawlerBBService bitbucketCrawler;
    private final CrawlerGHService githubCrawler;
    private final CrawlerGLService gitlabCrawler;
    private final CrawlerStateHandler stateHandler;
    private final JanitorService janitorService;

    @Value("${matilda.crawler.toggle.check.remotely}")
    private boolean matildaCrawlerCheckRemotelyToggle;

    @Value("${matilda.crawler.toggle.recheck.locally}")
    private boolean matildaCrawlerRecheckLocallyToggle;

    private static int crawlCounter = 0;

    public CrawlerProcessor(GenericCrawlerService genericCrawlerService, CrawlerBBService bitbucketCrawler,
                                   CrawlerGHService githubCrawler, CrawlerGLService gitlabCrawler,
                                   CrawlerStateHandler crawlerStateHandler, JanitorService janitorService) {
        this.genericCrawlerService = genericCrawlerService;
        this.bitbucketCrawler = bitbucketCrawler;
        this.githubCrawler = githubCrawler;
        this.gitlabCrawler = gitlabCrawler;
        this.stateHandler = crawlerStateHandler;
        this.janitorService = janitorService;
    }

    public boolean processProjectDto(ProcessingProjectDto projectDto) {
        LOG.info("");
        LOG.info("");
        LOG.info("___________________________________________________________________________________________________");
        LOG.info("________[{}]__________ START CRAWLING OF {}/{} __________________", ++crawlCounter, projectDto.getRepoName(), projectDto.getProjectName());

        if (startExtraction(projectDto)) {
            LOG.info("  returned projDir for further processing: " + projectDto.getProjectDir());
            LOG.info("__________________ END CRAWLING OF {}/{} __________________", projectDto.getRepoName(), projectDto.getProjectDir());
            LOG.info("");
            LOG.info("");
            return true;
        } else {
            LOG.info("______________________ END crawling - Data not saved to forward for analysis ______________________");
            LOG.info("");
            LOG.info("");
            return false;
        }
    }

    private boolean startExtraction(ProcessingProjectDto projectDto) {
        MatildaStatusCode matildaStatusCode = null;
        ProjectProfile projectProfile = null;
        try {
            projectDto.setRepoName(projectDto.getRepoProjName().substring(0, projectDto.getRepoProjName().indexOf("/")));
            projectDto.setProjectName(projectDto.getRepoProjName().substring(projectDto.getRepoProjName().indexOf("/") + 1));

            if((projectDto.getRepoProjName().toLowerCase().contains("framework") && projectDto.getRepoProjName().toLowerCase().contains("base")) ||
                    (projectDto.getRepoProjName().toLowerCase().contains("framework") && projectDto.getRepoProjName().toLowerCase().contains("android"))) {
                throw new InvalidCrawlerRequestException("Seems like one of those uncrawleable projects -> skip!");
            }

            Instant start = Instant.now();

            projectProfile = stateHandler.getProjectProfileByRepoProj(projectDto.getRepoName(), projectDto.getProjectName());
            if (projectProfile != null) {
                matildaStatusCode = projectProfile.getStatus();
            }
            projectProfile = handleLoadedProjectState(projectProfile, projectDto);

            File cloneDir = new File(projectDto.getTargetPath() + File.separator + projectDto.getRepoProjName() + File.separator + "clone" + File.separator + ".git");
            File cloneZipDir = new File(projectDto.getTargetPath() + File.separator + projectDto.getRepoProjName() + File.separator + "clone.zip");

            boolean isInitialCrawlingNeeded = isInitialCrawlingNeeded(projectDto, projectProfile, cloneDir, cloneZipDir);
            boolean isRemoteUpdateNeeded = isRemoteUpdateNeeded(projectDto, projectProfile, cloneDir, cloneZipDir);

            if (crawlOrUpdateProject(projectDto, start, isInitialCrawlingNeeded, isRemoteUpdateNeeded, cloneDir, cloneZipDir)) {
                matildaStatusCode = MatildaStatusCode.FINISHED_CRAWLING;
                return true;
            } else {
                matildaStatusCode = MatildaStatusCode.ERROR_PROJECT_NOT_SUPPORTED;
            }
        } catch (GitAPIException e) {
            LOG.error("Some git related exception occurred: ", e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (IOException e) {
            LOG.error("Error while processing on disk: ", e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (InvalidCrawlerRequestException e) {
            LOG.error("Stop crawling because of invalid request: {}", e.getMessage());
            projectProfile = null; // Setting project profile to null to prevent updates in finally and just ignore request
        } catch (Exception e) {
            LOG.error("A general exception while crawling occurred: ", e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } finally {
            try {
                LOG.debug("Final check to update projectProfile-state if necessary to state: {}", matildaStatusCode);
                if (projectProfile != null && projectProfile.getStatus() != matildaStatusCode) {
                    LOG.debug("Persist projectProfile to change statuscode from {} to {}", projectProfile.getStatus(), matildaStatusCode);
                    // Only update Status if project profile is initialized in DB & Statuscode changed
                    projectProfile.setStatus(matildaStatusCode);
                    stateHandler.saveOrUpdateProjectProfile(projectProfile);
                } else if (isNotEmpty(projectDto.getRepoName()) && isNotEmpty(projectDto.getProjectName()) &&
                        ObjectUtils.isNotEmpty(matildaStatusCode)) {
                    stateHandler.updateStatusOfProjectProfile(projectDto.getRepoName(), projectDto.getProjectName(), matildaStatusCode);
                }
            } catch (Exception e) {
                LOG.error("Error on finally updating matildastate after crawling: ", e);
            }
        }
        return false;
    }

    private ProjectProfile handleLoadedProjectState(ProjectProfile projectProfile, ProcessingProjectDto projectDto) throws InvalidCrawlerRequestException {
        if (projectProfile == null) {
            projectProfile = new ProjectProfile(projectDto.getMatildaId(), MatildaStatusCode.RECEIVED_REQUEST_FOR_CRAWLING,
                    projectDto.getProjectName(), projectDto.getRepoName(), projectDto.getUri());
            stateHandler.saveOrUpdateProjectProfile(projectProfile);
            return projectProfile;
        } else if (isProjectNeverRequestedButAlreadyCrawled(projectProfile)) { // Project was never requested but is already crawled!
            projectProfile.setMatildaRequestId(projectDto.getMatildaId());
        } else if (!StringUtils.equals(projectProfile.getMatildaRequestId(), projectDto.getMatildaId())) {
            throw new InvalidCrawlerRequestException("Project " + projectDto.getRepoProjName() + " already crawled. Request-id "
                    + projectDto.getMatildaId() + " does not match crawled project mid: " + projectProfile.getMatildaRequestId());
        }

        // check if project is already in process and should not be recrawled yet!
        if (isProjectAlreadyInProcess(projectProfile)) {
            throw new InvalidCrawlerRequestException("Project " + projectDto.getRepoProjName() +
                    " already in crawling process. Abort request to avoid parallel crawling.");
        }

        return projectProfile;
    }

    private boolean isProjectAlreadyInProcess(ProjectProfile projectProfile) {
        return projectProfile.getStatus() != null &&
                projectProfile.getStatus().getStatusCode() >= MatildaStatusCode.STARTED_CRAWLING.getStatusCode() &&
                projectProfile.getStatus().getStatusCode() < MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode();
    }

    private boolean isProjectNeverRequestedButAlreadyCrawled(ProjectProfile projectProfile) {
        return isEmpty(projectProfile.getMatildaRequestId()) && isNotEmpty(projectProfile.getProjectName()) &&
                isNotEmpty(projectProfile.getRepositoryName());
    }

    /**
     * UPDATE := ProjectProfile != null & STATE > 141-149|>=900 & Dir OR ZIP! locally available & FLAG = TRUE
     * <p>
     * CREATE  = remotelyUpdate := false
     * UPDATE  = remotelyUpdate := true
     * RECHECK = remotelyUpdate := false
     */
    private boolean isRemoteUpdateNeeded(ProcessingProjectDto projectDto, ProjectProfile projectProfile, File cloneDir, File cloneZipDir) {
        LOG.info("Check if repo {} should be updated remotely.", projectDto.getRepoProjName());
        return projectDto.isRepoUpdateable() && projectProfile != null &&
                projectProfile.getStatus().getStatusCode() >= MatildaStatusCode.FINISHED_ANALYZING_PROJECT.getStatusCode() &&
                (cloneDir.exists() || cloneZipDir.exists());
    }

    /**
     * CREATE  = newlyCrawlProject := true
     * UPDATE  = newlyCrawlProject := false
     * RECHECK = newlyCrawlProject := false
     */
    private boolean isInitialCrawlingNeeded(ProcessingProjectDto projectDto, ProjectProfile projectProfile, File cloneDir, File cloneZipDir) {
        LOG.info("Check if repo {} should be crawled.", projectDto.getRepoProjName());

        return (projectProfile == null ||
                projectProfile.getStatus().getStatusCode() >= MatildaStatusCode.ERROR_GENERAL.getStatusCode() ||
                projectProfile.getStatus().getStatusCode() == MatildaStatusCode.RECEIVED_REQUEST_FOR_CRAWLING.getStatusCode())
                && !projectDto.isRepoUpdateable() && !cloneDir.exists() && !cloneZipDir.exists();
    }

    private boolean crawlOrUpdateProject(ProcessingProjectDto tcDto, Instant start, boolean newlyCrawlProject,
                                         boolean remotelyUpdate, File cloneDir, File cloneZipDir)
            throws InterruptedException, IOException, GitAPIException, RepositorySourceException, CrawlerException {
        LOG.info("Crawl or Update decision: newlyCrawl={}, updateRemotely={}", newlyCrawlProject, remotelyUpdate);
        String projDir = startProcessing(tcDto, newlyCrawlProject, remotelyUpdate, cloneDir, cloneZipDir);
        sleepIfNeeded(start);

        if (StringUtils.isNotEmpty(projDir)) {
            tcDto.setProjectDir(projDir);
            return true;
        }
        LOG.info("  cancel further processing... ");
        return false;
    }

    /**
     * CREATE = newlyCrawlProject := true  & remotelyUpdate := false => Crawl newly from git-host
     * UPDATE = newlyCrawlProject := false  & remotelyUpdate := true => Update local available project from git-host
     * CHECK  = newlyCrawlProject := false & remotelyUpdate := false => Recrawl available project locally only
     */
    private String startProcessing(ProcessingProjectDto tcDto, boolean newlyCrawlProject, boolean remotelyUpdate,
                                   File cloneDir, File cloneZipDir) throws IOException, GitAPIException,
            RepositorySourceException, CrawlerException {
        String projDir = "";
        if (!newlyCrawlProject && !remotelyUpdate) {
            if(matildaCrawlerRecheckLocallyToggle) {
                projDir = performLocalRecheck(tcDto); // LOCAL RECHECK
            }
        } else {
            if(matildaCrawlerCheckRemotelyToggle) {
                projDir = crawlOrRemoteUpdateProject(tcDto, newlyCrawlProject, remotelyUpdate, cloneDir, cloneZipDir); // REMOTE (RE)CHECK
            }
        }
        return projDir;
    }

    private void sleepIfNeeded(Instant start) throws InterruptedException {
        long timeElapsed = Duration.between(start, Instant.now()).toMillis();
        if (timeElapsed < 5000) {
            LOG.info("Crawling is going too fast! Let's wait for {} milliseconds.", 5000 - timeElapsed);
            Thread.sleep(5000 - timeElapsed);
        }
    }

    private String performLocalRecheck(ProcessingProjectDto tcDto) throws IOException, GitAPIException {
        LOG.info("-> CRAWLING-MODE: Repo will be rechecked locally");
        String projDir = tcDto.getTargetPath() + File.separator + tcDto.getRepoProjName();
        stateHandler.updateStatusOfProjectProfile(tcDto.getRepoName(), tcDto.getProjectName(), MatildaStatusCode.STARTED_CRAWLING_RECHECK);
        janitorService.reactivateArchivedFolderStructure(projDir);
        janitorService.cleanUpProjectInfoFromDatabases(tcDto.getRepoProjName(), tcDto.getRepoName(), tcDto.getProjectName(), tcDto.getRepoSource().toString());
        Git git = Git.open(Paths.get(tcDto.getProjectDir() + File.separator + "clone").toFile());
        genericCrawlerService.crawlLocalRepo(tcDto, git);
        return projDir;
    }

    private String crawlOrRemoteUpdateProject(ProcessingProjectDto tcDto, boolean newlyCrawlProject, boolean remotelyUpdate,
                                              File cloneDir, File cloneZipDir) throws RepositorySourceException, CrawlerException {

        if (newlyCrawlProject && !tcDto.isRepoUpdateable()) { // = NEW CRAWLING
            LOG.info("-> CRAWLING-MODE: Repo will be crawled initially - update status of project profile");
            stateHandler.updateStatusOfProjectProfile(tcDto.getRepoName(), tcDto.getProjectName(), MatildaStatusCode.STARTED_CRAWLING);
            LOG.info("                  updated status of project profile");
        } else if (!newlyCrawlProject && remotelyUpdate) { // = UPDATE CRAWLING (REMOTE)
            updateByRemoteCrawling(tcDto, cloneDir, cloneZipDir);
        }

        String projDir = switch (tcDto.getRepoSource()) {
            case github ->
                    githubCrawler.cloneRepoStructure(tcDto.getRepoProjName(), tcDto.getTargetPath(), tcDto.isJanitor(), tcDto.getSince());
            case gitlab ->
                    gitlabCrawler.cloneRepoStructure(tcDto.getRepoProjName(), tcDto.getTargetPath(), tcDto.isJanitor(), tcDto.getSince());
            case bitbucket ->
                    bitbucketCrawler.cloneRepoStructure(tcDto.getRepoProjName(), tcDto.getTargetPath(), tcDto.isJanitor(), tcDto.getSince());
            default ->
                    throw new RepositorySourceException("Unsupported repository source requested: " + tcDto.getRepoSource());
        };

        return projDir;
    }

    private void updateByRemoteCrawling(ProcessingProjectDto tcDto, File cloneDir, File cloneZipDir) throws CrawlerException {
        LOG.info("-> CRAWLING-MODE: Repo will be updated by crawling remote repository again");
        if (!cloneDir.exists() && !cloneZipDir.exists()) {
            throw new CrawlerException("No proper data available to update project remotely: " + tcDto.getRepoProjName());
        }
        stateHandler.updateStatusOfProjectProfile(tcDto.getRepoName(), tcDto.getProjectName(), MatildaStatusCode.STARTED_CRAWLING_UPDATE);
        if (!cloneDir.exists() && cloneZipDir.exists()) {
            janitorService.reactivateArchivedFolderStructure(tcDto.getTargetPath() + File.separator + tcDto.getRepoProjName());
        }
        janitorService.cleanUpProjectInfoFromDatabases(tcDto.getRepoProjName(), tcDto.getRepoName(), tcDto.getProjectName(), tcDto.getRepoSource().toString());
    }
}
