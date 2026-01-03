package edu.hm.ccwi.matilda.runner.cleaner;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;

public class MeisterProperRunner {

    private static final String CRAWLING_DIRECTORY = "/opt/matilda/crawling";

    public static void main(String[] args) {
        String[] repositories = new File(CRAWLING_DIRECTORY).list();
        if (repositories != null) {
            for (String repository : repositories) {
                String[] project = new File(CRAWLING_DIRECTORY + "/" + repository).list();
                if (ArrayUtils.isEmpty(project)) {
                    continue;
                }
                new Thread(new MeisterProper(CRAWLING_DIRECTORY, repository, project)).start();
            }
        }
    }
}