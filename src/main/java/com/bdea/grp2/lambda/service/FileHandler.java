package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.FolderPaths;
import com.bdea.grp2.lambda.configuration.WordTokenizer;
import com.bdea.grp2.lambda.model.*;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
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

    public boolean createTagCloud(MultipartFile file) throws Exception {
        try {
            String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.equals("")) {
                throw new IllegalArgumentException("Cannot handle empty filename");
            }
            File found = this.fileRepository.findById(fileName).orElse(null);
            if (found != null) {
                throw new IOException("File already uploaded");
            }
            final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
            frequencyAnalyzer.setWordFrequenciesToReturn(300);
            frequencyAnalyzer.setMinWordLength(4);
            frequencyAnalyzer.setWordTokenizer(new WordTokenizer());

            List<String> texts = new ArrayList<>();
            texts.add(fileContent);
            final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(texts);
            long wordCount = wordFrequencies.stream().map(WordFrequency::getFrequency).reduce(0, Integer::sum);
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
        } catch (Exception e) {
            log.error("Exception: ", e);
            throw new Exception("New file job failed with error", e);
        }
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
