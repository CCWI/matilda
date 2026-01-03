package edu.hm.ccwi.matilda.base.util;

public class ProgressHandler {

    private int maxAmount;
    private int progressAmount;

    public ProgressHandler(int maxAmount) {
        this.maxAmount = maxAmount;
    }

    public int incrementProgress() {
        this.progressAmount = this.progressAmount + 1;
        return this.progressAmount;
    }

    public int incrementProgress(int i) {
        this.progressAmount = this.progressAmount + i;
        return this.progressAmount;
    }

    public int decrementProgress() {
        this.progressAmount = this.progressAmount - 1;
        return this.progressAmount;
    }

    public int getMaxAmount() {
        return this.maxAmount;
    }

    public int getProgressAmount() {
        return this.progressAmount;
    }
}
