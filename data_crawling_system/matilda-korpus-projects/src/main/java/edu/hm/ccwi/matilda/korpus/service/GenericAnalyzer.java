package edu.hm.ccwi.matilda.korpus.service;

import edu.hm.ccwi.matilda.korpus.model.CrawledRepository;

/**
 * @deprecated This class is obsolete and not fully implemented.
 * Use specific analyzer implementations instead.
 * Scheduled for removal in future versions.
 */
@Deprecated
public class GenericAnalyzer {

    private GeneralKorpusResults results;

    public GenericAnalyzer() {
        results = new GeneralKorpusResults();
    }

    public boolean analyze(CrawledRepository crawledRepository) {
        // Placeholder implementation - not functional
        return true;
    }

    public GeneralKorpusResults getResults() {
        return results;
    }

    public void setResults(GeneralKorpusResults results) {
        this.results = results;
    }
}
