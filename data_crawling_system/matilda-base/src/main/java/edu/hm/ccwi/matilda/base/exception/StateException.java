package edu.hm.ccwi.matilda.base.exception;

public class StateException extends Exception {

    public StateException(String msg) {
        super(msg);
    }

    public StateException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
