package edu.hm.ccwi.matilda.runner.cleaner;

import edu.hm.ccwi.matilda.base.util.IOHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class JanitorService {

    private static final Logger LOG = LoggerFactory.getLogger(JanitorService.class);

    public void cleanUpFolder(File dir) {
        if (dir.exists() && dir.isDirectory()) {
            LOG.info("Janitor - removing folder: {}", dir.getName());
            IOHandler.removeDirSafely(dir);
        }
    }

    /**
     * Zipping (and unzipping) example from: https://www.baeldung.com/java-compress-and-uncompress
     */
    public void archiveCrawledProject(String projectDir, String dirName) throws IOException {
        File projectCloneDir = new File(projectDir + File.separator + dirName);
        LOG.info("Janitor - archive project dir {}", projectCloneDir.getAbsolutePath());
        if (projectCloneDir.exists() && projectCloneDir.isDirectory()) {
            try (FileOutputStream fos = new FileOutputStream(projectDir + File.separator + dirName + ".zip")) {
                try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {
                    zipFile(projectCloneDir, projectCloneDir.getName(), zipOut);
                }
            }
        }
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
            }
            zipOut.closeEntry();
            File[] children = fileToZip.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
                }
            }
            return;
        }

        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            zipOut.putNextEntry(new ZipEntry(fileName));
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
        }
    }
}