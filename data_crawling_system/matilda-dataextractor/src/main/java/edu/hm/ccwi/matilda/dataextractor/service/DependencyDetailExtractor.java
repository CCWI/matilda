package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.util.CrawledRevisionHelper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyDetailExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(DependencyDetailExtractor.class);

    private final List<CrawledRevision> overallSubsequentRevisions;
    private final List<CrawledRevision> olderRevisionList;
    private CrawledRevision currentRevision;

    public Set<String> tmpDebugListOfAddedSubsequences;
    public Set<String> tmpDebugListOfRemovedSubsequences;

    public DependencyDetailExtractor(CrawledRevision currentRevision, List<CrawledRevision> overallSubsequentRevisions,
                                     List<CrawledRevision> olderRevisionList) {
        this.currentRevision = currentRevision;
        this.overallSubsequentRevisions = overallSubsequentRevisions;
        this.olderRevisionList = olderRevisionList;
        this.tmpDebugListOfRemovedSubsequences = new HashSet<>();
        this.tmpDebugListOfAddedSubsequences = new HashSet<>();
    }

    protected Set<CrawledRevision> findRemovableRevisionsAndPrepareDeletion(Map<String, CrawledRevision> overallRemovableMarkedRevisions) {
        Set<CrawledRevision> removableCrawledRevisions = new HashSet<>();

        if (CollectionUtils.isNotEmpty(currentRevision.getProjectList())) {
            LinkedHashSet<String> newSubsequencesForCurrentRevision = new LinkedHashSet<>();
            for (String subseqCommitId : currentRevision.getSubsequentCommitIdList()) {
                newSubsequencesForCurrentRevision.addAll(processSubSeqRev(removableCrawledRevisions, subseqCommitId, overallRemovableMarkedRevisions));
            }
            if (CollectionUtils.isNotEmpty(newSubsequencesForCurrentRevision)) {
                // 1) Add Subset of subproj to proj
                currentRevision.addSubsequentRevisionList(newSubsequencesForCurrentRevision);
                tmpDebugListOfAddedSubsequences.addAll(newSubsequencesForCurrentRevision);
            }
        }
        return removableCrawledRevisions;
    }

    private LinkedHashSet<String> processSubSeqRev(Set<CrawledRevision> removableCrawledRevisions,
                                                   String subseqCommitId,
                                                   Map<String, CrawledRevision> overallRemovableMarkedRevisions) {
        LinkedHashSet<String> newSubsequencesForCurrentRevision = new LinkedHashSet<>();
        CrawledRevision subseqRev = CrawledRevisionHelper.findRevisionById(overallSubsequentRevisions, subseqCommitId);
        if (subseqRev == null || CollectionUtils.isEmpty(subseqRev.getProjectList())) {
            return newSubsequencesForCurrentRevision;
        }

        // Optimization: subseq-commitId is already marked as removed
        // -> if this is true (should be rare case in complex trees), then just remove ref to subseq-commit and
        //    link all subsubseq-commits to currentRev
        if(overallRemovableMarkedRevisions.containsKey(subseqCommitId)) {
            prepareDeletionOfSubRevision(removableCrawledRevisions, newSubsequencesForCurrentRevision, subseqRev);
        }

        boolean dependenciesChangedBetweenVersions = false;
        for (CrawledProject project : currentRevision.getProjectList()) {
            if(olderRevisionList.isEmpty()) {
                markDependenciesOfOriginalCommitAsNew(project);
            }
            for (CrawledProject subSeqProject : subseqRev.getProjectList()) {
                if (checkProjectDetailsAreAvailable(project, subSeqProject) &&
                        isSameProjects(project, currentRevision.getCommitId(), subSeqProject, subseqRev.getCommitId())) {
                    // FORWARD-check: compare dep. with subsequent dep. - if no change, remove nextRev to avoid duplicate dep-lists
                    if (dependenciesChangedBetweenVersions(project.getDependencyList(), subSeqProject.getDependencyList())) {
                        // changes detected note dependencies as "newlyAdded" or "isRemoved"
                        analyzeRemovedAndNewlyAddedDependencies(project.getDependencyList(), subSeqProject.getDependencyList());
                        dependenciesChangedBetweenVersions = true;
                    }
                }
            }
        }

        if (!dependenciesChangedBetweenVersions) {
            prepareDeletionOfSubRevision(removableCrawledRevisions, newSubsequencesForCurrentRevision, subseqRev);
        }

        return newSubsequencesForCurrentRevision;
    }

    private void prepareDeletionOfSubRevision(Set<CrawledRevision> removableCrawledRevisions,
                                              LinkedHashSet<String> newSubsequencesForCurrentRevision,
                                              CrawledRevision subseqRev) {
        // 0) Remove subseq-revision from current revision -> those are not relevant anymore
        // and will be marked to remove from revision-list as well
        currentRevision.getSubsequentCommitIdList().remove(subseqRev);
        tmpDebugListOfRemovedSubsequences.add(subseqRev.getCommitId());

        // 1) Add Subset of subproj to proj
        for (String subsubseqRevCommitId : subseqRev.getSubsequentCommitIdList()) {
            newSubsequencesForCurrentRevision.add(subsubseqRevCommitId);
        }

        logTrace(subseqRev);

        if (currentRevision.getSimilarFollowupCommitsUntil() == null ||
                (currentRevision.getSimilarFollowupCommitsUntil() != null && subseqRev.getCommitDate() != null &&
                        currentRevision.getSimilarFollowupCommitsUntil().isBefore(subseqRev.getCommitDate()))) {

            // 2) setSimilarFollowupCommitsUntil(subproj.getCommitDate()) if it is unset or before commitdate of subseq
            currentRevision.setSimilarFollowupCommitsUntil(subseqRev.getCommitDate());

            // 3) return list of removeable subproj-list
            removableCrawledRevisions.add(subseqRev);
        }
    }

    private void logTrace(CrawledRevision subseqRev) {
        if(LOG.isTraceEnabled()) {
            LOG.trace("        RemoveRevisionAlgorithm: REMOVE COMMIT: {}", subseqRev.getCommitId());
            LOG.trace("        RemoveRevisionAlgorithm: ENRICH PARENT {} including {} subsequent commits BY {} subsequent " +
                            "commits of removable revision", currentRevision.getCommitId(),
                    currentRevision.getSubsequentCommitIdList().size(), subseqRev.getSubsequentCommitIdList().size());
            LOG.trace("          -> Commit {} has now {} subsequent commits",
                    currentRevision.getCommitId(), currentRevision.getSubsequentCommitIdList().size());
        }
    }

    private boolean checkProjectDetailsAreAvailable(CrawledProject project, CrawledProject followUpProject) {
        return project != null && project.getProjectGroup() != null && project.getProjectArtifact() != null &&
                followUpProject != null && followUpProject.getProjectGroup() != null &&
                followUpProject.getProjectArtifact() != null && project.getProjectPath() != null &&
                followUpProject.getProjectPath() != null;
    }

    private boolean isSameProjects(CrawledProject project, String commitId, CrawledProject followUpProject, String subseqId) {
        return project.getProjectGroup().equalsIgnoreCase(followUpProject.getProjectGroup()) &&
                project.getProjectArtifact().equalsIgnoreCase(followUpProject.getProjectArtifact()) &&
                checkForSameProjectsByPaths(project, commitId, followUpProject, subseqId);
    }

    private boolean checkForSameProjectsByPaths(CrawledProject project, String commitId, CrawledProject followUpProject, String subseqId) {
        if(project.getProjectPath().contains(commitId) && followUpProject.getProjectPath().contains(subseqId)) {
            String relProjPath = project.getProjectPath().substring(project.getProjectPath().lastIndexOf(commitId) + commitId.length());
            String relSubPPath = followUpProject.getProjectPath().substring(followUpProject.getProjectPath().lastIndexOf(subseqId) + subseqId.length());
            return StringUtils.equalsIgnoreCase(relProjPath, relSubPPath);
        }
        if(project.getProjectPath().contains("/src/") && followUpProject.getProjectPath().contains("/src/")) {
            String projectSubPath = project.getProjectPath().substring(project.getProjectPath().indexOf("/src/"));
            String followUpProjectSubPath = followUpProject.getProjectPath().substring(project.getProjectPath().indexOf("/src/"));
            return projectSubPath.equalsIgnoreCase(followUpProjectSubPath);
        } else {
            // Set true, since it might be the parent-DMF or they are using a different folder structure instead of the common /src/.
            // (In this case GA should at least be the same!)
            return true;
        }
    }

    protected void markDependenciesOfOriginalCommitAsNew(CrawledProject project) {
        // mark all newly added dependencies from first/initial commit
        for(CrawledDependency cd : project.getDependencyList()) {
            cd.setNewlyAdded(true);
        }
    }

    private String getGroupArtifactString(String projectGroup, String projectArtifact) {
        return projectGroup.trim() + ":" + projectArtifact.trim();
    }

    private boolean dependenciesChangedBetweenVersions(List<CrawledDependency> projCDList, List<CrawledDependency> subseqCDList) {
        return (checkDepListChanged(projCDList, subseqCDList) || checkDepListChanged(subseqCDList, projCDList));
    }

    /**
     * Check on removed and newly added Dependencies forward and backward.
     */
    public List<CrawledDependency> analyzeRemovedAndNewlyAddedDependencies(List<CrawledDependency> projCDList, List<CrawledDependency> subseqProjCDList) {
        // find all removed (relevant) dependencies
        List<CrawledDependency> forwardChangesRelDep = getChangedRelevantDependenciesBetweenLists(projCDList, subseqProjCDList);
        forwardChangesRelDep.forEach(relDep -> relDep.setRemoved(true));

        // check for a problem occurred, indicated by "removed" is on true for all dependencies
        if(checkForAllDependenciesRemovedInBiggerDependencyList(projCDList)) {
            forwardChangesRelDep.forEach(relDep -> relDep.setRemoved(false));
        }

        // find all newly added dependencies
        List<CrawledDependency> backwardChangesRelDep = getChangedRelevantDependenciesBetweenLists(subseqProjCDList, projCDList);
        backwardChangesRelDep.forEach(relDep -> relDep.setNewlyAdded(true));

        // find replaced dependencies
        for (CrawledDependency removedDep : forwardChangesRelDep) {
            if (removedDep.isRemoved() && removedDep.isRelevant()) {
                for (CrawledDependency crawledDependency : backwardChangesRelDep) {
                    if (crawledDependency.isNewlyAdded() && crawledDependency.isRelevant()) {
                        if (removedDep.getCategory() != null && crawledDependency.getCategory() != null &&
                                removedDep.getCategory().equals(crawledDependency.getCategory())) {
                            removedDep.setReplacedBy(crawledDependency);
                        }
                    }
                }
            }
        }

        return Stream.concat(forwardChangesRelDep.stream(), backwardChangesRelDep.stream()).collect(Collectors.toList());
    }

    public boolean checkForAllDependenciesRemovedInBiggerDependencyList(List<CrawledDependency> forwardChangesRelDep) {
        return forwardChangesRelDep.size() > 2 && forwardChangesRelDep.stream().allMatch(CrawledDependency::isRemoved);
    }

    /**
     * Compare two tag-strings. //TODO It must match 100% for now, but might be lowered later by config)
     */
    public boolean isTagListEqualOrContaining(List<String> tagList1, List<String> tagList2) {
        if((CollectionUtils.isEmpty(tagList1) && CollectionUtils.isNotEmpty(tagList2)) ||
                (CollectionUtils.isNotEmpty(tagList1) && CollectionUtils.isEmpty(tagList2))) {
            return false;
        }

        if(CollectionUtils.isEqualCollection(tagList1, tagList2)) {
            return true;
        } else {
            return CollectionUtils.isNotEmpty(tagList1) && CollectionUtils.isNotEmpty(tagList2)
                    && CollectionUtils.containsAll(tagList1, tagList2) || CollectionUtils.containsAll(tagList2, tagList1);
        }
    }

    /**
     * Checks every element of the originList, if it exists in the second checkingList.
     */
    public boolean checkDepListChanged(List<CrawledDependency> originList, List<CrawledDependency> checkingList) {
        for (CrawledDependency originDep : originList) {
            boolean foundInOtherList = false;
            for (CrawledDependency checkDep : checkingList) {
                if (getGroupArtifactString(originDep.getGroup(), originDep.getArtifact())
                        .equalsIgnoreCase(getGroupArtifactString(checkDep.getGroup(), checkDep.getArtifact()))) {
                    foundInOtherList = true;
                }
            }
            if(!foundInOtherList) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks relevant elements of the originList, if it exists in the second checkingList.
     */
    public List<CrawledDependency> getChangedRelevantDependenciesBetweenLists(List<CrawledDependency> originList,
                                                                              List<CrawledDependency> checkingList) {
        List<CrawledDependency> changedRelevantDependencies = new ArrayList<>();
        for (CrawledDependency originDep : originList) {
            boolean foundInOtherList = false;
            for (CrawledDependency checkDep : checkingList) {
                if (getGroupArtifactString(originDep.getGroup(), originDep.getArtifact())
                    .equalsIgnoreCase(getGroupArtifactString(checkDep.getGroup(), checkDep.getArtifact()))) {
                        foundInOtherList = true;
                }
            }
            if(!foundInOtherList) {
                changedRelevantDependencies.add(originDep);
            }
        }
        return changedRelevantDependencies;
    }

    public CrawledRevision getCurrentRevision() {
        return currentRevision;
    }

    public void setCurrentRevision(CrawledRevision currentRevision) {
        this.currentRevision = currentRevision;
    }
}
