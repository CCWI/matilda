package edu.hm.ccwi.matilda.korpus;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.io.IOException;

@Deprecated
public class DependencyKorpusNNEvaluationTest {

    public static void main(String[] args) throws IOException {
        SparkSession spark = SparkSession.builder().appName("pipelineExample").master("local").getOrCreate();
        spark.sparkContext().setLogLevel("OFF");

        String path = "matilda-korpus-dependencies/src/main/resources/10_cross_fold/split_09/predictions.csv";

        JavaRDD<Row> javaRowRDD = spark.read().csv(path).javaRDD();
        JavaPairRDD<Object, Object> predictionAndLabels = javaRowRDD.mapToPair(row -> new Tuple2<>(Double.parseDouble(row.get(0).toString()),
                Double.parseDouble(row.get(1).toString())));

        MulticlassMetrics multiclassMetrics = new MulticlassMetrics(predictionAndLabels.rdd());

        double accuracy = multiclassMetrics.accuracy();
        double weightedPrecision = multiclassMetrics.weightedPrecision();
        double weightedRecall = multiclassMetrics.weightedRecall();
        double weightedFMeasure = multiclassMetrics.weightedFMeasure();

        System.out.println("##### result - accuracy - " + accuracy);
        System.out.println("##### result - precision - " + weightedPrecision);
        System.out.println("##### result - recall - " + weightedRecall);
        System.out.println("##### result - fmeasure - " + weightedFMeasure);

    }
}
