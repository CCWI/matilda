package edu.hm.ccwi.matilda.base.exception;

public class RepositoryAlreadyExistsException extends Exception {

    public RepositoryAlreadyExistsException(String msg) {
        super(msg);
    }

    public RepositoryAlreadyExistsException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
