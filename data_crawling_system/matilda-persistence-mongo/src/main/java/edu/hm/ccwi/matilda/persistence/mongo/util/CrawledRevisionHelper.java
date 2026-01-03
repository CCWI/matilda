package edu.hm.ccwi.matilda.persistence.mongo.util;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;

import java.util.List;
import java.util.ListIterator;

public class CrawledRevisionHelper {

    public static CrawledRevision findRevisionById(List<CrawledRevision> subsequentRevisionList, String subseqCommitId) {
        return subsequentRevisionList.stream()
                .filter(subseqRev -> subseqCommitId != null && subseqCommitId.equalsIgnoreCase(subseqRev.getCommitId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find parent revision for specific crawledRevision.
     * 
     * @param currentRevision - Revision under analysis to find its parent
     * @param previousRevisionList - Revisionlist which might include the parent of the provided revision
     * @return
     */
    public static CrawledRevision findParentRevision(CrawledRevision currentRevision, List<CrawledRevision> previousRevisionList) {
        // Iterate in reverse.
        ListIterator<CrawledRevision> li = previousRevisionList.listIterator(previousRevisionList.size());
        while (li.hasPrevious()) {
            CrawledRevision prevOlderRev = li.previous();
            if(prevOlderRev.getSubsequentCommitIdList() != null && prevOlderRev.getSubsequentCommitIdList().size() > 0) {
                for(String subsequentCommitId : prevOlderRev.getSubsequentCommitIdList()) {
                    if(subsequentCommitId.equalsIgnoreCase(currentRevision.getCommitId())) {
                        return prevOlderRev;
                    }
                }
            }
        }
        return null;
    }
}
