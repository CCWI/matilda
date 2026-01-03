package edu.hm.ccwi.matilda.persistence.mongo.util;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;

import java.util.Comparator;

public class RevisionTimeSorter implements Comparator<CrawledRevision> {

    @Override
    public int compare(CrawledRevision rev1, CrawledRevision rev2) {
        return rev1.getCommitDate().compareTo(rev2.getCommitDate());
    }
}