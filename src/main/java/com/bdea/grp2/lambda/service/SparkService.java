package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.model.Df;
import com.bdea.grp2.lambda.model.DfRepository;
import com.bdea.grp2.lambda.model.TermId;
import com.bdea.grp2.lambda.model.Tf;
import com.bdea.grp2.lambda.model.Tfidf;
import com.kennycason.kumo.WordFrequency;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import scala.Tuple2;
import scala.Tuple3;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.log;

@Slf4j
@Service
public class SparkService implements Serializable {

    private transient final DfRepository dfRepository;

    private transient final TagCloudService tagCloudService;
    private transient final TfService tfService;

    private final SparkSession sparkSession;
    private final Properties connectionProperties;

    @Autowired
    public SparkService(DfRepository dfRepository, TagCloudService tagCloudService, TfService tfService) {
        this.dfRepository = dfRepository;
        this.tagCloudService = tagCloudService;
        this.tfService = tfService;

        this.sparkSession = SparkSession.builder()
                .appName("lambda-session").master("local[*]").getOrCreate();
        this.connectionProperties = new Properties();
        connectionProperties.put("driver", "org.postgresql.Driver");
        connectionProperties.put("user", "grp2");
        connectionProperties.put("password", "grp2");
        this.sparkSession.sparkContext().setLogLevel("ERROR");
    }

    @PreDestroy
    private void destroySparkSession() {
        this.sparkSession.close();
    }

    public void newFileJob(String fileName, List<Tf> tfs) throws Exception {
        try {
            Dataset<Row> dfData = sparkSession.read().jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties);

            // Get file count for idf calculation
            Dataset<Row> files = sparkSession.read().jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.file", this.connectionProperties);
            long fileCount = files.count();

            // Create tf dataframe and view
            Dataset<Row> tf = sparkSession.createDataFrame(tfs, Tf.class);
            // Flatten TermId object
            tf = tf.select(tf.col("termId.fileName").as("fileName"), tf.col("termId.term").as("term"), tf.col("tf"));
            tf.createOrReplaceTempView("tf");
            // add initial df column
            tf = tf.withColumn("df", lit(1)).repartition(tf.col("term")).sortWithinPartitions("term");
            // Combine with df table values
            tf = tf.unionByName(dfData, true);
            // Create view for df calculation
            tf.createOrReplaceTempView("dfCalc");

            Dataset<Row> dfCalc = this.sparkSession.sql("SELECT term, MAX(df) as df FROM dfCalc GROUP BY term");
            dfCalc.show();

            // Write new df to db
            //dfCalc.write().mode("overwrite").option("truncate", "true").option("encoding", "UTF-8").jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties);
            this.sparkSession.catalog().dropTempView("dfCalc");
            // Create new view with updated data
            dfCalc.createOrReplaceTempView("df");

            // Create dataset with tf and calculated df
            Dataset<Row> tfAndDf = this.sparkSession.sql("SELECT tf.fileName, tf.term, tf.tf, df.df FROM tf JOIN df ON df.term = tf.term");
            tfAndDf.show();

            // Calculate inverse document frequency smooth (vgl. https://en.wikipedia.org/wiki/Tf%E2%80%93idf) to prevent tf-idf of 0
            tfAndDf = tfAndDf.withColumn("fileCount", lit(fileCount));
            tfAndDf = tfAndDf.withColumn("divided", tfAndDf.col("fileCount").divide(tfAndDf.col("df").plus(lit(1))));
            tfAndDf = tfAndDf.withColumn("idf", log(tfAndDf.col("divided")));
            tfAndDf = tfAndDf.withColumn("idf", tfAndDf.col("idf").plus(lit(1)));
            Dataset<Row> tfAndDfAndIdf = tfAndDf.drop("fileCount", "divided");

            // Calculate tf-idf
            Dataset<Row> tfIdf = tfAndDfAndIdf.withColumn("tfidf", tfAndDfAndIdf.col("tf").multiply(tfAndDfAndIdf.col("idf")));
            //tfIdf.show();
            // Multiply to get values > 1 for tag cloud generation
            tfIdf = tfIdf.withColumn("tfidf", tfIdf.col("tfidf").multiply(10000));
            tfIdf = tfIdf.select(tfIdf.col("term"), tfIdf.col("tfidf"));
            Dataset<Tfidf> tfIdfTyped = tfIdf.as(Encoders.bean(Tfidf.class));
            tfIdf.show();
            List<Tfidf> rows = tfIdfTyped.collectAsList();
            // Build tag cloud data
            List<WordFrequency> wordFrequencies = new ArrayList<>();
            rows.forEach(r -> wordFrequencies.add(new WordFrequency(r.getTerm(), (int) r.getTfidf())));
            this.tagCloudService.createTagCloud(wordFrequencies, fileName);
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new Exception("Spark job failed with error", e);
        }
    }

    public boolean runBatchJob(List<File> files) throws Exception {
        try {
            // Process all files in the list (with Spark RDD)
            JavaSparkContext javaSparkContext = JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
            JavaRDD<File> filesRdd = javaSparkContext.parallelize(files, files.size());

            TfService tfService = this.tfService;
            JavaRDD<Tf> termFrequenciesFlat = filesRdd
                    .flatMap(file -> {
                        byte[] content = Files.readAllBytes(file.toPath());
                        List<Tf> tfs = tfService.extractTfsFromFile(content, file.getName());
                        log.info("Finished file {} analysis", file.getName());
                        return tfs.iterator();
                    });

            JavaPairRDD<String, List<Tf>> termFrequenciesByFileList = termFrequenciesFlat.mapToPair(tf -> {
                List<Tf> tfList = new ArrayList<>();
                tfList.add(tf);
                return new Tuple2<>(tf.getTermId().getFileName(), tfList);
            }).reduceByKey((l1, l2) -> {
                List<Tf> combined = new ArrayList<>();
                combined.addAll(l1);
                combined.addAll(l2);
                return combined;
            });

            // calc df
            JavaPairRDD<String, Integer> termTermCount = termFrequenciesFlat.mapToPair(tf -> new Tuple2<>(tf.getTermId().getTerm(), tf.getTermCount()));
            Map<String, Long> df = termTermCount.countByKey();
            List<Df> dfObjects = df.entrySet().stream().map(k -> Df.builder().term(k.getKey()).df(Math.toIntExact(k.getValue())).build())
                    .collect(Collectors.toList());
            this.dfRepository.saveAll(dfObjects);
            log.info("Finished DF calc");

            List<Tuple2<String, List<Tf>>> collectedTfsByFile = termFrequenciesByFileList.collect();
            for (Tuple2<String, List<Tf>> tfs : collectedTfsByFile) {
                this.newFileJob(tfs._1(), tfs._2());
            }

            // Global tag cloud calculation
            int globalWordCount = termFrequenciesFlat.map(tf -> tf.getTermCount()).reduce(Integer::sum);
            JavaPairRDD<String, Float> globalTfSum = termTermCount
                    .reduceByKey(Integer::sum)
                    .mapToPair(tuple -> new Tuple2<>(tuple._1, ((float) tuple._2 / (float) globalWordCount) / (float) df.get(tuple._1)));
            log.info("Finished global tag cloud calc");

            // Create the global tag cloud
            List<Tuple2<String, Float>> globalWordFrequenciesList = globalTfSum.collect();
            List<WordFrequency> globalWordFrequencies = new ArrayList<>();
            globalWordFrequenciesList.forEach(tuple -> globalWordFrequencies.add(new WordFrequency(tuple._1, Math.round(tuple._2 * 10000))));
            this.tagCloudService.createTagCloud(globalWordFrequencies, "global_tag_cloud");
            log.info("Finished global tag cloud creation");
            return true;
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new Exception("Spark job failed with error", e);
        }
    }
}
