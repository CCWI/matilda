package edu.hm.ccwi.matilda.korpus.model;

import java.time.LocalDate;

public class ProcessingProjectDto {

    private String matildaId;
    private RepoSource repoSource;
    private String repoProjName;
    private String repoName;
    private String projectName;
    private String projectDir;
    private String targetPath;
    private boolean repoUpdateable;
    private boolean janitor;
    private LocalDate since;

    public ProcessingProjectDto(String matildaId, RepoSource repoSource, String repoProjName, String targetPath,
                                boolean repoUpdateable, boolean useJanitor, LocalDate commitsSince) {
        this.matildaId = matildaId;
        this.repoSource = repoSource;
        this.repoProjName = repoProjName;
        this.targetPath = targetPath;
        this.repoUpdateable = repoUpdateable;
        this.janitor = useJanitor;
        this.since = commitsSince;
    }

    public ProcessingProjectDto(String matildaId, RepoSource repoSource, String repoProjName, String repoName,
                                String projectName, String projectDir, String targetPath, boolean repoUpdateable, boolean janitor, LocalDate since) {
        this.matildaId = matildaId;
        this.repoSource = repoSource;
        this.repoProjName = repoProjName;
        this.repoName = repoName;
        this.projectName = projectName;
        this.projectDir = projectDir;
        this.targetPath = targetPath;
        this.repoUpdateable = repoUpdateable;
        this.janitor = janitor;
        this.since = since;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public RepoSource getRepoSource() {
        return repoSource;
    }

    public void setRepoSource(RepoSource repoSource) {
        this.repoSource = repoSource;
    }

    public String getRepoProjName() {
        return repoProjName;
    }

    public void setRepoProjName(String repoProjName) {
        this.repoProjName = repoProjName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public boolean isRepoUpdateable() {
        return repoUpdateable;
    }

    public void setRepoUpdateable(boolean repoUpdateable) {
        this.repoUpdateable = repoUpdateable;
    }

    public boolean isJanitor() {
        return janitor;
    }

    public void setJanitor(boolean janitor) {
        this.janitor = janitor;
    }

    public LocalDate getSince() {
        return since;
    }

    public void setSince(LocalDate since) {
        this.since = since;
    }
}
