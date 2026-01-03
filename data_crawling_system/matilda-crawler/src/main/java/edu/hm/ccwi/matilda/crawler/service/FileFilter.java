package edu.hm.ccwi.matilda.crawler.service;

import java.util.ArrayList;
import java.util.List;

public class FileFilter {

    public static List<String> documentationNameFilter() {
        List<String> filterList = new ArrayList<>();
        filterList.add("README.md");
        filterList.add("README.markdown");
        return filterList;
    }

    public static List<String> dMFNameFilter() {
        List<String> filterList = new ArrayList<>();
        filterList.add("pom.xml");
        // Note: Currently only pom.xml is supported
        // Future support planned for: build.gradle, build.sbt, ivy.xml, project.clj, package.json
        // Also considering: ant, buildr, leiningen, grape
        return filterList;
    }
}