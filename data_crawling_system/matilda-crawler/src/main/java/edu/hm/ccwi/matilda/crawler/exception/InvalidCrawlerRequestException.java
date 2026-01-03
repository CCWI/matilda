package edu.hm.ccwi.matilda.crawler.exception;

public class InvalidCrawlerRequestException extends Exception {

    public InvalidCrawlerRequestException(String msg) {
        super(msg);
    }

    public InvalidCrawlerRequestException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
