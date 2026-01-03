package edu.hm.ccwi.matilda.crawler.exception;

public class CrawlerException extends Exception {

    public CrawlerException(String msg) {
        super(msg);
    }

    public CrawlerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
