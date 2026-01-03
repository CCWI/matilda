package edu.hm.ccwi.matilda.gateway.service;

public interface AnalyzeService {

    /**
     *
     * @param url
     * @param matildaId
     * @return
     */
    String analyzeProject(String url, String matildaId) throws Exception;

}
