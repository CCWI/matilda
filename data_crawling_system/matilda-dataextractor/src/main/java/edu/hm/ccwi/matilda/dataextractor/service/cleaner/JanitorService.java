package edu.hm.ccwi.matilda.dataextractor.service.cleaner;

import edu.hm.ccwi.matilda.base.util.IOHandler;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
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
    void archiveCrawledProject(String projectDir, String dirName) throws IOException {
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

    void unarchiveCrawledProject(String zipFolder, String zipFileName) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFolder + File.separator + zipFileName))) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFileForUnzip(new File(zipFolder), zipEntry);
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    void unzipIO(Path path, Charset charset) throws IOException {
        String fileBaseName = FilenameUtils.getBaseName(path.getFileName().toString());
        Path destFolderPath = Paths.get(path.getParent().toString(), fileBaseName);

        try (ZipFile zipFile = new ZipFile(path.toFile(), ZipFile.OPEN_READ, charset)){
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = destFolderPath.resolve(entry.getName());
                entryPath = new File(entryPath.toString().replace("clone" + File.separator + "clone", "clone"))
                        .toPath();
                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    try (InputStream in = zipFile.getInputStream(entry)){
                        try (OutputStream out = new FileOutputStream(entryPath.toFile())){
                            IOUtils.copy(in, out);
                        }
                    }
                }
            }
        }
    }

    private File newFileForUnzip(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
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