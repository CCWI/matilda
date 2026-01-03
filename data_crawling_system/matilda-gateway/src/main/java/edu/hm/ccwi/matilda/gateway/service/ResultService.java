package edu.hm.ccwi.matilda.gateway.service;

import org.springframework.http.ResponseEntity;

public interface ResultService {

    /**
     *
     * @param matildaRequestId
     * @return
     */
    ResponseEntity<Object> getProjectProfile(String matildaRequestId);

    /**
     *
     * @param matildaRequestId
     * @return
     */
    ResponseEntity<Object> requestResultCard(String matildaRequestId);

    /**
     *
     * @param matildaProjectId
     * @return
     */
    ResponseEntity<Object> requestResultCardTest(String matildaProjectId);

}
