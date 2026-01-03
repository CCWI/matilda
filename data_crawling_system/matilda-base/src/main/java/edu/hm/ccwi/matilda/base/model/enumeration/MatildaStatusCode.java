package edu.hm.ccwi.matilda.base.model.enumeration;

import com.google.common.collect.Maps;

import java.util.EnumSet;
import java.util.Map;

public enum MatildaStatusCode {

    RECEIVED_REQUEST_FOR_CRAWLING(110),
    STARTED_CRAWLING(111),
    STARTED_CRAWLING_UPDATE(112),
    STARTED_CRAWLING_RECHECK(113),
    FINISHED_CRAWLING(114),
    RECEIVED_REQUEST_FOR_DATA_EXTRACTION(120),
    FINISHED_DATA_EXTRACTION(121),
    RECEIVED_REQUEST_FOR_ANALYZING_PROJECT(130),
    FINISHED_ANALYZING_PROJECT(131),
    ERROR_GENERAL(900),
    ERROR_PROJECT_NOT_SUPPORTED(901),
    ERROR_ROLLBACK(902);

    private int statuscode;

    MatildaStatusCode(int statuscode) {
        this.statuscode = statuscode;
    }

    private final static Map<Integer, MatildaStatusCode> STATUS_CODE_MAP =
            Maps.uniqueIndex(EnumSet.allOf(MatildaStatusCode.class), MatildaStatusCode::getStatusCode);

    public static MatildaStatusCode getMatildaStatusCode(int statuscode) {
        return STATUS_CODE_MAP.get(statuscode);
    }

    public int getStatusCode() {
        return statuscode;
    }

}