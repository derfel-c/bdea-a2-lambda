package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.model.*;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileHandler {
    private final String root = "files";
    private final WordRepository wordRepository;
    private final FileRepository fileRepository;


    @Autowired
    public FileHandler(WordRepository wordRepository, FileRepository fileRepository) {
        this.wordRepository = wordRepository;
        this.fileRepository = fileRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(root));
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize folder for upload!");
        }
    }

    public boolean createTagCloud(MultipartFile file) throws IOException {
        String fileContent = new String(file.getBytes());
        String fileName = file.getOriginalFilename();
        final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
        frequencyAnalyzer.setWordFrequenciesToReturn(300);
        frequencyAnalyzer.setMinWordLength(4);

        List<String> texts = new ArrayList<>();
        texts.add(fileContent);
        final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(texts);

        File fileEntity = File.builder().filename(fileName).wordCount(wordFrequencies.size()).build();
        this.fileRepository.save(fileEntity);

        final List<Word> words = new ArrayList<>();
        for (WordFrequency wf : wordFrequencies) {
            WordId id = WordId.builder().fileName(fileName).word(wf.getWord()).build();
            words.add(Word.builder().tf(wf.getFrequency()).df(1).wordId(id).build());
        }
        this.wordRepository.saveAll(words);

        final Dimension dimension = new Dimension(600, 600);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(300));
        wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
        wordCloud.setFontScalar(new SqrtFontScalar(8, 50));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile(root + "/" + fileName + ".png");
        return true;
    }

    public Set<String> listFiles() throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(root))) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    public byte[] getTagCloud(String filename) throws IOException {
        Path img = Paths.get(root + "/" + filename);
        return Files.readAllBytes(img);
    }
}
