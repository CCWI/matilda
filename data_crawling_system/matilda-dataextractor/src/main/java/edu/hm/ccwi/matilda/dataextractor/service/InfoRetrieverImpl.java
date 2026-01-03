package edu.hm.ccwi.matilda.dataextractor.service;

import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDependency;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledDocumentation;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledProject;
import edu.hm.ccwi.matilda.persistence.mongo.model.CrawledRevision;
import edu.hm.ccwi.matilda.dataextractor.service.nlp.LanguageDetection;
import edu.hm.ccwi.matilda.dataextractor.service.reader.MarkdownReader;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class InfoRetrieverImpl implements InfoRetriever {

    private static final Logger LOG = LoggerFactory.getLogger(InfoRetrieverImpl.class);

    private final LanguageDetection languageDetection;
    private final MarkdownReader mdr;

    public InfoRetrieverImpl(LanguageDetection languageDetection, MarkdownReader mdr) {
        this.languageDetection = languageDetection;
        this.mdr = mdr;
    }

    public CrawledDocumentation extractDocumentation(String commitId, String revisionPath) throws Exception {
        List<File> docFileList = mdr.findAllMDInProject(revisionPath);
        if (docFileList == null || docFileList.size() == 0) {
            return null;
        }

        CrawledDocumentation cDoc = new CrawledDocumentation(commitId, mdr.convertMarkdownToHtml(docFileList));

        // Analyze all Readmes
        cDoc.setLanguage(languageDetection.getLanguageCode(String.join(";", cDoc.getDocumentationFileList())));

        return cDoc;
    }

    public boolean isMDInProject(String projectUri) {
        return mdr.isMDInProject(projectUri);
    }

    /**
     * Filter out all dependencies to project-own artifacts by its groupId.
     */
    public List<CrawledRevision> filterFirstPartyProjectDependencies(List<CrawledRevision> revList) {
        if (!revList.isEmpty()) {
            LOG.info("    Start cleanup by filtering first-party dependencies for {} revisions", revList.size());
            for (CrawledRevision rev : revList) {
                for (CrawledProject cp : rev.getProjectList()) {
                    LOG.trace("     -> check on first party dependencies for {}:{}: {}", rev.getCommitId(), cp.getProjectName(),
                            cp.getDependencyList().size());
                    String projectGroup = cp.getProjectGroup();
                    cp.getDependencyList().removeIf(dep -> {
                        return valideOnFirstPartyProject(projectGroup, dep);
                    });
                    LOG.trace("     -> after check on first party dependencies for {}:{}: {}", rev.getCommitId(), cp.getProjectName(),
                            cp.getDependencyList().size());
                }
            }
        }
        return revList;
    }

    private boolean valideOnFirstPartyProject(String projectGroup, CrawledDependency dep) {
        LOG.trace("       -> validate for FirstParty -> projectGroup: {}", projectGroup);
        boolean isFirstParty = StringUtils.isEmpty(projectGroup) || dep == null || StringUtils.isEmpty(dep.getGroup())
                || projectGroup.contains(dep.getGroup()) || dep.getGroup().contains(projectGroup);
        LOG.trace("       -> validate isFirstParty: {}", isFirstParty);
        if(dep != null) {
            LOG.trace("       -> validate dependency: {} : {}", dep.getGroup(), dep.getArtifact());
        } else {
            LOG.trace("       -> validate dependency: IT IS null");
        }
        return isFirstParty;
    }

    /**
     * Filter out all dependencies which include unresolved placeholder in GA.
     */
    public List<CrawledRevision> filterUnresolvedProjectDependencies(List<CrawledRevision> revList) {
        if (!revList.isEmpty()) {
            LOG.info("    Start cleanup by filtering unresolved dependencies for {} revisions", revList.size());
            for (CrawledRevision rev : revList) {
                for (CrawledProject cp : rev.getProjectList()) {
                    cp.getDependencyList().removeIf(dep -> {
                            boolean isUnresolved = dep.getGroup().contains("${") || dep.getArtifact().contains("${");
                            if (isUnresolved) {
                                LOG.trace("      -> Found unresolvedDependency: {}", dep);
                            }
                            return isUnresolved;
                    });
                    //cp.getDependencyList().removeIf(dep -> dep.getGroup().contains("${") || dep.getArtifact().contains("${"));
                }
            }
        }
        return revList;
    }
}