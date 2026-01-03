package edu.hm.ccwi.matilda.base.util;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class IOHandler {

    private static final Logger LOG = LoggerFactory.getLogger(IOHandler.class);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(5);

    private final List<File> fileList;

    public IOHandler() {
        fileList = new ArrayList();
    }

    public File getCloneDirectory(String targetPath) {
        Path path = Paths.get(System.getProperty("user.home"), targetPath);
        do {
            path.toFile().mkdirs();
        } while (!Files.exists(path));
        return path.toFile();
    }

    public List<File> findFile(String name, File file) {
        File[] list = file.listFiles();
        if (list != null) {
            for (File fil : list) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else if (StringUtils.equalsIgnoreCase(name, fil.getName())) {
                    fileList.add(fil.getParentFile());
                }
            }
        }
        return fileList;
    }

    public List<File> findSimilarFiles(String name, File file) {
        if (file.listFiles() != null) {
            for (File fil : file.listFiles()) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else {
                    if (fil.isFile() && StringUtils.contains(fil.getName().toLowerCase(), name.toLowerCase())) {
                        fileList.add(fil);
                    }
                }
            }
        }
        return fileList;
    }

    public boolean similarFilesAvailable(String name, File file) {
        if (file.listFiles() != null) {
            for (File fil : file.listFiles()) {
                if (fil.isDirectory()) {
                    findFile(name, fil);
                } else {
                    if (fil.isFile() && StringUtils.contains(fil.getName().toLowerCase(), name.toLowerCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /**
     * Remove directory safely (asynchronous with gc and thread sleep).
     */
    public static void removeDirSafely(File dir) {
        try {
            Thread.sleep(1000);
            System.gc();
            Thread.sleep(2000);
            Future<Boolean> successfulCleaning = EXECUTOR_SERVICE.submit(new FileCleaner(dir));
            LOG.info("IOHandler: Remove directory safely: {} -- success: {}", dir.getAbsoluteFile(),
                    successfulCleaning.get(600, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            LOG.error("Error while remove directory safely occurred. ", e);
        }
    }
}