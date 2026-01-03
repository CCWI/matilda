package edu.hm.ccwi.matilda.persistence.mongo.model;

import edu.hm.ccwi.matilda.base.model.enumeration.RevisionType;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class CrawledRevision {

    @Id
    private String commitId;
    private List<String> subsequentCommitIdList;
    private List<String> branchList;
    private RevisionType type;
    private String directoryPath;
    private LocalDateTime commitDate;
    private LocalDateTime dateOfClone;
    private LocalDateTime similarFollowupCommitsUntil;
    private List<CrawledProject> projectList;

    public CrawledRevision() {
        this.subsequentCommitIdList = new ArrayList<>();
    }

    public CrawledRevision(String commitId, RevisionType type, String directoryPath, LocalDateTime commitDate,
                           LocalDateTime dateOfClone, List<CrawledProject> projectList) {
        this.commitId = commitId;
        this.subsequentCommitIdList = new ArrayList<>();
        this.branchList = new ArrayList<>();
        this.type = type;
        this.directoryPath = directoryPath;
        this.commitDate = commitDate;
        this.dateOfClone = dateOfClone;
        this.projectList = projectList;
    }

    public void addCrawledProjectList(List<CrawledProject> cpList) {
        if(projectList == null) {
            projectList = new ArrayList<>();
        }
        for(CrawledProject cp : cpList) {
            projectList.add(cp);
        }
    }

    public void addSubsequentRevision(String subsequentCommitId) {
        if(this.subsequentCommitIdList == null) {
            this.subsequentCommitIdList = new ArrayList<>();
        }
        this.subsequentCommitIdList.add(subsequentCommitId);
    }

    public void addSubsequentRevision(String... subsequentCommitIds) {
        if(this.subsequentCommitIdList == null) {
            this.subsequentCommitIdList = new ArrayList<>();
        }
        for (String subsequentCommitId : subsequentCommitIds) {
            this.subsequentCommitIdList.add(subsequentCommitId);
        }
    }

    public void addSubsequentRevisionList(Set<String> anotherSubsequentCommitIdList) {
        if(this.subsequentCommitIdList == null) {
            this.subsequentCommitIdList = new ArrayList<>();
        }
        for (String subsequentRevisionCommitId : anotherSubsequentCommitIdList) {
            if(!this.subsequentCommitIdList.contains(subsequentRevisionCommitId)) {
                this.subsequentCommitIdList.add(subsequentRevisionCommitId);
            }
        }
    }

    public void setSubsequentCommitIdList(List<String> subsequentCommitIdList) {
        this.subsequentCommitIdList = subsequentCommitIdList;
    }

    public void setBranchList(List<String> branchList) {
        this.branchList = branchList;
    }

    public void addBranch(String branchname) {
        if(this.branchList == null) {
            this.branchList = new ArrayList<>();
        }
        this.branchList.add(branchname);
    }

    public String getCommitId() { return commitId; }

    public void setCommitId(String commitId) { this.commitId = commitId; }

    public RevisionType getType() {
        return type;
    }

    public void setType(RevisionType type) {
        this.type = type;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }

    public LocalDateTime getCommitDate() {
        return commitDate;
    }

    public void setCommitDate(LocalDateTime commitDate) {
        this.commitDate = commitDate;
    }

    public List<CrawledProject> getProjectList() {
        return projectList;
    }

    public void setProjectList(List<CrawledProject> projectList) {
        this.projectList = projectList;
    }

    public LocalDateTime getDateOfClone() { return dateOfClone; }

    public void setDateOfClone(LocalDateTime dateOfClone) { this.dateOfClone = dateOfClone; }

    public LocalDateTime getSimilarFollowupCommitsUntil() {
        return similarFollowupCommitsUntil;
    }

    public void setSimilarFollowupCommitsUntil(LocalDateTime similarFollowupCommitsUntil) {
        this.similarFollowupCommitsUntil = similarFollowupCommitsUntil;
    }

    public List<String> getSubsequentCommitIdList() { return subsequentCommitIdList; }

    public List<String> getBranchList() { return branchList; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CrawledRevision that = (CrawledRevision) o;
        return Objects.equals(commitId, that.commitId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commitId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}