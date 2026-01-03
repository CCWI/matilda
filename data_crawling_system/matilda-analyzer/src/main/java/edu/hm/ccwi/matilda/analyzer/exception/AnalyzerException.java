package edu.hm.ccwi.matilda.analyzer.exception;

public class AnalyzerException extends Exception {

    public AnalyzerException() {}

    public AnalyzerException(String msg) {
        super(msg);
    }

    public AnalyzerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
