package edu.hm.ccwi.matilda.base.util;

import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

public class GitCommons {

    public static String extractHashFromCommitIdString(String fullStringCommit) {
        List fragmentedCommit = Collections.list(new StringTokenizer(fullStringCommit, " "));
        Object o = fragmentedCommit.size() > 1 ? fragmentedCommit.get(1) : fragmentedCommit.get(0);
        if(o != null) {
            return o.toString();
        }
        return null;
    }

    public static String createCommitName(RevCommit commit) throws IOException {
        String commitId = extractHashFromCommitIdString(commit.getId().toString());
        return commit.getCommitTime() + "_" + commitId;
    }

    public static LocalDateTime convertEpochSecondToLocalDate(String epochSeconds) {
        return Instant.ofEpochSecond(Long.parseLong(epochSeconds)).atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Check if repo is already cloned and files are in the folder.
     */
    public static boolean isRepoLocallyAvailable(Path pathToClone) {
        if (pathToClone != null) {
            File file = pathToClone.toFile();
            return file.isDirectory() && file.listFiles().length > 0;
        }
        return false;
    }
}
