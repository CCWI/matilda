package edu.hm.ccwi.matilda.base.util;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

public class FileCleaner implements Callable<Boolean> {

    private static final Logger LOG = LoggerFactory.getLogger(FileCleaner.class);

    private File fileDir;

    public FileCleaner(File dir) {
        this.fileDir = dir;
    }

    @Override
    public Boolean call() {
        LOG.info("Deleting dir: " + fileDir);
        Boolean success = false;
        try {
            if (isDirNotDeleted(success)) {
                success = deleteRecursively();
            }
            if (isDirNotDeleted(success)) {
                deleteDirDirectly();
            }

            LOG.info("Deleting dir " + fileDir + " recursively successful in first place: " + success);
            if (isDirNotDeleted(success)) {
                FileUtils.forceDeleteOnExit(fileDir);
            }
        } catch (IOException e) {
            LOG.error("Exception while deleting dir '" + this.fileDir.getPath() + "': " + e.getMessage());
        }

        return success;
    }

    private boolean isDirNotDeleted(Boolean success) {
        return (!success) && this.fileDir != null && this.fileDir.isDirectory() && this.fileDir.exists();
    }

    private boolean deleteRecursively() {
        boolean success = false;
        try {
            Thread.sleep(5000);
            try(Stream<Path> walk = Files.walk(this.fileDir.toPath())) {
            walk.map(Path::toFile).sorted(Comparator.reverseOrder()).forEach(File::delete);
            }
            Thread.sleep(2000);
            if (this.fileDir != null && this.fileDir.isDirectory() && this.fileDir.exists()) {
                success = FileSystemUtils.deleteRecursively(this.fileDir);
            }
        } catch (IOException e) {
            LOG.error("Error while deleting directories recursively: {}", e.getMessage());
        } catch (InterruptedException e) {
            LOG.error("Error while deleting directories recursively: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        return success;
    }

    public void deleteDirDirectly() {
        try {
            Thread.sleep(2000);
            FileUtils.deleteDirectory(fileDir);
        } catch (IOException | InterruptedException e) {
            LOG.error("Error while deleting directories recursively: {}", e.getMessage());
        }

    }
}