package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.FolderPaths;
import com.bdea.grp2.lambda.model.DfRepository;
import com.bdea.grp2.lambda.model.FileRepository;
import com.bdea.grp2.lambda.model.TfRepository;
import com.bdea.grp2.lambda.model.TfidfRepository;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

@Service
public class SparkService {

    private final TfRepository tfRepository;
    private final DfRepository dfRepository;
    private final TfidfRepository tfidfRepository;
    private final FileRepository fileRepository;

    private SparkSession sparkSession;

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

    }

    public void newFileJob(MultipartFile file) {
        Dataset<Row> df = this.sparkSession.read().text(FolderPaths.TXT_FILES + "/" + file.getOriginalFilename());
        df.show();
    }
}
