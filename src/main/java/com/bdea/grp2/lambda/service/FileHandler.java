package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.FolderPaths;
import com.bdea.grp2.lambda.model.*;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;
import org.apache.spark.sql.AnalysisException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileHandler {
    private final TfRepository tfRepository;
    private final DfRepository dfRepository;
    private final TfidfRepository tfidfRepository;
    private final FileRepository fileRepository;
    private final SparkService sparkService;


    @Autowired
    public FileHandler(TfRepository tfRepository, DfRepository dfRepository, TfidfRepository tfidfRepository, FileRepository fileRepository, SparkService sparkService) {
        this.tfRepository = tfRepository;
        this.fileRepository = fileRepository;
        this.dfRepository = dfRepository;
        this.tfidfRepository = tfidfRepository;
        this.sparkService = sparkService;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(FolderPaths.ROOT));
            Files.createDirectories(Paths.get(FolderPaths.TAG_CLOUDS));
            Files.createDirectories(Paths.get(FolderPaths.TXT_FILES));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public boolean saveTextFile(MultipartFile file) throws IllegalArgumentException {
        if (file == null || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Cannot save null file");
        }
        String name = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (name.contains("..")) {
                throw new IllegalArgumentException("Filename invalid " + name);
            }
            Path path = Paths.get(FolderPaths.TXT_FILES).resolve(name);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to save file: " + name);
        }
        return true;
    }

    public boolean createTagCloud(MultipartFile file) throws IOException, AnalysisException {
        String fileContent = new String(file.getBytes());
        String fileName = file.getOriginalFilename();
        final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
        frequencyAnalyzer.setWordFrequenciesToReturn(300);
        frequencyAnalyzer.setMinWordLength(4);

        List<String> texts = new ArrayList<>();
        texts.add(fileContent);
        final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(texts);
        long wordCount = wordFrequencies.stream().map(WordFrequency::getFrequency).reduce(0,Integer::sum);
        File fileEntity = File.builder().filename(fileName).wordCount(wordCount).build();
        this.fileRepository.save(fileEntity);

        final List<Tf> tfs = new ArrayList<>();
        for (WordFrequency wf : wordFrequencies) {
            TermId id = TermId.builder().fileName(fileName).term(wf.getWord()).build();
            tfs.add(Tf.builder().tf((float) wf.getFrequency() / wordCount).termId(id).build());
        }
        this.tfRepository.saveAll(tfs);
        this.sparkService.newFileJob(file, tfs);
        return true;
//        List<Tfidf> tfidfs = this.sparkService.newFileJob(file, tfs);
//        wordFrequencies.clear();
//        for (Tfidf tfidf : tfidfs) {
//            wordFrequencies.add(new WordFrequency(tfidf.getWord(), tfidf.getTfidf()));
//        }
//
//        final Dimension dimension = new Dimension(600, 600);
//        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
//        wordCloud.setPadding(2);
//        wordCloud.setBackground(new CircleBackground(300));
//        wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
//        wordCloud.setFontScalar(new SqrtFontScalar(8, 50));
//        wordCloud.build(wordFrequencies);
//        wordCloud.writeToFile(FolderPaths.TAG_CLOUDS + "/" + fileName + ".png");
//        return true;
    }

    public Set<String> listTagClouds() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(FolderPaths.TAG_CLOUDS))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public Set<String> listTxtFiles() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(FolderPaths.TXT_FILES))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public byte[] getTagCloud(String filename) throws IOException {
        Path img = Paths.get(FolderPaths.TAG_CLOUDS + "/" + filename);
        return Files.readAllBytes(img);
    }
}
