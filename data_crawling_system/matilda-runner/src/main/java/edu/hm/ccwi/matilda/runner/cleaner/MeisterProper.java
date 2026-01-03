package edu.hm.ccwi.matilda.runner.cleaner;

import java.io.File;
import java.io.IOException;

public class MeisterProper implements Runnable {

    private final String crawlingDirectory;
    private final String repository;
    private final String[] projects;

    public MeisterProper(String crawlingDirectory, String repository, String[] projects) {
        this.crawlingDirectory = crawlingDirectory;
        this.repository = repository;
        this.projects = projects;
    }

    @Override
    public void run() {
        for (String project : projects) {
            try {
                cleanupInProject(new JanitorService(), repository, project);
            } catch (Exception e) {
                System.err.println("An error occurred for " + repository + "/" + project + ". " +
                        "Meister Proper is continuing with next Project");
            }
        }
    }

    private void cleanupInProject(JanitorService janitorService, String repository, String project) throws IOException {
        File projDir = new File(crawlingDirectory + "/" + repository + "/" + project);

        String[] cloneDirs = projDir.list((directory, fileName) -> fileName.equals("clone"));
        String[] commitsDirs = projDir.list((directory, fileName) -> fileName.equals("commits"));

        boolean wasCommitDirAvailable = false;
        if (commitsDirs != null) {
            for (String commitDir : commitsDirs) {
                wasCommitDirAvailable = removeCommitDir(janitorService, repository, project, wasCommitDirAvailable, commitDir);
            }
        }

        if (cloneDirs != null) {
            for (String cloneDir : cloneDirs) {
                cleanupDirectories(janitorService, repository, project, wasCommitDirAvailable, cloneDir);
            }
        }
    }

    private void cleanupDirectories(JanitorService janitorService, String repository, String project,
                                           boolean wasCommitDirAvailable, String cloneDir) throws IOException {
        File clone = new File(crawlingDirectory + "/" + repository + "/" + project + "/" + cloneDir);
        if (clone.isDirectory()) {
            System.out.println(clone.getAbsolutePath());
            // ZIP Dir
            if (wasCommitDirAvailable) {
                janitorService.archiveCrawledProject(crawlingDirectory + "/" + repository + "/" + project, "clone");
                janitorService.cleanUpFolder(clone); // remove dir
            } else {
                janitorService.cleanUpFolder(new File(crawlingDirectory + "/" + repository + "/" + project)); // remove dir
            }
        }
    }

    private boolean removeCommitDir(JanitorService janitorService, String repository, String project,
                                           boolean isCommitDirAvailable, String commitDir) {
        File commit = new File(crawlingDirectory + "/" + repository + "/" + project + "/" + commitDir);
        if (commit.isDirectory()) {
            System.out.println(commit.getAbsolutePath());
            isCommitDirAvailable = true;
            janitorService.cleanUpFolder(commit); // remove dir
        }
        return isCommitDirAvailable;
    }
}
