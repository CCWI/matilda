package edu.hm.ccwi.matilda.korpus.libsim;

import java.io.Serializable;
import java.util.Objects;

public class LibSimResult implements Serializable {

    private boolean success;
    private String prediction;
    private String percent;
    private String token;

    public LibSimResult() {
    }

    public LibSimResult(LibSimResult libSimResult) {
        this.success = libSimResult.isSuccess();
        this.prediction = libSimResult.getPrediction();
        this.percent = libSimResult.getPercent();
        this.token = libSimResult.getToken();
    }

    public LibSimResult(boolean success, String prediction, String percent, String token) {
        this.success = success;
        this.prediction = prediction;
        this.percent = percent;
        this.token = token;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getPrediction() {
        return prediction;
    }

    public void setPrediction(String prediction) {
        this.prediction = prediction;
    }

    public String getPercent() {
        return percent;
    }

    public void setPercent(String percent) {
        this.percent = percent;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibSimResult that = (LibSimResult) o;
        return Objects.equals(prediction, that.prediction) && Objects.equals(percent, that.percent) && Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(prediction, percent, token);
    }

    @Override
    public String toString() {
        return "LibSimResult{" +
                "success=" + success +
                ", prediction='" + prediction + '\'' +
                ", percent='" + percent + '\'' +
                ", token='" + token + '\'' +
                '}';
    }
}
