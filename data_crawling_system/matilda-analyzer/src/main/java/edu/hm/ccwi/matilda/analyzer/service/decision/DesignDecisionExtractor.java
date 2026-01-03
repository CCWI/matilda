package edu.hm.ccwi.matilda.analyzer.service.decision;

import edu.hm.ccwi.matilda.base.util.ProgressHandler;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.jpa.model.ExtractedDesignDecisionEntity;
import javassist.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;

@Service
public class DesignDecisionExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DesignDecisionExtractor.class);

    final CrawledRevisionRepository mongoRevRepo;

    public DesignDecisionExtractor(CrawledRevisionRepository mongoRevRepo) {
        this.mongoRevRepo = mongoRevRepo;
    }

    Set<ExtractedDesignDecisionEntity> analyzeRepositoryOnDD(CrawledRepository crawledRepository,
                                                             List<CrawledRevision> revisionListOfRepository) throws NotFoundException {
        // A) Find root and orphan commits
        Set<String> rootCommitOfRepository = findOrphanRootCommits(revisionListOfRepository);
        if (CollectionUtils.isEmpty(rootCommitOfRepository)) {
            LOG.error("No root commits and no orphan commits found in Repository {}/{}. Skip and continue.",
                    crawledRepository.getProjectName(), crawledRepository.getRepositoryName());
            throw new NotFoundException("No root/orphan commit found in crawled Repository");
        }

        // B) Find decisions per commit ...
        Set<ExtractedDesignDecisionEntity> overallExtractedDecisionSet = new HashSet<>();
        ProgressHandler progressHandler = new ProgressHandler(revisionListOfRepository.size());
        for (CrawledRevision crawledRevision : revisionListOfRepository) {
            long startTime = System.currentTimeMillis();

            overallExtractedDecisionSet.addAll(extractDesignDecisionsForCommit(crawledRepository,
                    revisionListOfRepository, crawledRevision, rootCommitOfRepository));

            int progress = progressHandler.incrementProgress();
            if (progress % 50 == 0 || progress == 1 || progress == 2 || progress == 3 || progress == revisionListOfRepository.size()) {
                LOG.debug("      Progress analyzing: [{}/{}] - Time: {} sec and found {} decisions", progress,
                        progressHandler.getMaxAmount(), ((System.currentTimeMillis() - startTime) / 1000.0), overallExtractedDecisionSet.size());
            }
        }

        // C) Filter Decisions to avoid "back and forth"
        Set<ExtractedDesignDecisionEntity> overallFilteredDecisionSet = new HashSet<>();
        filterDecisionsOnBackAndForth(overallExtractedDecisionSet, overallFilteredDecisionSet);

        return overallFilteredDecisionSet;
    }

    private void filterDecisionsOnBackAndForth(Set<ExtractedDesignDecisionEntity> overallExtractedDecisionSet, Set<ExtractedDesignDecisionEntity> overallFilteredDecisionSet) {
        Set<ExtractedDesignDecisionEntity> lastDecisionOfBackAndForthRevisions = new HashSet<>();
        for (ExtractedDesignDecisionEntity extractedDesignDecisionEntity : overallExtractedDecisionSet) {
            if (!isRevertedDecision(overallExtractedDecisionSet, extractedDesignDecisionEntity) ||
                    lastDecisionOfBackAndForthRevisions.contains(extractedDesignDecisionEntity)) {
                overallFilteredDecisionSet.add(extractedDesignDecisionEntity);
            } else {
                // FIND LAST REVISION OF THE BACK-AND-FORTH-PROCEDURE AND ADD TO A EXCEPTIONLIST TO ADD THOSE TO overallFilteredDecisionSet
                // -> by doing so, the final merge-decision or current state is found!
                ExtractedDesignDecisionEntity lastMergedDD = null;
                for (ExtractedDesignDecisionEntity dd : overallExtractedDecisionSet) {
                    if(isCurrentlyLatestRevisionOfBackAndForthDecisionSwitch(extractedDesignDecisionEntity, lastMergedDD, dd)) {
                            lastMergedDD = dd;
                    }
                }

                if(lastMergedDD != null) { // should not be null -> if so, then just ignore and go on
                    // add to overallFilteredDecisionSet if it is the last commit of backand forth else save in
                    // lastDecisionOfBackAndForthRevisions for further iteration
                    if(extractedDesignDecisionEntity.equals(lastMergedDD)) {
                        overallFilteredDecisionSet.add(extractedDesignDecisionEntity);
                    } else {
                        lastDecisionOfBackAndForthRevisions.add(lastMergedDD);
                    }
                }
            }
        }
    }

    private boolean isCurrentlyLatestRevisionOfBackAndForthDecisionSwitch(ExtractedDesignDecisionEntity extractedDesignDecisionEntity, ExtractedDesignDecisionEntity lastMergedDD, ExtractedDesignDecisionEntity dd) {
        return isPartOfBackAndForthDecisionSwitch(extractedDesignDecisionEntity, dd) &&
                (lastMergedDD == null || lastMergedDD.getDecisionCommitTime().isBefore(dd.getDecisionCommitTime()));
    }

    private boolean isPartOfBackAndForthDecisionSwitch(ExtractedDesignDecisionEntity extractedDesignDecisionEntity, ExtractedDesignDecisionEntity dd) {
        return (StringUtils.equals(dd.getInitial(), extractedDesignDecisionEntity.getInitial()) &&
            StringUtils.equals(dd.getTarget(), extractedDesignDecisionEntity.getTarget()) &&
            dd.getDecisionCommitTime().isAfter(extractedDesignDecisionEntity.getDecisionCommitTime())) ||
           (StringUtils.equals(dd.getInitial(), extractedDesignDecisionEntity.getTarget()) &&
            StringUtils.equals(dd.getTarget(), extractedDesignDecisionEntity.getInitial()) &&
            dd.getDecisionCommitTime().isAfter(extractedDesignDecisionEntity.getDecisionCommitTime()));
    }

    private Set<ExtractedDesignDecisionEntity> extractDesignDecisionsForCommit(CrawledRepository crawledRepository,
                                                                               List<CrawledRevision> revisionListOfRepository,
                                                                               CrawledRevision crawledRevision,
                                                                               Set<String> rootCommitOfRepository) {
        Set<ExtractedDesignDecisionEntity> extractedDecisionCollection = new HashSet<>();
        Map<String, ExtractedDesignDecisionEntity> extractedDesignDecisionMap = new HashMap<>();

        if (crawledRevision.getSubsequentCommitIdList().size() > 200) {
            return extractedDecisionCollection;
        }

        // 1) Handle orphan commits ( = root-commits without subsequent commits)
        if (rootCommitOfRepository.contains(crawledRevision.getCommitId())) {
            extractedDecisionCollection.addAll(extractDDForOrphanCommits(crawledRepository, crawledRevision, extractedDecisionCollection));
        }

        // 2) Extract decisions made by subsequent commits in contrast to the crawled commit
        if (CollectionUtils.isNotEmpty(crawledRevision.getSubsequentCommitIdList())) {
            Set<CrawledDependency> dependencySet = SetUtils.orderedSet(getConsolidatedDependencySet(crawledRevision));

            LOG.debug("        --> orphans:{}, subseq-list:{}, dependency-set:{}", extractedDecisionCollection.size(),
                    crawledRevision.getSubsequentCommitIdList().size(), dependencySet.size());

            for (String subsequentCommitId : crawledRevision.getSubsequentCommitIdList()) {
                CrawledRevision subseqCrawledRevision;
                try {
                    subseqCrawledRevision = getRevisionByCommitId(revisionListOfRepository, subsequentCommitId);
                } catch (NotFoundException e) {
                    LOG.error("Revision {} link from parent revision {} not found. Skip and continue with next... :: {}",
                            subsequentCommitId, crawledRevision.getCommitId(), e.getMessage());
                    continue;
                }

                if (CollectionUtils.isEmpty(crawledRevision.getProjectList()) || CollectionUtils.isEmpty(subseqCrawledRevision.getProjectList())) {
                    continue;
                }

                // 3) Get consolidated distinct dependency lists
                Set<CrawledDependency> subseqDepSet = SetUtils.orderedSet(getConsolidatedDependencySet(subseqCrawledRevision));

                // 4) Check if there are differences between sets (necessary since projects consolidated)
                if (!SetUtils.isEqualSet(dependencySet, subseqDepSet)) {
                    Set<ExtractedDesignDecisionEntity> extractedDecisions = new HashSet<>();

                    // 5) Check for removed dependencies and their replacement
                    findRemovedAndReplacedDependencies(crawledRepository, crawledRevision, subseqCrawledRevision, dependencySet,
                            revisionListOfRepository, subseqDepSet, extractedDecisions, extractedDecisionCollection);

                    // 6) Check for newly added dependencies ( = check ONLY for subsequent newly added dependencies)
                    findNewlyAddedDependencies(crawledRepository, crawledRevision, subsequentCommitId,
                            subseqCrawledRevision, subseqDepSet, extractedDecisions, extractedDecisionCollection);

                    // 7) Collect decisions
                    extractedDecisionCollection.addAll(extractedDecisions);

                    // 8) add decisions to MAP for tracing
                    for (ExtractedDesignDecisionEntity extractedDecision : extractedDecisions) {
                        extractedDesignDecisionMap.put(crawledRevision.getCommitId().substring(0, 10) + " and " +
                                subseqCrawledRevision.getCommitId().substring(0, 10), extractedDecision);
                    }
                }
            }
        }

        if (LOG.isTraceEnabled()) {
            printExtractedDecisions(extractedDesignDecisionMap);
        }


        return extractedDecisionCollection;
    }

    private void printExtractedDecisions(Map<String, ExtractedDesignDecisionEntity> extractedDesignDecisionMap) {
        extractedDesignDecisionMap.forEach((key, value) -> {
            LOG.trace("#### Found Design Decisions between {}:", key);
            LOG.trace("####    " + value.getInitial() + " --> " + value.getTarget());
        });
    }

    /**
     * Einsiedler-Commits erst einmal gesondert betrachten & bei mehreren root-commits (bspw. orphan-branches?) wie vorgehen?
     * 04.2021: Alle Entscheidungen extrahieren und sicher stellen, dass Entscheidungen nicht redundant persistiert werden
     * --> Prüfe ob bereits eine gleiche ExtractedDesignDecision gibt, die im gleichen Projekt (Repo/Proj),
     * zur gleichen Zeit "decisionCommitTime" den gleichen Migrationspfad (init->target) enthält.
     */
    private Set<ExtractedDesignDecisionEntity> extractDDForOrphanCommits(CrawledRepository crawledRepository, CrawledRevision crawledRevision,
                                                                         Set<ExtractedDesignDecisionEntity> overallExtractedDecisionCollection) {
        Set<ExtractedDesignDecisionEntity> extractedDecisions = new HashSet<>();
        if (CollectionUtils.isNotEmpty(crawledRevision.getProjectList())) {
            // Get consolidated distinct dependency lists
            Set<CrawledDependency> dependencySet = SetUtils.orderedSet(getConsolidatedDependencySet(crawledRevision));
            for (CrawledDependency crawledDependency : dependencySet) {
                if (crawledDependency != null && crawledDependency.isNewlyAdded()) {
                    // ---------------- NEW DECISION ---------------------
                    ExtractedDesignDecisionEntity addedDesignDecision = new ExtractedDesignDecisionEntity(crawledRepository.getRepositoryName(),
                            crawledRepository.getProjectName(), crawledRevision.getCommitId(), null, crawledRevision.getCommitId(),
                            crawledDependency.toGAString(), crawledDependency.getCategory(), crawledRevision.getCommitDate());

                    // if decision already extracted by analyzing replacements
                    if (!isAddedDesignDecisionAlreadyKnown(overallExtractedDecisionCollection, addedDesignDecision)) {
                        // decision extraction completed -> save to list
                        extractedDecisions.add(addedDesignDecision);
                    }
                }
            }
        }
        return extractedDecisions;
    }

    private CrawledRevision getRevisionByCommitId(List<CrawledRevision> revisionListOfRepository, String subsequentCommitId) throws NotFoundException {
        for (CrawledRevision crawledRevision : revisionListOfRepository) {
            if (crawledRevision != null && StringUtils.equalsIgnoreCase(crawledRevision.getCommitId(), subsequentCommitId)) {
                return crawledRevision;
            }
        }

        throw new NotFoundException("Found no revision for commit: " + subsequentCommitId);
    }

    private Set<ExtractedDesignDecisionEntity> findNewlyAddedDependencies(CrawledRepository crawledRepository,
                                                                          CrawledRevision crawledRevision,
                                                                          String subsequentCommitId,
                                                                          CrawledRevision subseqCrawledRevision,
                                                                          Set<CrawledDependency> subseqDepSet,
                                                                          Set<ExtractedDesignDecisionEntity> extractedDecisionsOfCommit,
                                                                          Set<ExtractedDesignDecisionEntity> overallExtractedDecisionCollection) {
        LOG.trace("  findNewlyAddedDependencies for {} subseq-dependencies", subseqDepSet.size());
        for (CrawledDependency subseqCrawledDependency : subseqDepSet) {
            if (subseqCrawledDependency != null && subseqCrawledDependency.isNewlyAdded()) {
                if (CollectionUtils.isNotEmpty(extractedDecisionsOfCommit)) {
                    boolean skipCrawledDependency = false;
                    for (ExtractedDesignDecisionEntity edd : extractedDecisionsOfCommit) {
                        if (StringUtils.equals(edd.getTarget(), subseqCrawledDependency.toGAString()) &&
                                StringUtils.equals(edd.getDecisionCommitId(), subsequentCommitId)) {
                            skipCrawledDependency = true;
                        }
                    }
                    if (skipCrawledDependency) {
                        continue;
                    }
                }

                // ---------------- NEW DECISION ---------------------
                ExtractedDesignDecisionEntity addedDesignDecision = new ExtractedDesignDecisionEntity(crawledRepository.getRepositoryName(),
                        crawledRepository.getProjectName(), crawledRevision.getCommitId(), null, subsequentCommitId,
                        subseqCrawledDependency.toGAString(), subseqCrawledDependency.getCategory(), subseqCrawledRevision.getCommitDate());

                // if decision is not already extracted by analyzing replacements
                if (!isAddedDesignDecisionAlreadyKnown(overallExtractedDecisionCollection, addedDesignDecision)) {
                    // decision extraction completed -> save to list
                    extractedDecisionsOfCommit.add(addedDesignDecision);
                }
            }
        }

        return extractedDecisionsOfCommit;
    }

    /**
     * Check for following construct of repeating decisions to exclude them:
     * <p>
     * r1: null -> x
     * r2: x -> null
     * ==> r1 + r2 should not be persisted if the revisions are subsequent!
     * <p>
     * If this occurs between two commits, it could be a crawling issue or an experiment / reverted decision
     */
    boolean isRevertedDecision(Set<ExtractedDesignDecisionEntity> ddSet, ExtractedDesignDecisionEntity dd) {
        // 1) check if reversed initial-target-DD is known in extractedDecisions
        if (ddSet.stream().anyMatch(p -> StringUtils.equals(p.getRepository(), dd.getRepository())
                && StringUtils.equals(p.getProject(), dd.getProject())
                && StringUtils.equals(p.getInitial(), dd.getTarget())  // find value from example: null
                && StringUtils.equals(p.getTarget(), dd.getInitial()))) {
            return true;
        }
        return false;
    }

    private Set<ExtractedDesignDecisionEntity> findRemovedAndReplacedDependencies(CrawledRepository crawledRepository,
                                                                                  CrawledRevision crawledRevision,
                                                                                  CrawledRevision subseqCrawledRevision,
                                                                                  Set<CrawledDependency> dependencySet,
                                                                                  List<CrawledRevision> revisionListOfRepository,
                                                                                  Set<CrawledDependency> subseqDepSet,
                                                                                  Set<ExtractedDesignDecisionEntity> extractedDecisionSet,
                                                                                  Set<ExtractedDesignDecisionEntity> overallExtractedDecisionSet) {
        LOG.trace("  findRemovedAndReplacedDependencies for {} dependencies and {} subseq-dependencies", dependencySet.size(), subseqDepSet.size());
        for (CrawledDependency crawledDependency : dependencySet) {
            if (crawledDependency != null && crawledDependency.isRemoved()) {
                boolean foundAnyReplacement = false;
                // check whether there are replacements in subsequent commits + save if new
                for (CrawledDependency subseqDependency : subseqDepSet) {
                    boolean foundReplacement = findReplacedDependencies(crawledRepository, crawledRevision, subseqCrawledRevision,
                            revisionListOfRepository, extractedDecisionSet, crawledDependency, subseqDependency, overallExtractedDecisionSet);

                    if (!foundAnyReplacement) {
                        foundAnyReplacement = foundReplacement;
                    }
                }

                // no replacement found => create removed-only decision + save if new
                if (!foundAnyReplacement) {
                    // ---------------- NEW DECISION ---------------------
                    ExtractedDesignDecisionEntity removedDesignDecision =
                            createRemovedDesignDecision(crawledRepository, revisionListOfRepository, crawledRevision, crawledDependency);

                    // decision extraction completed -> save to list if does not exist
                    if (!isRemovedDesignDecisionAlreadyKnown(overallExtractedDecisionSet, removedDesignDecision)) {
                        extractedDecisionSet.add(removedDesignDecision);
                    }
                }
            }
        }

        return extractedDecisionSet;
    }

    private boolean findReplacedDependencies(CrawledRepository crawledRepository, CrawledRevision crawledRevision,
                                             CrawledRevision subseqCrawledRevision, List<CrawledRevision> revisionListOfRepository,
                                             Set<ExtractedDesignDecisionEntity> extractedDecisionSet, CrawledDependency crawledDependency,
                                             CrawledDependency subseqDependency,
                                             Set<ExtractedDesignDecisionEntity> overallExtractedDecisionSet) {
        if (subseqDependency.isNewlyAdded() && subseqDependency.getCategory() != null && crawledDependency.isRemoved() &&
                subseqDependency.getCategory().equalsIgnoreCase(crawledDependency.getCategory())) {

            // ---------------- NEW DECISION ---------------------
            ExtractedDesignDecisionEntity replacedDesignDecision =
                    createRemovedDesignDecision(crawledRepository, revisionListOfRepository, crawledRevision, crawledDependency);
            replacedDesignDecision.setInitial(crawledDependency.toGAString());
            replacedDesignDecision.setDecisionCommitTime(crawledRevision.getCommitDate());
            replacedDesignDecision.setDecisionCommitId(crawledRevision.getCommitId());
            replacedDesignDecision.setTarget(subseqDependency.toGAString());
            replacedDesignDecision.setDecisionCommitTime(subseqCrawledRevision.getCommitDate());
            replacedDesignDecision.setDecisionCommitId(subseqCrawledRevision.getCommitId());

            // decision extraction completed -> save to list if does not exist
            if (!isReplacedDesignDecisionAlreadyKnown(overallExtractedDecisionSet, replacedDesignDecision)) {
                extractedDecisionSet.add(replacedDesignDecision);
            }

            // update revision
            crawledDependency.setReplacedBy(subseqDependency);
            mongoRevRepo.save(crawledRevision);
            return true;
        }
        return false;
    }

    boolean isAddedDesignDecisionAlreadyKnown(Set<ExtractedDesignDecisionEntity> extractedDecisions, ExtractedDesignDecisionEntity addedDesignDecision) {
        Predicate<ExtractedDesignDecisionEntity> isKnown = p -> StringUtils.equals(p.getRepository(), addedDesignDecision.getRepository())
                && StringUtils.equals(p.getProject(), addedDesignDecision.getProject())
                && StringUtils.equals(p.getTarget(), addedDesignDecision.getTarget());

        return extractedDecisions.stream().anyMatch(isKnown);
    }

    boolean isReplacedDesignDecisionAlreadyKnown(Set<ExtractedDesignDecisionEntity> extractedDecisions, ExtractedDesignDecisionEntity replacedDesignDecision) {
        Predicate<ExtractedDesignDecisionEntity> isKnown = p ->
                StringUtils.equals(p.getRepository(), replacedDesignDecision.getRepository())
                        && StringUtils.equals(p.getProject(), replacedDesignDecision.getProject())
                        && StringUtils.equals(p.getInitial(), replacedDesignDecision.getInitial())
                        && StringUtils.equals(p.getTarget(), replacedDesignDecision.getTarget());

        return extractedDecisions.stream().anyMatch(isKnown);
    }

    boolean isRemovedDesignDecisionAlreadyKnown(Set<ExtractedDesignDecisionEntity> extractedDecisions, ExtractedDesignDecisionEntity removedDesignDecision) {
        Predicate<ExtractedDesignDecisionEntity> isKnown = p -> StringUtils.equals(p.getRepository(), removedDesignDecision.getRepository())
                && StringUtils.equals(p.getProject(), removedDesignDecision.getProject())
                && StringUtils.equals(p.getInitial(), removedDesignDecision.getInitial())
                && StringUtils.equals(p.getTarget(), null);

        return extractedDecisions.stream().anyMatch(isKnown);
    }

    private ExtractedDesignDecisionEntity createRemovedDesignDecision(CrawledRepository crawledRepository, List<CrawledRevision> revisionListOfRepository,
                                                                      CrawledRevision crawledRevision, CrawledDependency crawledDependency) {
        ExtractedDesignDecisionEntity designDecision = new ExtractedDesignDecisionEntity(crawledRepository.getRepositoryName(), crawledRepository.getProjectName());
        designDecision.setInitialCommitId(crawledRevision.getCommitId());
        designDecision.setInitial(crawledDependency.toGAString());
        designDecision.setDecisionSubject(crawledDependency.getCategory());
        designDecision.setTarget(null);
        CrawledRevision nextSubSeqRevision = findNextNearestSubseqRevision(crawledRevision.getSubsequentCommitIdList(), revisionListOfRepository);
        if (nextSubSeqRevision != null) {
            designDecision.setDecisionCommitTime(nextSubSeqRevision.getCommitDate());
            designDecision.setDecisionCommitId(nextSubSeqRevision.getCommitId());
        }
        return designDecision;
    }

    private CrawledRevision findNextNearestSubseqRevision(List<String> subsequentCommitIdList, List<CrawledRevision> revisionListOfRepository) {
        CrawledRevision nextNearestRev = null;
        for (String subsequentCommitId : subsequentCommitIdList) {
            try {
                CrawledRevision subseqCrawledRevision = getRevisionByCommitId(revisionListOfRepository, subsequentCommitId);
                if (nextNearestRev == null) {
                    nextNearestRev = subseqCrawledRevision;
                } else if (subseqCrawledRevision.getCommitDate().isBefore(nextNearestRev.getCommitDate())) {
                    nextNearestRev = subseqCrawledRevision;
                }
            } catch (NotFoundException e) {
                // No revision found -> ignore and get next
            }
        }
        return nextNearestRev;
    }

    private Set<CrawledDependency> getConsolidatedDependencySet(CrawledRevision crawledRevision) {
        Set<CrawledDependency> dependencySet = new HashSet<>();
        if (crawledRevision != null) {
            for (CrawledProject crawledProject : crawledRevision.getProjectList()) {
                if (crawledProject != null) {
                    dependencySet.addAll(crawledProject.getDependencyList());
                }
            }
        }
        return dependencySet;
    }

    private Set<String> findOrphanRootCommits(List<CrawledRevision> revisionListOfRepository) {
        LOG.trace("Find orphan commits in revisionlist...");
        Set<String> rootCommitIdSet = new HashSet<>();
        Set<String> subsetCommitIdSet = new HashSet<>();

        // 1) find all subset-commits
        for (CrawledRevision crawledRevision : revisionListOfRepository) {
            if (crawledRevision != null && CollectionUtils.isNotEmpty(crawledRevision.getSubsequentCommitIdList())) {
                subsetCommitIdSet.addAll(crawledRevision.getSubsequentCommitIdList());
            }
        }

        // 2) check if revision-commit is not in subset-commit-list (=root-commit)
        for (CrawledRevision crawledRevision : revisionListOfRepository) {
            if (CollectionUtils.isEmpty(crawledRevision.getSubsequentCommitIdList()) && CollectionUtils.isEmpty(subsetCommitIdSet)) {
                LOG.trace("  -- Found orphan commit in revisionlist: " + crawledRevision.getCommitId());
                rootCommitIdSet.add(crawledRevision.getCommitId());
            } else if (crawledRevision.getCommitId() != null && !subsetCommitIdSet.contains(crawledRevision.getCommitId())) {
                LOG.trace("  -- Found orphan/root commit in revisionlist: " + crawledRevision.getCommitId());
                rootCommitIdSet.add(crawledRevision.getCommitId());
            }
        }

        return rootCommitIdSet;
    }
}
