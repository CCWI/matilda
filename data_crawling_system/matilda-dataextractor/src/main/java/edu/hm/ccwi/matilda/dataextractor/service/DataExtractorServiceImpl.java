package edu.hm.ccwi.matilda.dataextractor.service;

import com.google.common.collect.Lists;
import edu.hm.ccwi.matilda.base.exception.RepositoryAlreadyExistsException;
import edu.hm.ccwi.matilda.base.exception.StateException;
import edu.hm.ccwi.matilda.base.model.enumeration.MatildaStatusCode;
import edu.hm.ccwi.matilda.base.model.enumeration.RepoSource;
import edu.hm.ccwi.matilda.base.model.state.ProjectProfile;
import edu.hm.ccwi.matilda.base.util.GitCommons;
import edu.hm.ccwi.matilda.base.util.ProgressHandler;
import edu.hm.ccwi.matilda.dataextractor.service.cleaner.ArchiverService;
import edu.hm.ccwi.matilda.dataextractor.service.reader.GradleReader;
import edu.hm.ccwi.matilda.dataextractor.service.reader.IvyReader;
import edu.hm.ccwi.matilda.dataextractor.service.reader.PomReader;
import edu.hm.ccwi.matilda.persistence.jpa.model.LibraryEntity;
import edu.hm.ccwi.matilda.persistence.mongo.model.*;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import edu.hm.ccwi.matilda.persistence.mongo.util.CrawledRevisionHelper;
import edu.hm.ccwi.matilda.persistence.mongo.util.RevisionTimeSorter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonSerializationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Extract data from project including dependency-details and documentation.
 *
 * @author Max.Auch
 */
@Service
public class DataExtractorServiceImpl implements DataExtractorService {

    private static final Logger LOG = LoggerFactory.getLogger(DataExtractorServiceImpl.class);

    private static int extractorCounter = 0;

    private final InfoRetriever infoRetriever;
    private final PomReader pomReader;
    private final GradleReader gradleReader;
    private final IvyReader ivyReader;
    private final CrawledSoftwareRepository swRepo;
    private final ArchiverService archiverService;
    private final ExtractorStateHandler stateHandler;
    private final StatisticExtractorService statisticExtractorService;
    private final UtilService utilService;
    private final DependencyLibraryHandler libHandler;

    @Value("${matilda.dataextractor.ignore.existingRepos}")
    private boolean ignoreExistingRepos;

    @Value("${matilda.dataextractor.project.revision.maximum}")
    private int maximumRevisionsInProject;

    @Value("${matilda.dataextractor.project.dependency.maximum}")
    private int maximumDependenciesInProjects;

    public DataExtractorServiceImpl(InfoRetriever infoRetriever, PomReader pomReader, GradleReader gradleReader,
                                    IvyReader ivyReader, CrawledSoftwareRepository swRepo, ArchiverService archiverService,
                                    ExtractorStateHandler stateHandler, StatisticExtractorService statisticExtractorService,
                                    UtilService utilService, DependencyLibraryHandler libHandler) {
        this.infoRetriever = infoRetriever;
        this.pomReader = pomReader;
        this.gradleReader = gradleReader;
        this.ivyReader = ivyReader;
        this.swRepo = swRepo;
        this.archiverService = archiverService;
        this.stateHandler = stateHandler;
        this.statisticExtractorService = statisticExtractorService;
        this.utilService = utilService;
        this.libHandler = libHandler;
    }

    public String extractorProcessor(String repoName, String projectName, String projDir, RepoSource source) {
        LOG.info("");
        LOG.info("");
        LOG.info("___________________________________________________________________________________________________");
        LOG.info("________[{}]__________ START DATA-EXTRACTING {}/{} FROM {} __________________", ++extractorCounter, repoName, projectName, source);

        boolean datExtractionSuccessful = false;
        MatildaStatusCode matildaStatusCode = null;
        CrawledRepository crawledRepository = new CrawledRepository(projectName, repoName, source, projDir);

        // load data from db
        ProjectProfile projectProfile = stateHandler.getProjectProfileByRepoProj(repoName, projectName);

        // START data extraction
        LOG.info("  Start extracting clone: {}", projDir + File.separator + "clone");
        try (Git git = Git.init().setDirectory(new File(projDir + File.separator + "clone")).call()) {
            matildaStatusCode = updateProjectProfileState(projectProfile);
            git.getRepository();
            CrawledRepository cr = utilService.getCrawledRepositoryFromMongoDB(crawledRepository);
            if (cr != null && cr.getRepositoryName() != null && cr.getProjectName() != null && cr.getDirectoryPath() != null) {
                if (ignoreExistingRepos) {
                    throw new RepositoryAlreadyExistsException("");
                } else {
                    crawledRepository = cr;
                }
            }

            // Create Revision if not exists
            List<CrawledRevision> revList = new ArrayList<>();
            extractRevisionData(projDir, crawledRepository, git, revList);

            if (revList.isEmpty()) {
                throw new NoSuchElementException("List of revisions is empty");
            }

            // Create statistic before cleaning, adjusting and enriching repo
            CrawledStatistic crawledStatistic = statisticExtractorService.createCrawledStatistic(repoName, projectName, source, git, revList);

            // Modify revisions of repository by cleaning, enrichment and adjustment
            revList = cleanupAdjustEnrichRepository(crawledRepository, git, revList);

            // Update statistic after cleaning, adjusting and enriching repo
            statisticExtractorService.updateCrawledStatistic(revList, crawledStatistic);

            //Extract documentation for left revisions and save revisions + docs to MongoDB
            extractAndProcessDocumentation(crawledRepository, revList);

            // Save repository to MongoDB
            swRepo.save(crawledRepository);
            LOG.info("SAVED extracted repository");
            statisticExtractorService.saveStatistic(crawledStatistic);

            // Cleanup Commits-Folder and archive project into zip-file
            archiverService.archiveCrawledProjectDirectory(projDir);

            datExtractionSuccessful = true;
            matildaStatusCode = MatildaStatusCode.FINISHED_DATA_EXTRACTION;

        } catch (RepositoryAlreadyExistsException e) {
            return null; // stop extraction of repository
        } catch (BsonSerializationException e) {
            LOG.error("MongoDB Bson serialization failed for crawledRepo {}/{}: {}",
                    crawledRepository.getRepositoryName(), crawledRepository.getProjectName(), e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (IOException e) {
            LOG.error("Creating directory failed: {}", e.getMessage());
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (NullPointerException e) {
            LOG.error("Nullpointer occurred while extracting Data: ", e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (GitAPIException e) {
            LOG.error("Forking on Git-Repo: ", e);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (NoSuchElementException e) {
            LOG.error("Error by working on Git-Repo: {}", e.getMessage());
            LOG.info("Delete crawled Repository from database...");
            swRepo.delete(crawledRepository);
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } catch (InterruptedException e) {
            LOG.error("An interrupt exception occurred - Thread will be stopped. Exception: {}", e.getMessage());
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("A general exception occurred: {}", e.getMessage());
            matildaStatusCode = MatildaStatusCode.ERROR_GENERAL;
        } finally {
            LOG.info("Set final result state of extraction for further processing / abortion");
            if (projectProfile != null) { // Only update Status if project profile is initialized in DB
                projectProfile.setStatus(matildaStatusCode);
                stateHandler.saveOrUpdateProjectProfile(projectProfile);
                if (isCleanupOfProjectNeeded(projDir)) {
                    // Cleanup Commits-Folder and archive project into zip-file
                    archiverService.archiveCrawledProjectDirectory(projDir);
                }
            }
        }

        LOG.info("____{}______________________________________________________________", datExtractionSuccessful ?
                "SUCCESSFUL DATA-EXTRACTION" : "FAILURE ON DATA-EXTRACTION");
        LOG.info("");
        LOG.info("");
        return datExtractionSuccessful ? crawledRepository.getId() : null; // null causes no further queuing/processing
    }

    private boolean isCleanupOfProjectNeeded(String projDir) {
        return new File(projDir + File.separator + "commits").exists() ||
                new File(projDir + File.separator + "clone").exists();
    }

    private MatildaStatusCode updateProjectProfileState(ProjectProfile projectProfile) throws StateException {
        if (projectProfile != null) {
            projectProfile.setStatus(MatildaStatusCode.RECEIVED_REQUEST_FOR_DATA_EXTRACTION);
            stateHandler.saveOrUpdateProjectProfile(projectProfile);
            return MatildaStatusCode.RECEIVED_REQUEST_FOR_DATA_EXTRACTION;
        } else {
            throw new StateException("No project profile found for received dataextraction request");
        }
    }

    private List<CrawledRevision> cleanupAdjustEnrichRepository(CrawledRepository crawledRepository,
                                                                Git git, List<CrawledRevision> revList) throws Exception {
        long start1 = System.nanoTime();  //----------------------------------------------------------------------------
        LOG.info("    ### Start \"cleanup adjust enrich\" of Repository");
        revList.sort(new RevisionTimeSorter()); //sort revList by dateOfCommit
        debugLoggingOfRevisionList(revList, "      Revisions before first filtering: ");
        revList = cleanupDependencyMissingCrawledRevisions(revList);
        revList = infoRetriever.filterFirstPartyProjectDependencies(revList); // Filter first-party dep. by GA.
        revList = cleanupDependencyMissingCrawledRevisions(revList);
        revList = infoRetriever.filterUnresolvedProjectDependencies(revList); // Filter placeholders in GA
        debugLoggingOfRevisionList(revList, "      Revisions between first filtering #3: ");
        revList = cleanupDependencyMissingCrawledRevisions(revList);
        debugLoggingOfRevisionList(revList, "      Revision after first filtering: ");

        // Prevent endless processing by limiting size at this point!
        //TODO OPTIMIZE THE SUBSEQUENT CODE (gatherAdditionalInfoByTreewalkComparisonAndAdjustRevlist()) to crawl big projects as well.

        if (CollectionUtils.isEmpty(revList)) {
            LOG.info("      Revlist is empty before filtering..");
            throw new NoSuchElementException("      CrawledRepository " + crawledRepository.getId() +
                    " will be removed because of empty revList after cleanup!");
        }

        if (revList.size() > maximumRevisionsInProject) {
            LOG.error("      Project is too big for Dataextractor: {}", revList.size());
            throw new Exception("Project too big for Dataextractor: " + revList.size() + " -> " +
                    crawledRepository.getRepositoryName() + "/" + crawledRepository.getProjectName() + " ignored");
        }

        LOG.info("      Assign branch names to each revision..");
        revList = addBranchNamesToEachRevision(git, revList);

        LOG.info("      Assign categories and tags to revisions: start assign categories and tags for {} revisions", revList.size());
        assignCategoriesAndTagsToRevisions(revList);
        LOG.info("        --> took {} seconds", ((System.nanoTime() - start1) / 1000000000));
        long start2 = System.nanoTime();

        LOG.info("      Gather Info by treewalk: start assign categories and tags for {} revisions", revList.size());
        gatherAdditionalInfoByTreewalkComparisonAndAdjustment(revList);
        LOG.info("        --> took {} seconds", ((System.nanoTime() - start2) / 1000000000));

        if (revList.isEmpty()) {
            LOG.info("      Revlist is empty after final filtering..");
            throw new NoSuchElementException("CrawledRepository " + crawledRepository.getId() +
                    " will be removed because of empty revList after cleanup!");
        } else {
            debugLoggingOfRevisionList(revList, "        Rev after final filtering: ");
        }

        LOG.info("    ### \"cleanup adjust enrich\" - gather/filter Info took overall {} seconds.", (System.nanoTime() - start1) / 1000000000);
        return revList;
    }

    double calculateMeanSubseqRevisionSize(List<CrawledRevision> revList) {
        return revList.stream()
                .mapToDouble(rev -> rev.getSubsequentCommitIdList().size())
                .average()
                .orElse(0.0);
    }

    private void debugLoggingOfRevisionList(List<CrawledRevision> revList, String s) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("  ##########################");
            for (CrawledRevision crawledRevision : revList) {
                if (CollectionUtils.isNotEmpty(crawledRevision.getSubsequentCommitIdList())) {
                    for (String subCommitId : crawledRevision.getSubsequentCommitIdList()) {
                        LOG.debug("{} {} --> {}", s, crawledRevision.getCommitId(), subCommitId);
                    }
                } else {
                    LOG.debug(s + crawledRevision.getCommitId());
                }
            }
            LOG.debug("  ##########################");
        }
        LOG.info("Summary: {} {}", s, revList.size());
    }

    private void extractAndProcessDocumentation(CrawledRepository crawledRepository, List<CrawledRevision> revList) throws Exception {
        long start = System.nanoTime(); //----------------------------------------------------------------
        LOG.info("    ### Start extraction and saving of documents"); //-------------
        for (CrawledRevision rev : revList) {
            CrawledDocumentation crawledDocumentation = infoRetriever.extractDocumentation(rev.getCommitId(), rev.getDirectoryPath());
            if (crawledDocumentation != null && crawledDocumentation.getLanguage() != null && crawledDocumentation.getLanguage().contains("en")) {
                utilService.saveRevResultsToDB(crawledRepository.getRepositoryName() + "/" + crawledRepository.getProjectName(),
                        rev, crawledDocumentation);
            }
            crawledRepository.getRevisionCommitIdList().add(rev.getCommitId());
        }
        LOG.info("    ### Extract docs + save took {} seconds.", (System.nanoTime() - start) / 1000000000); //-------------
    }

    private void extractRevisionData(String projDir, CrawledRepository crawledRepository, Git git, List<CrawledRevision> revList) throws Exception {
        long start = System.nanoTime();  //--------------------------------------------------------------------------------
        LOG.info("    ### Start extracting revision data...");
        File commitDirs = new File(projDir + File.separator + "commits");
        if (utilService.directoryValid(commitDirs) && ArrayUtils.isNotEmpty(commitDirs.listFiles())) {
            for (File commitDir : commitDirs.listFiles()) {
                int dependencyCounter = 0;
                if (!utilService.directoryValid(commitDir)) {
                    throw new Exception("Revision directory seems to be null; commitDir: " + commitDir);
                }
                if (!commitDir.getName().contains("_")) {
                    throw new Exception("Revision directory does not meet the required naming: " + commitDir.getName());
                }
                List<CrawledProject> crawledProjectList = extractProjectDependencies(crawledRepository.getRepositoryName(),
                        crawledRepository.getProjectName(), commitDir.getPath());
                if (CollectionUtils.isNotEmpty(crawledProjectList) && infoRetriever.isMDInProject(commitDir.getPath())) {
                    for (CrawledProject proj : crawledProjectList) {
                        dependencyCounter += proj.getDependencyList().size();
                    }
                    String[] dirNameParts = commitDir.getName().split("_"); // datetime, (_), commitId
                    if (revList.stream().noneMatch(item -> StringUtils.equals(dirNameParts[1], item.getCommitId()))) {
                        LocalDateTime dateOfCommit = GitCommons.convertEpochSecondToLocalDate(dirNameParts[0]);
                        revList.add(new CrawledRevision(dirNameParts[1], utilService.getCommitType(git, dirNameParts[1]),
                                commitDir.getPath(), dateOfCommit, LocalDateTime.now(), crawledProjectList));
                    }
                }
                if (dependencyCounter > maximumDependenciesInProjects) {
                    throw new Exception("Found more than 1000 Dependencies in a single revision. Project will be ignored!");
                }
            }
        }

        LOG.info("    ### Create Revision took {} seconds.", (System.nanoTime() - start) / 1000000000); //-------------
    }

    /**
     * Add Branch-Name to Revisions.
     */
    private List<CrawledRevision> addBranchNamesToEachRevision(Git git, List<CrawledRevision> revList) throws GitAPIException, IOException {
        List<Ref> branchRef = git.branchList().call();
        LOG.info("    Adding branch-names (Amount: {}) to each revision (Amount: {})", branchRef.size(), revList.size());

        // TRY CREATE MAP:
        List<Map.Entry<String, String>> entryList = new ArrayList<>();
        for (Ref branch : branchRef) {
            Iterable<RevCommit> commits = git.log().add(git.getRepository().resolve(branch.getName())).call();
            if (commits != null) {
                for (RevCommit commit : Lists.newArrayList(commits.iterator())) {
                    if (branch.getName() != null) {
                        entryList.add(Map.entry(commit.getName(), branch.getName()));
                    }
                }
            }
        }

        LOG.info("        Created map of commits in branches - size: {}", entryList.size());
        ProgressHandler progressHandler = new ProgressHandler(revList.size());
        int progress = progressHandler.incrementProgress();
        for (CrawledRevision rev : revList) {
            entryList.stream().filter(entry -> rev.getCommitId().equalsIgnoreCase(entry.getKey())).forEach(entry -> rev.addBranch(entry.getValue()));
            if (progress % 100 == 0) {
                LOG.info("        Progress adding revisions: [{}/{}]", progress, progressHandler.getMaxAmount());
            }
        }

        return revList;
    }

    private void assignCategoriesAndTagsToRevisions(List<CrawledRevision> revList) {
        int progress = 0;
        for (CrawledRevision rev : revList) {
            progress++;
            for (CrawledProject proj : rev.getProjectList()) {
                for (CrawledDependency dep : proj.getDependencyList()) {
                    libHandler.assignCategoriesAndTags(dep);
                }
            }
            if (progress % 500 == 0) {
                LOG.info("-------Progress on assigning categories/tags to revs: {}/{}", progress, revList.size());
            }
        }
    }

    /**
     * Remove crawledProject if no dependencies available.
     * Check if crawledProjects left in crawledRevision -> if not, delete crawledRevision.
     */
    private List<CrawledRevision> cleanupDependencyMissingCrawledRevisions(List<CrawledRevision> revList) {
        LOG.info("    Start cleanup dependencies for {} revisions", revList.size());
        for (ListIterator<CrawledRevision> iter = revList.listIterator(); iter.hasNext(); ) {
            CrawledRevision rev = iter.next();
            LOG.trace("      cleanupDependencyMissingCrawledRevisions: {}", rev);
            if (rev != null) {
                rev.getProjectList().removeIf(proj -> {
                    boolean isUnresolved = proj.getDependencyList().isEmpty();
                    if (isUnresolved) {LOG.trace("      -> Dependency-List for Project {}:{} is empty", proj.getProjectGroup(), proj.getProjectArtifact());}
                    return isUnresolved;
                });
                rev.getProjectList().removeIf(Objects::isNull);
                if (rev.getProjectList().isEmpty()) {
                    LOG.debug("      -> project-list of rev is null and rev will be removed!");
                    iter.remove();
                }
            } else {
                LOG.debug("      -> rev is null and will be removed!");
                iter.remove();
            }
        }
        revList.removeIf(Objects::isNull);
        return revList;
    }

    /**
     * Compare from the oldest Revision to the youngest.
     */
    void gatherAdditionalInfoByTreewalkComparisonAndAdjustment(List<CrawledRevision> revisionList) throws Exception {
        revisionList.removeIf(Objects::isNull);
        LOG.info("    Start gather additional info by TreewalkComparison and adjust revlist for {} revisions.", revisionList.size());

        if (revisionList.size() > 1) {
            LOG.info("      Start creating revision tree");
            createRevisionTreeFromCurrentRevisionAsRoot(revisionList);

            debugLoggingOfRevisionList(revisionList, "  Revision after creating trees: ");
            Map<String, CrawledRevision> removableMarkedRevisions = retrieveRemovableMarkedRevisions(revisionList);

            debugLoggingOfRevisionList(revisionList, "  Revision before cleanup: ");
            utilService.cleanUpAllReferences(revisionList, removableMarkedRevisions); // clean up all references
            debugLoggingOfRevisionList(revisionList, "  Revision after cleanup: ");
        } else {
            throw new Exception("    Rev list emtpy at this point -> project should not be saved and instead ignored!");
        }
    }

    private Map<String, CrawledRevision> retrieveRemovableMarkedRevisions(List<CrawledRevision> revisionList) {
        LOG.info("    Start getting as removable marked revisions on list of size {}", revisionList.size());
        ProgressHandler progressHandler = new ProgressHandler(revisionList.size());
        Map<String, CrawledRevision> overallRemovableMarkedRevisions = new HashMap<>();
        int logProgressSteps = 10;

        for (int revisionCounter = 0; revisionCounter < revisionList.size(); revisionCounter++) {
            long start = System.nanoTime(); //----------------------------------------------------------------
            int progress = progressHandler.incrementProgress();
            if (overallRemovableMarkedRevisions.containsKey(revisionList.get(revisionCounter).getCommitId())) {
                if (progress % logProgressSteps == 1 || progress == 1 || progress == revisionList.size()) {
                    LOG.debug("        Progress find removable revs: [{}/{}/iter:{}] -> Skip this iter because already removable",
                            revisionCounter, progressHandler.getMaxAmount(), progress);
                }
                continue;
            }
            DependencyDetailExtractor dde = createDependencyDetailExtractor(revisionList, revisionCounter);
            Set<CrawledRevision> removableRevisions = dde.findRemovableRevisionsAndPrepareDeletion(overallRemovableMarkedRevisions);

            if (CollectionUtils.isEmpty(removableRevisions) || isRemovableRevisionSetAlreadyKnownCompletely(removableRevisions, overallRemovableMarkedRevisions)) {
                if (progress % logProgressSteps == 1 || progress == 1 || progress == revisionList.size()) {
                    LOG.debug("        Progress find removable revs: [{}/{}/iter:{}] -> Skip this iter because already removable",
                            revisionCounter, progressHandler.getMaxAmount(), progress);
                }
                continue;
            }

            for (CrawledRevision removableRevision : removableRevisions) {
                // If there are others, just remove subseq-reference from rev -> ensure, that other revisions
                //          are checked on updated revlist. So subseq-reference should be decreased by 1.
                revisionList.get(revisionCounter).getSubsequentCommitIdList().remove(removableRevision.getCommitId());
                if (isRevisionRemovable(revisionList, overallRemovableMarkedRevisions, removableRevision)) {
                    // If there are no others, remove subseq-reference from rev and remove subseq-rev
                    overallRemovableMarkedRevisions.put(removableRevision.getCommitId(), removableRevision);
                } else {
                    LOG.trace("      Removable marked revision is still linked in another commit -> cannot be removed for now, " +
                            "but it's subseq-reference is removed");
                }
            }

            revisionCounter--; // reprocess revision, since it might contain new subseq-revisions (from removable marked revisions)

            if (progress % logProgressSteps == 1 || progress == 1 || progress == revisionList.size()) {
                LOG.debug("        Progress find removable revs: [{}/{}/iter:{}] in {}s - Found {} new rem. revs => Total: {}", revisionCounter,
                        progressHandler.getMaxAmount(), progress, ((System.nanoTime() - start) / 1000000000), removableRevisions.size(),
                        overallRemovableMarkedRevisions.size());
                LOG.debug("          --> currentRev: {} has now subseq revisions: {}", dde.getCurrentRevision().getCommitId(),
                        dde.getCurrentRevision().getSubsequentCommitIdList().size());
                if(dde.tmpDebugListOfAddedSubsequences.size() > 10 || dde.tmpDebugListOfRemovedSubsequences.size() > 10) {
                    LOG.debug("          --> added subseqRev: {}", dde.tmpDebugListOfAddedSubsequences.size());
                    LOG.debug("          --> removed subseqRev: {}", dde.tmpDebugListOfRemovedSubsequences.size());
                } else {
                    LOG.debug("          --> added subseqRev: {}", dde.tmpDebugListOfAddedSubsequences);
                    LOG.debug("          --> removed subseqRev: {}", dde.tmpDebugListOfRemovedSubsequences);
                }
            }
        }

        return overallRemovableMarkedRevisions;
    }

    private boolean isRemovableRevisionSetAlreadyKnownCompletely(Set<CrawledRevision> removableRevisions, Map<String, CrawledRevision> removableMarkedRevisions) {
        for (CrawledRevision removableRevision : removableRevisions) {
            if(!removableMarkedRevisions.containsKey(removableRevision.getCommitId())) {
                return false;
            }
        }
        return true;
    }

    private boolean isRevisionRemovable(List<CrawledRevision> revisionList, Map<String, CrawledRevision> removableMarkedRevisions,
                                        CrawledRevision removableRevision) {
        return removableRevision.getCommitId() != null
                && !isLinkedByNonRemovableRevision(revisionList, removableRevision.getCommitId(), removableMarkedRevisions)
                && !removableMarkedRevisions.containsKey(removableRevision.getCommitId());
    }

    private DependencyDetailExtractor createDependencyDetailExtractor(List<CrawledRevision> revisionList, int i) {
        List<CrawledRevision> overallSubsequentRevisions = new ArrayList<>();
        List<CrawledRevision> olderRevisionList = new ArrayList<>();
        if (i + 1 <= revisionList.size()) {
            try {
                overallSubsequentRevisions = revisionList.subList(i + 1, revisionList.size());
                if (i > 0) {
                    olderRevisionList = revisionList.subList(0, i);
                } else {
                    olderRevisionList = revisionList.subList(0, 0);
                }
            } catch (IllegalArgumentException e) {
                LOG.error("    Sublist in gatherAdditionalInfoByTreewalkComparison()-method could not be created: {}", e.getMessage());
            }
        }
        CrawledRevision crawledRevision = revisionList.get(i);
        return new DependencyDetailExtractor(crawledRevision, overallSubsequentRevisions, olderRevisionList);
    }

    private void createRevisionTreeFromCurrentRevisionAsRoot(List<CrawledRevision> revisionList) {
        for (int revisionCounter = 0; revisionCounter < revisionList.size(); revisionCounter++) {
            List<CrawledRevision> overallSubsequentRevisions = new ArrayList<>();
            if (revisionCounter + 1 <= revisionList.size()) {
                overallSubsequentRevisions = revisionList.subList(revisionCounter + 1, revisionList.size());
            }

            CrawledRevision currentRevision = revisionList.get(revisionCounter);
            if (currentRevision == null || CollectionUtils.isEmpty(currentRevision.getProjectList())) {
                continue;
            }

            for (CrawledRevision subsequentRevision : overallSubsequentRevisions) {
                if (isFollowupRevisionOnSameBranch(currentRevision, subsequentRevision) &&
                        isSubsequentRevision(currentRevision, subsequentRevision, overallSubsequentRevisions)) {
                    currentRevision.addSubsequentRevision(subsequentRevision.getCommitId());
                }
            }
        }
    }

    /**
     * TODO descibe LOGIC!!
     */
    private boolean isSubsequentRevision(CrawledRevision currentRevision, CrawledRevision subsequentRevision,
                                         List<CrawledRevision> overallSubsequentRevisions) {
        for(String listedSubSeqCommitId : currentRevision.getSubsequentCommitIdList()) {
            CrawledRevision listedSubseqRevision = CrawledRevisionHelper.findRevisionById(overallSubsequentRevisions, listedSubSeqCommitId);
            if(listedSubseqRevision != null && listedSubseqRevision.getBranchList() != null &&
                    listedSubseqRevision.getBranchList().containsAll(subsequentRevision.getBranchList())) {
                return false;
            }
        }
        return true;
    }

    private boolean isFollowupRevisionOnSameBranch(CrawledRevision currentRev, CrawledRevision subsequentRev) {
        if (!currentRev.getBranchList().containsAll(subsequentRev.getBranchList())) {
            return false;
        }

        return true;
    }

    private boolean isLinkedByNonRemovableRevision(List<CrawledRevision> revisionList, String commitId,
                                                   Map<String, CrawledRevision> removableMarkedRevisions) {
        if (CollectionUtils.isNotEmpty(revisionList)) {
            return revisionList.stream()
                    .anyMatch(crawledRevision ->
                            crawledRevision != null &&
                            crawledRevision.getSubsequentCommitIdList() != null &&
                            crawledRevision.getSubsequentCommitIdList().contains(commitId) && // check if commitId is still linked
                            !removableMarkedRevisions.containsKey(crawledRevision.getCommitId()));  // check if this link is not
                                                                                                    // deprecated because the linking
                                                                                                    // revision is also removable
        }
        return false;
    }

    /**
     * Extract DependencyManagementFile-Info.
     *
     * @param crawledRepositoryName - repository name
     * @param crawledProjectName    - project name
     * @param revisionDir           - directory of revision
     * @return list of extracted projects
     * @throws Exception - any kind of exception
     */
    private List<CrawledProject> extractProjectDependencies(String crawledRepositoryName, String crawledProjectName,
                                                            String revisionDir) throws Exception {
        List<File> pomFilesInProject = pomReader.findAllRelatedFilesInProject(revisionDir);
        if (CollectionUtils.isNotEmpty(pomFilesInProject)) { // POM
            LOG.debug("    Found {} pom-project(s) to extract dependencies", pomFilesInProject.size());
            return pomReader.extractProjectDependency(pomFilesInProject, revisionDir, crawledRepositoryName, crawledProjectName);
        }

        List<File> gradleFilesInProject = gradleReader.findAllRelatedFilesInProject(revisionDir);
        if (CollectionUtils.isNotEmpty(gradleFilesInProject)) { // GRADLE
            LOG.debug("    Found {} gradle-project(s) to extract dependencies", gradleReader.findAllRelatedFilesInProject(revisionDir).size());
            return gradleReader.extractProjectDependency(gradleFilesInProject, revisionDir, crawledRepositoryName, crawledProjectName);
        }

        List<File> ivyFilesInProject = ivyReader.findAllRelatedFilesInProject(revisionDir);
        if (CollectionUtils.isNotEmpty(ivyFilesInProject)) { // IVY
            LOG.debug("    Found {} ivy-project(s) to extract dependencies", ivyReader.findAllRelatedFilesInProject(revisionDir).size());
            return ivyReader.extractProjectDependency(ivyFilesInProject, revisionDir, crawledRepositoryName, crawledProjectName);
        }

        return List.of();
    }
}