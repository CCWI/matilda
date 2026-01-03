package edu.hm.ccwi.matilda.korpus.classification;

public class EvaluationResult {

    private String modelname;

    private int crossValidationK;

    private int evaluationSetId;

    private double accuracy;

    private double precision;

    private double recall;

    private double f1;

    private double falsePositiveRate;

    private double truePositiveRate;

    public EvaluationResult() {
    }

    public EvaluationResult(double accuracy, double precision, double recall, double f1, double falsePositiveRate, double truePositiveRate) {
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.f1 = f1;
        this.falsePositiveRate = falsePositiveRate;
        this.truePositiveRate = truePositiveRate;
    }

    public String getModelname() {
        return modelname;
    }

    public void setModelname(String modelname) {
        this.modelname = modelname;
    }

    public int getCrossValidationK() {
        return crossValidationK;
    }

    public void setCrossValidationK(int crossValidationK) {
        this.crossValidationK = crossValidationK;
    }

    public int getEvaluationSetId() {
        return evaluationSetId;
    }

    public void setEvaluationSetId(int evaluationSetId) {
        this.evaluationSetId = evaluationSetId;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public double getRecall() {
        return recall;
    }

    public void setRecall(double recall) {
        this.recall = recall;
    }

    public double getF1() {
        return f1;
    }

    public void setF1(double f1) {
        this.f1 = f1;
    }

    public double getFalsePositiveRate() {
        return falsePositiveRate;
    }

    public void setFalsePositiveRate(double falsePositiveRate) {
        this.falsePositiveRate = falsePositiveRate;
    }

    public double getTruePositiveRate() {
        return truePositiveRate;
    }

    public void setTruePositiveRate(double truePositiveRate) {
        this.truePositiveRate = truePositiveRate;
    }

    @Override
    public String toString() {
        return "EvaluationResult{" +
                "modelname='" + modelname + '\'' +
                ", crossValidationK=" + crossValidationK +
                ", evaluationSetId=" + evaluationSetId +
                ", accuracy=" + accuracy +
                ", precision=" + precision +
                ", recall=" + recall +
                ", f1=" + f1 +
                ", falsePositiveRate=" + falsePositiveRate +
                ", truePositiveRate=" + truePositiveRate +
                '}';
    }
}
