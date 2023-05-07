package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.model.*;
import com.kennycason.kumo.WordFrequency;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.api.java.JavaSparkContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.apache.spark.sql.functions.lit;
import static org.apache.spark.sql.functions.log;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.PairFlatMapFunction;
import scala.Tuple2;

import java.util.Iterator;

@Slf4j
@Service
public class SparkService {

    private final TfRepository tfRepository;
    private final DfRepository dfRepository;
    private final TfidfRepository tfidfRepository;
    private final FileRepository fileRepository;

    private final TagCloudService tagCloudService;
    private final TfService tfService;

    private SparkSession sparkSession;
    private Properties connectionProperties;

    @Autowired
    public SparkService(TfRepository tfRepository, DfRepository dfRepository, TfidfRepository tfidfRepository,
            FileRepository fileRepository, TagCloudService tagCloudService, TfService tfService) {
        this.tfRepository = tfRepository;
        this.fileRepository = fileRepository;
        this.dfRepository = dfRepository;
        this.tfidfRepository = tfidfRepository;
        this.tagCloudService = tagCloudService;
        this.tfService = tfService;
    }

    @PostConstruct
    private void initSparkSession() {
        this.sparkSession = SparkSession.builder()
                .appName("lambda-session").master("local[*]").getOrCreate();
        this.connectionProperties = new Properties();
        connectionProperties.put("driver", "org.postgresql.Driver");
        connectionProperties.put("user", "grp2");
        connectionProperties.put("password", "grp2");
    }

    @PreDestroy
    private void destroySparkSession() {
        this.sparkSession.close();
    }

    public void newFileJob(String fileName, List<Tf> tfs) throws Exception {
        try {
            Dataset<Row> dfData = sparkSession.read().jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties)
                    .cache(); // VERY IMPORTANT OTHERWISE DATA CANNOT BE WRITTEN FULLY

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

            Dataset<Row> dfCalc = this.sparkSession.sql("SELECT term, SUM(df) as df FROM dfCalc GROUP BY term");
            dfCalc.show();

            // Write new df to db
            dfCalc.write().mode("overwrite").option("truncate", "true").option("encoding", "UTF-8").jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties);
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
    
            JavaPairRDD<String, Tuple2<String, Float>> termFrequencies = filesRdd
                    .flatMapToPair(file -> {
                        byte[] content = Files.readAllBytes(file.toPath());
                        List<Tf> tfs = tfService.extractTfsFromFile(content, file.getName());
                        // recreate the wordcloud for this file
                        // this.newFileJob(file.getName(), tfs);
                        List<Tuple2<String, Tuple2<String, Float>>> result = new ArrayList<>();
                        for (Tf tf : tfs) {
                            result.add(new Tuple2<>(tf.getTermId().getTerm(),
                                    new Tuple2<>(tf.getTermId().getFileName(), tf.getTf())));
                        }
                        return result.iterator();
                    });
            // calc df
            Map<String, Long> df = termFrequencies.mapToPair(tuple -> new Tuple2<>(tuple._1, tuple._2._2)).countByKey();
    
            // Global tag cloud calculation
            JavaPairRDD<String, Float> globalTfSum = termFrequencies
                    .mapToPair(tuple -> new Tuple2<>(tuple._1, tuple._2._2))
                    .reduceByKey(Float::sum)
                    .mapToPair(tuple -> new Tuple2<>(tuple._1, tuple._2 / (float) df.get(tuple._1)));
    
            // Create the global tag cloud
            List<Tuple2<String, Float>> globalWordFrequenciesList = globalTfSum.collect();
            List<WordFrequency> globalWordFrequencies = new ArrayList<>();
            globalWordFrequenciesList.forEach(tuple -> globalWordFrequencies.add(new WordFrequency(tuple._1, Math.round(tuple._2 * 10000))));
            this.tagCloudService.createTagCloud(globalWordFrequencies, "global_tag_cloud");
    
            return true;
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new Exception("Spark job failed with error", e);
        }
    }    

    private void createGlobalTagCloud() {
        // Vorhandenen tfidf-Werte aus der Datenbank lesen
        Dataset<Row> globalTfIdf = sparkSession.read().jdbc("jdbc:postgresql://localhost:5432/lambda-grp2",
                "public.tfidf", this.connectionProperties);

        // Globale TF-Summe für jeden Begriff berechnen
        Dataset<Row> globalTfSum = globalTfIdf.groupBy(globalTfIdf.col("term")).sum("tfidf")
                .withColumnRenamed("sum(tfidf)", "tfidf_sum");

        // Globale Tag-Cloud erstellen
        Dataset<WordFrequency> globalWordFrequencies = globalTfSum.map(
            (MapFunction<Row,WordFrequency>)row -> new WordFrequency(row.getString(0), (int) row.getDouble(1)), Encoders.bean(WordFrequency.class));
        List<WordFrequency> globalWordFrequenciesList = globalWordFrequencies.collectAsList();
        this.tagCloudService.createTagCloud(globalWordFrequenciesList, "global_tag_cloud");
    }
}
