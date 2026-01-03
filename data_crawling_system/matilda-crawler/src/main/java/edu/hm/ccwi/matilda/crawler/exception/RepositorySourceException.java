package edu.hm.ccwi.matilda.crawler.exception;

public class RepositorySourceException extends Exception {

    public RepositorySourceException(String msg) {
        super(msg);
    }

    public RepositorySourceException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
