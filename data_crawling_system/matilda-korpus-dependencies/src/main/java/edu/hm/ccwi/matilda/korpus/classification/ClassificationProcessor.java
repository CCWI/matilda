package edu.hm.ccwi.matilda.korpus.classification;

import edu.hm.ccwi.matilda.korpus.util.CsvUtils;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.ml.*;
import org.apache.spark.ml.classification.*;
import org.apache.spark.ml.evaluation.MulticlassClassificationEvaluator;
import org.apache.spark.ml.feature.*;
import org.apache.spark.mllib.evaluation.MulticlassMetrics;
import org.apache.spark.sql.*;
import org.apache.spark.sql.types.*;
import org.apache.spark.util.Utils;
import scala.Tuple2;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.apache.spark.sql.functions.col;

public class ClassificationProcessor {

    Map<Integer, String> indexMapForCategories;

    public ClassificationProcessor() {
    }

    public ClassificationProcessor(Map<Integer, String> indexMapForCategories) {
        this.indexMapForCategories = indexMapForCategories;
    }

    public NaiveBayesModel trainNaiveBayesClassificationModel(List<Row> data, String[] bow) throws IOException {
        SparkSession spark = createSparkContext();
        Dataset<Row> vectorModelData = createVectorModelDataset(data, bow, spark);

        NaiveBayesModel nbModel = trainNaiveBayesModel(vectorModelData);

        return nbModel;
    }

    public Dataset<Row> classifyDataByModel(NaiveBayesModel nbModel, String[] bow, List<Row> gaTaggedOnlyEntry) {
        SparkSession spark = createSparkContext();

        StructType schema = new StructType(new StructField[] {
                new StructField("ga", DataTypes.StringType, false, Metadata.empty()),
                new StructField("text", new ArrayType(DataTypes.StringType, true), false, Metadata.empty())
        });

        CountVectorizerModel cvm = new CountVectorizerModel(bow).setInputCol("text").setOutputCol("features").setBinary(true);
        Dataset<Row> vectorModelData = cvm.transform(spark.createDataFrame(gaTaggedOnlyEntry, schema));
        Dataset<Row> result = nbModel.setFeaturesCol("features").transform(vectorModelData);
        result.select("features", "ga", "prediction", "text").show(25,false);

        return result;
    }

    public List<EvaluationResult> classificationAndEvaluation(List<Row> data, String[] bow, double[] split, long jobTimeMillis) throws IOException {
        SparkSession spark = createSparkContext();
        Dataset<Row> vectorModelData = createVectorModelDataset(data, bow, spark);

        // Split the data into train and test
        Dataset<Row>[] dataSplits = vectorModelData.randomSplit(split, Utils.random().nextLong());

        List<EvaluationResult> evaluationResults = new ArrayList<>();

        for (int iterationCounter = 0; iterationCounter < dataSplits.length; iterationCounter++) {
            Dataset<Row> train = null;
            for (int j = 0; j < dataSplits.length; j++) {
                if(j != iterationCounter) {
                    if(train == null) {
                        train = dataSplits[j];
                    } else {
                        train = train.union(dataSplits[j]);
                    }
                }
            }

            Dataset<Row> test = dataSplits[iterationCounter];

            // ### Multinomial Naive Bayes #############################################################################
            EvaluationResult nbEvaluationResult = classifyByNaiveBayes(train, test, iterationCounter, jobTimeMillis);
            nbEvaluationResult.setCrossValidationK(dataSplits.length);
            evaluationResults.add(nbEvaluationResult);

            //### Multinomial logistic regression ######################################################################
            EvaluationResult lrEvaluationResult = classifyByMultinomialLogisticRegression(train, test, iterationCounter, jobTimeMillis);
            lrEvaluationResult.setCrossValidationK(dataSplits.length);
            evaluationResults.add(lrEvaluationResult);

            //### Random forest ########################################################################################
            //EvaluationResult rfEvaluationResult = classifyByRandomForest(train, test, iterationCounter, jobTimeMillis);
            //rfEvaluationResult.setCrossValidationK(dataSplits.length);
            //evaluationResults.add(rfEvaluationResult);
        }

        return evaluationResults;
    }

    SparkSession createSparkContext() {
        SparkSession spark = SparkSession.builder().appName("pipelineExample").master("local").getOrCreate();
        spark.sparkContext().setLogLevel("OFF");
        return spark;
    }

    Dataset<Row> createVectorModelDataset(List<Row> data, String[] bow, SparkSession spark) {
        StructType schema = new StructType(new StructField [] {
                new StructField("label", DataTypes.IntegerType, false, Metadata.empty()),
                new StructField("text", new ArrayType(DataTypes.StringType, true), false, Metadata.empty())
        });

        //shuffle data
        Collections.shuffle(data);

        // Define CountVectorizerModel with a-priori vocabulary
        CountVectorizerModel cvm = new CountVectorizerModel(bow).setInputCol("text").setOutputCol("features").setBinary(true);
        Dataset<Row> vectorModelData = cvm.transform(spark.createDataFrame(data, schema));
        return vectorModelData;
    }

    private void classifyByGBT(Dataset<Row> train, Dataset<Row> test) {
        StringIndexerModel labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").fit(train);
        // Automatically identify categorical features, and index them. Set maxCategories so features with > 4 distinct values are treated as continuous.
        VectorIndexerModel featureIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setMaxCategories(4).fit(train);
        GBTClassifier gbt = new GBTClassifier().setLabelCol("label").setFeaturesCol("features").setMaxIter(10);
        // Convert indexed labels back to original labels.
        IndexToString labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictedLabel").setLabels(labelIndexer.labels());
        // Chain indexers and GBT in a Pipeline.
        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[] {labelIndexer, featureIndexer, gbt, labelConverter});
        // Train model. This also runs the indexers.
        Dataset<Row> gbtPredictions = pipeline.fit(train).transform(test);
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("accuracy");
        double accuracyGBT = evaluator.evaluate(gbtPredictions);
    }

    private void classifyByMultilayerPerceptron(Dataset<Row> train, Dataset<Row> test) {
        // specify layers for the neural network: input layer of size 4 (features), two intermediate of size 5 and 4 and output of size 3 (classes)
        int[] layers = new int[] {4, 5, 4, 3};
        StringIndexerModel labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").setHandleInvalid("skip").fit(train);
        // Automatically identify categorical features, and index them. Set maxCategories so features with > 4 distinct values are treated as continuous.
        VectorIndexerModel featureIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setHandleInvalid("skip").setMaxCategories(4).fit(train);
        // create the trainer and set its parameters
        MultilayerPerceptronClassifier trainer = new MultilayerPerceptronClassifier().setLayers(layers).setFeaturesCol("features").setLabelCol("label")
                .setBlockSize(128).setSeed(1234L).setMaxIter(100);
        // Convert indexed labels back to original labels.
        IndexToString labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictedLabel").setLabels(labelIndexer.labels());
        // Chain indexers and GBT in a Pipeline.
        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[] {labelIndexer, featureIndexer, trainer, labelConverter});
        // Train model. This also runs the indexers.
        Dataset<Row> mpcPredictions = pipeline.fit(train).transform(test);
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("accuracy");
        double accuracyMPC = evaluator.evaluate(mpcPredictions);
        //...//
        trainer.set("handleInvalid", "skip");
        MultilayerPerceptronClassificationModel mpModel = trainer.fit(train);
        Dataset<Row> result = mpModel.transform(test);
        Dataset<Row> predictionAndLabels = result.select("prediction", "label");
        MulticlassClassificationEvaluator mpEvaluator = new MulticlassClassificationEvaluator().setMetricName("accuracy");
    }

    private EvaluationResult classifyByNaiveBayes(Dataset<Row> train, Dataset<Row> test, int iterationCounter,
                                                  long jobTimeMillis) throws IOException {
        NaiveBayesModel nbModel = trainNaiveBayesModel(train);

        nbModel.write().overwrite().save(jobTimeMillis + "/trainedModel/spark-naive-bayes-model-" + iterationCounter);
        //nbModel = NaiveBayesModel.load("matilda-korpus-dependencies/trainedModel/spark-naive-bayes-model");
        Dataset<Row> nbPredictions = nbModel.transform(test); // Select example rows to display.
        EvaluationResult evaluationResult = evaluate("Naive Bayes", test, nbModel, nbPredictions);
        evaluationResult.setModelname("naive bayes");
        evaluationResult.setEvaluationSetId(iterationCounter+1);

        // ###################
        // ##### EXPORT: #####
        // ###################
        nbPredictions
                .withColumn("text", col("text").cast("string"))
                .withColumn("features", col("features").cast("string"))
                .withColumn("rawPrediction", col("rawPrediction").cast("string"))
                .withColumn("probability", col("probability").cast("string"))
                .coalesce(1).write().csv(jobTimeMillis + File.separator + "nbPredictions-"+ iterationCounter +".csv");
        return evaluationResult;
    }

    private NaiveBayesModel trainNaiveBayesModel(Dataset<Row> train) {
        NaiveBayes nb = new NaiveBayes().setFeaturesCol("features").setLabelCol("label");
        NaiveBayesModel nbModel = nb.fit(train); // train the model
        return nbModel;
    }

    private double classifyByDecisionTree(Dataset<Row> train, Dataset<Row> test, int iterationCounter) throws IOException {
        StringIndexerModel labelIndexer = new StringIndexer().setInputCol("label").setOutputCol("indexedLabel").setHandleInvalid("skip").fit(train);
        VectorIndexerModel featureIndexer = new VectorIndexer().setInputCol("features").setOutputCol("indexedFeatures").setMaxCategories(50) // features with > 50 distinct values are treated as continuous.
                .setHandleInvalid("skip").fit(train); // Train a DecisionTree model.
        DecisionTreeClassifier dt = new DecisionTreeClassifier().setLabelCol("indexedLabel").setFeaturesCol("indexedFeatures");
        IndexToString labelConverter = new IndexToString().setInputCol("prediction").setOutputCol("predictedLabel").setLabels(labelIndexer.labels());
        // Chain indexers and tree in a Pipeline.
        Pipeline pipeline = new Pipeline().setStages(new PipelineStage[]{labelIndexer, featureIndexer, dt, labelConverter});
        PipelineModel dtModel = pipeline.fit(train); // Train model. This also runs the indexers.
        dtModel.write().overwrite().save("matilda-korpus-dependencies/trainedModel/spark-decision-tree-model-" + iterationCounter);
        Dataset<Row> dtPredictions = dtModel.transform(test); // Make predictions.
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator()
                .setLabelCol("label").setPredictionCol("prediction").setMetricName("accuracy");
        return evaluator.evaluate(dtPredictions);
    }

    private EvaluationResult classifyByMultinomialLogisticRegression(Dataset<Row> train, Dataset<Row> test, int iterationCounter,
                                                                     long jobTimeMillis) throws IOException {
        LogisticRegression lr = new LogisticRegression().setFeaturesCol("features").setLabelCol("label")
                .setMaxIter(100).setRegParam(0.1).setElasticNetParam(1.0).setFamily("multinomial");
        LogisticRegressionModel lrModel = lr.fit(train);
        lrModel.write().overwrite().save(jobTimeMillis + "/trainedModel/spark-logistic-regression-model-" + iterationCounter);
        Dataset<Row> lrPredictions = lrModel.transform(test); // Select example rows to display.
        EvaluationResult evaluationResult = evaluate("Logistic Regression", test, lrModel, lrPredictions);
        evaluationResult.setModelname("logistic regression");
        evaluationResult.setEvaluationSetId(iterationCounter+1);

        lrPredictions
                .withColumn("text", col("text").cast("string"))
                .withColumn("features", col("features").cast("string"))
                .withColumn("rawPrediction", col("rawPrediction").cast("string"))
                .withColumn("probability", col("probability").cast("string"))
                .coalesce(1).write().csv(jobTimeMillis + File.separator + "lrPredictions-"+ iterationCounter +".csv");

        return evaluationResult;
    }

    private EvaluationResult classifyByRandomForest(Dataset<Row> train, Dataset<Row> test, int iterationCounter,
                                                    long jobTimeMillis) throws IOException {
        RandomForestClassifier rf = new RandomForestClassifier().setFeaturesCol("features").setLabelCol("label");
        RandomForestClassificationModel rfModel = rf.fit(train);
        rfModel.write().overwrite().save(jobTimeMillis + "/trainedModel/spark-random-forest-model-" + iterationCounter);
        Dataset<Row> rfPredictions = rfModel.transform(test); // Select example rows to display.
        EvaluationResult evaluationResult = evaluate("Random Forest", test, rfModel, rfPredictions);
        evaluationResult.setModelname("random forest");
        evaluationResult.setEvaluationSetId(iterationCounter+1);

        rfPredictions
                .withColumn("text", col("text").cast("string"))
                .withColumn("features", col("features").cast("string"))
                .withColumn("rawPrediction", col("rawPrediction").cast("string"))
                .withColumn("probability", col("probability").cast("string"))
                .coalesce(1).write().csv(jobTimeMillis + File.separator + "rfPredictions-"+ iterationCounter +".csv");

        return evaluationResult;
    }

    /**
     *  #######################
     *  ##### Evaluation: #####
     *  #######################
     *
     * @param test
     * @param model
     * @param predictions
     * @return
     */
    private EvaluationResult evaluate(String classificationModelType, Dataset<Row> test, PredictionModel model, Dataset<Row> predictions) {

        // 1) compute accuracy on the test set
        MulticlassClassificationEvaluator evaluator = new MulticlassClassificationEvaluator().setLabelCol("label")
                .setPredictionCol("prediction").setMetricName("accuracy");
        double accuracy = evaluator.evaluate(predictions);

        // 2) confusion matrix
        JavaRDD<LabeledPoint> training = test.javaRDD().map((Row r) ->
                new LabeledPoint(((Integer) r.getAs("label")).intValue(), r.getAs("features")));
        JavaPairRDD<Object, Object> predictionAndLabels = training.mapToPair(p -> new Tuple2<>(model.predict(p.features()), p.label()));
        MulticlassMetrics metrics = new MulticlassMetrics(predictionAndLabels.rdd());
        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "----------------- START Ergebnisse " + classificationModelType +" --------------------", false);
        CsvUtils.appendToCsv("classificationStats.csv", "Confusionmatrix:", false);

        StringBuilder labelArrayBuilder = new StringBuilder();
        for (double label : metrics.labels()) {
            labelArrayBuilder.append(indexMapForCategories.get((int) label) + " | ");
        }

        CsvUtils.appendToCsv("classificationStats.csv", labelArrayBuilder.toString(), false);

        metrics.confusionMatrix().rowIter().foreach(v -> {
            StringBuilder arrayLine = new StringBuilder();
            for(double d : v.toArray()) { arrayLine.append(d + " | "); }
            CsvUtils.appendToCsv("classificationStats.csv", arrayLine.toString(), false); return null;
        });

        System.out.println("Confusion matrix: \n" + metrics.confusionMatrix());

        CsvUtils.appendToCsv("classificationStats.csv", "", false);
        CsvUtils.appendToCsv("classificationStats.csv", "Precision: " + metrics.weightedPrecision(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "Recall: " + metrics.weightedRecall(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "F1-Measure: " + metrics.weightedFMeasure(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "Accuracy: " + metrics.accuracy(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "FalsePositiveRate: " + metrics.weightedFalsePositiveRate(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "TruePositiveRate: " + metrics.weightedTruePositiveRate(), false);
        CsvUtils.appendToCsv("classificationStats.csv", "----------------- ENDE Ergebnisse " + classificationModelType + " --------------------", true);
        return new EvaluationResult(accuracy, metrics.weightedPrecision(), metrics.weightedRecall(), metrics.weightedFMeasure(), metrics.weightedFalsePositiveRate(), metrics.weightedTruePositiveRate());
    }
}