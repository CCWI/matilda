package edu.hm.ccwi.matilda.korpus;

import edu.hm.ccwi.matilda.korpus.classification.ClassificationProcessor;
import edu.hm.ccwi.matilda.korpus.classification.EvaluationResult;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Deprecated
public class DependencyKorpusAbstractPreTest {


    public static void main(String[] args) throws IOException {
        // Input data: Each row is a bag of words from a sentence or document.
        /*   List<Row> data = Arrays.asList(
                RowFactory.create(Arrays.asList("a", "b", "c", "d")),
                RowFactory.create(Arrays.asList("a", "b")),
                RowFactory.create(Arrays.asList("c", "d")),
                RowFactory.create(Arrays.asList("d", "e")),
                RowFactory.create(Arrays.asList("a", "d")),
                RowFactory.create(Arrays.asList("a", "c", "d")),
                RowFactory.create(Arrays.asList("a", "c")),
                RowFactory.create(Arrays.asList("c", "d", "f")),
                RowFactory.create(Arrays.asList("d", "e", "f")),
                RowFactory.create(Arrays.asList("a", "d", "e"))
        );
        */
        /*  0 = Android Packages
            1 = Groovy Plugins
            2 = Maven Plugins
            3 = Database
            4 = matrix-libs
         */
        List<Row> data = Arrays.asList(
                RowFactory.create(0, Arrays.asList("android")),
                RowFactory.create(0, Arrays.asList("android", "app")),
                RowFactory.create(0, Arrays.asList("android", "build")),
                RowFactory.create(0, Arrays.asList("android", "app", "build")),
                RowFactory.create(0, Arrays.asList("android", "sdk")),
                RowFactory.create(0, Arrays.asList("android", "mobile", "api")),
                RowFactory.create(0, Arrays.asList("app", "android")),
                RowFactory.create(0, Arrays.asList("build", "android")),
                RowFactory.create(0, Arrays.asList("app", "build", "android")),
                RowFactory.create(0, Arrays.asList("sdk", "android")),
                RowFactory.create(0, Arrays.asList("mobile", "android", "api")),
                //RowFactory.create(4, Arrays.asList("android", "app")),

                RowFactory.create(1, Arrays.asList("android", "plugin", "groovy")),
                RowFactory.create(1, Arrays.asList("plugin", "groovy")),
                RowFactory.create(1, Arrays.asList("groovy")),
                RowFactory.create(1, Arrays.asList("plugin", "groovy", "build")),
                RowFactory.create(1, Arrays.asList("groovy", "build", "sdk")),
                RowFactory.create(1, Arrays.asList("groovy", "build-system")),
                RowFactory.create(1, Arrays.asList("groovy", "build", "api")),
                RowFactory.create(1, Arrays.asList("groovy", "plugin", "api")),
                RowFactory.create(1, Arrays.asList("plugin", "groovy", "android")),
                RowFactory.create(1, Arrays.asList("groovy", "plugin")),
                RowFactory.create(1, Arrays.asList("groovy")),
                RowFactory.create(1, Arrays.asList("plugin", "build", "groovy")),
                RowFactory.create(1, Arrays.asList("sdk", "groovy", "build")),
                RowFactory.create(1, Arrays.asList("build-system", "groovy")),
                RowFactory.create(1, Arrays.asList("api", "groovy", "build")),
                RowFactory.create(1, Arrays.asList("plugin", "api", "groovy")),

                RowFactory.create(2, Arrays.asList("android", "plugin", "maven")),
                RowFactory.create(2, Arrays.asList("plugin", "maven")),
                RowFactory.create(2, Arrays.asList("maven")),
                RowFactory.create(2, Arrays.asList("maven", "build", "sdk")),
                RowFactory.create(2, Arrays.asList("maven", "build-system")),
                RowFactory.create(2, Arrays.asList("plugin", "maven", "build")),
                RowFactory.create(2, Arrays.asList("maven", "build", "api")),
                RowFactory.create(2, Arrays.asList("maven", "plugin", "api")),
                RowFactory.create(2, Arrays.asList("plugin", "maven", "android")),
                RowFactory.create(2, Arrays.asList("maven", "plugin")),
                RowFactory.create(2, Arrays.asList("maven")),
                RowFactory.create(2, Arrays.asList("build", "maven", "sdk")),
                RowFactory.create(2, Arrays.asList("build-system", "maven")),
                RowFactory.create(2, Arrays.asList("build", "plugin", "maven")),
                RowFactory.create(2, Arrays.asList("build", "maven", "api")),
                RowFactory.create(2, Arrays.asList("api", "maven", "plugin")),

                RowFactory.create(3, Arrays.asList("android", "h2")),
                RowFactory.create(3, Arrays.asList("h2", "plugin")),
                RowFactory.create(3, Arrays.asList("h2", "database")),
                RowFactory.create(3, Arrays.asList("database", "mobile")),
                RowFactory.create(3, Arrays.asList("database")),
                RowFactory.create(3, Arrays.asList("h2")),
                RowFactory.create(3, Arrays.asList("database", "api")),
                RowFactory.create(3, Arrays.asList("h2", "api")),
                RowFactory.create(3, Arrays.asList("h2", "android")),
                RowFactory.create(3, Arrays.asList("plugin", "h2")),
                RowFactory.create(3, Arrays.asList("database", "h2")),
                RowFactory.create(3, Arrays.asList("mobile", "database")),
                RowFactory.create(3, Arrays.asList("database")),
                RowFactory.create(3, Arrays.asList("h2")),
                RowFactory.create(3, Arrays.asList("api", "database")),
                RowFactory.create(3, Arrays.asList("api", "h2")),

                RowFactory.create(4, Arrays.asList("transform", "matrix"))
        );

        String[] bow = new String[]{"android", "app", "build", "sdk", "mobile", "api", "plugin", "groovy", "build-system", "maven", "h2", "database"};

        ClassificationProcessor nbccv = new ClassificationProcessor();
        List<EvaluationResult> evaluationResults = nbccv.classificationAndEvaluation(data, bow, new double[]{0.5, 0.5}, System.currentTimeMillis());

        for (EvaluationResult evaluationResult : evaluationResults) {
            System.out.println("Test set evaluation result = " + evaluationResult);
        }
    }
}
