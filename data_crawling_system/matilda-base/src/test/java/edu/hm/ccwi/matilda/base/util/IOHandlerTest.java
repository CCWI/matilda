package edu.hm.ccwi.matilda.base.util;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

public class IOHandlerTest {

    @Test
    public void getCloneDirectoryTest() throws IOException {
        File file = new IOHandler().getCloneDirectory("matilda-repo");
        System.out.println("AbstractPath: " + file.getAbsolutePath());
        System.out.println("Canonical: " + file.getCanonicalPath());
        System.out.println("Path: " + file.getPath());
    }
}
