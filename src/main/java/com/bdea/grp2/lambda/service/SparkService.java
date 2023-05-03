package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.model.*;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

import static org.apache.spark.sql.functions.*;

@Service
public class SparkService {

    private final TfRepository tfRepository;
    private final DfRepository dfRepository;
    private final TfidfRepository tfidfRepository;
    private final FileRepository fileRepository;

    private SparkSession sparkSession;
    private Properties connectionProperties;

    @Autowired
    public SparkService(TfRepository tfRepository, DfRepository dfRepository, TfidfRepository tfidfRepository, FileRepository fileRepository) {
        this.tfRepository = tfRepository;
        this.fileRepository = fileRepository;
        this.dfRepository = dfRepository;
        this.tfidfRepository = tfidfRepository;
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

    public List<Tfidf> newFileJob(MultipartFile file, List<Tf> tfs) throws AnalysisException {
        Dataset<Row> df = sparkSession.read().jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties)
                .cache(); // VERY IMPORTANT OTHERWISE DATA CANNOT BE WRITTEN FULLY
        // Create tf dataframe
        Dataset<Row> tfDf = sparkSession.createDataFrame(tfs, Tf.class);
        // Flatten TermId object
        tfDf = tfDf.select(tfDf.col("termId.fileName").as("fileName"), tfDf.col("termId.term").as("term"), tfDf.col("tf"));
        // add initial df column
        tfDf = tfDf.withColumn("df", lit(1)).repartition(tfDf.col("term")).sortWithinPartitions("term");
        // Combine with df table values
        Dataset<Row> cmbd = tfDf.unionByName(df, true);
        cmbd.createOrReplaceTempView("dfCalc");
        Dataset<Row> dfCalc = this.sparkSession.sql("SELECT term, SUM(df) as df FROM dfCalc GROUP BY term");
        dfCalc.show();
        // Write new df to db
        dfCalc.write().mode("overwrite").option("truncate", "true").option("encoding", "UTF-8").jdbc("jdbc:postgresql://localhost:5432/lambda-grp2", "public.df", this.connectionProperties);
        this.sparkSession.catalog().dropTempView("dfCalc");
//        Dataset<Row> combined = tfDf.join(df, tfDf.col("term").equalTo(df.col("term")), "left")
//                .select(tfDf.col("*"), df.col("df"));
//        combined = combined.na().fill(1);
//        combined.show();
        return null;
    }
}
