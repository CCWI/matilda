package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.base.model.enumeration.RevisionType;
import edu.hm.ccwi.matilda.base.util.GitCommons;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDocumentation;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRepository;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledRevisionRepository;
import edu.hm.ccwi.matilda.persistence.mongo.repo.CrawledSoftwareRepository;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.BsonSerializationException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

/**
 * Extract data from project including dependency-details and documentation.
 *
 * @author Max.Auch
 */
@Service
public class UtilService {

    private static final Logger LOG = LoggerFactory.getLogger(UtilService.class);

    private final CrawledSoftwareRepository swRepo;
    private final CrawledDocumentationRepository docRepo;
    private final CrawledRevisionRepository revRepo;

    @Value("${matilda.dataextractor.ignore.existingRepos}")
    private boolean ignoreExistingRepos;

    public UtilService(CrawledSoftwareRepository swRepo, CrawledDocumentationRepository docRepo, CrawledRevisionRepository revRepo) {
        this.swRepo = swRepo;
        this.docRepo = docRepo;
        this.revRepo = revRepo;
    }

    List<String> removeDuplicatesInList(List<String> l) {
        if(l != null && !l.isEmpty()) {
            Set<String> s = new TreeSet<>(l);
            return Arrays.asList(s.toArray(String[]::new));
        } else {
            return l;
        }
    }

    /**
     * Get Type of commit: HEAD, TAG or COMMIT.
     */
    RevisionType getCommitType(Git git, String commitId) throws GitAPIException {
        // check for BRANCH/HEAD (if commit is HEAD&TAG, then should be HEAD)
        for (Ref branch : git.branchList().call()) {
            if (StringUtils.equalsIgnoreCase(GitCommons.extractHashFromCommitIdString(branch.getObjectId().toString()), commitId)) {
                return RevisionType.head;
            }
        }

        // check for rev-type TAG
        for (Ref tag : git.tagList().call()) {
            if (StringUtils.equalsIgnoreCase(GitCommons.extractHashFromCommitIdString(tag.getObjectId().toString()), commitId)) {
                return RevisionType.tag;
            }
        }
        return RevisionType.commit;
    }

    void cleanUpAllReferences(List<CrawledRevision> revisionList, Map<String, CrawledRevision> removableMarkedRevisions) {
        revisionList.removeIf(Objects::isNull);
        if(MapUtils.isNotEmpty(removableMarkedRevisions)) {
            for (CrawledRevision removableCR : removableMarkedRevisions.values()) {
                for (CrawledRevision olderRev : revisionList) {
                    if (olderRev.getSubsequentCommitIdList().contains(removableCR.getCommitId())) { // find parent
                        for (String subseqIdOfChild : removableCR.getSubsequentCommitIdList()) { // add subIds to parent
                            olderRev.getSubsequentCommitIdList().add(subseqIdOfChild);
                        }
                        olderRev.getSubsequentCommitIdList().remove(removableCR.getCommitId()); // remove id ref from parents
                    }
                }
                revisionList.remove(removableCR);
            }
        }
    }

    /**
     * Get current CrawledRepository from MongoDB.
     */
    CrawledRepository getCrawledRepositoryFromMongoDB(CrawledRepository crawledRepository) {
        if (swRepo.existsById(crawledRepository.getId())) {
            return swRepo.findById(crawledRepository.getId()).orElse(null);
        } else {
            return ignoreExistingRepos ? null : crawledRepository;
        }
    }

    /**
     * Save Revision and Documentation to MongoDB
     */
    void saveRevResultsToDB(String repoProj, CrawledRevision rev, CrawledDocumentation doc) {
        try { // save Revision/Documentation to MongoDB
            revRepo.save(rev);
            docRepo.save(doc);
        } catch (BsonSerializationException e) {
            LOG.error("MongoDB Bson serialization failed for doc in {}/{}: {}", repoProj, rev.getCommitId(), e);
        }
    }

    boolean directoryValid(File dir) {
        return dir != null && StringUtils.isNotEmpty(dir.getName()) && dir.exists() && dir.isDirectory();
    }
}