package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.FolderPaths;
import com.bdea.grp2.lambda.configuration.WordTokenizer;
import com.bdea.grp2.lambda.model.TermId;
import com.bdea.grp2.lambda.model.Tf;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import com.kennycason.kumo.palette.ColorPalette;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TagCloudService implements Serializable {

    public void createTagCloud(List<WordFrequency> wordFrequencies, String fileName) {
        final Dimension dimension = new Dimension(600, 600);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        // Safeguard for values <= 0
        wordFrequencies = wordFrequencies.stream().filter(f -> f.getFrequency() >= 0).collect(Collectors.toList());
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(300));
        Random random = new Random();
        int c1 = random.nextInt(0xffffff + 1);
        int c2 = random.nextInt(0xffffff + 1);
        int c3 = random.nextInt(0xffffff + 1);
        int c4 = random.nextInt(0xffffff + 1);
        int c5 = random.nextInt(0xffffff + 1);
        int c6 = random.nextInt(0xffffff + 1);
        wordCloud.setColorPalette(new ColorPalette(new Color(c1), new Color(c2), new Color(c3), new Color(c4), new Color(c5), new Color(c6)));
        wordCloud.setFontScalar(new SqrtFontScalar(8, 50));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile(FolderPaths.TAG_CLOUDS + "/" + fileName + ".png");
    }

    public List<Tf> extractTfsFromFile(byte[] content, String fileName) {
        String fileContent = new String(content, StandardCharsets.UTF_8);

        final FrequencyAnalyzer frequencyAnalyzer = new FrequencyAnalyzer();
        frequencyAnalyzer.setWordFrequenciesToReturn(300);
        frequencyAnalyzer.setMinWordLength(4);
        frequencyAnalyzer.setWordTokenizer(new WordTokenizer());

        List<String> texts = new ArrayList<>();
        texts.add(fileContent);
        final List<WordFrequency> wordFrequencies = frequencyAnalyzer.load(texts);
        long wordCount = wordFrequencies.stream().map(WordFrequency::getFrequency).reduce(0, Integer::sum);

        final List<Tf> tfs = new ArrayList<>();
        for (WordFrequency wf : wordFrequencies) {
            TermId id = TermId.builder().fileName(fileName).term(wf.getWord()).build();
            tfs.add(Tf.builder().tf((float) wf.getFrequency() / wordCount).termId(id).termCount(wf.getFrequency()).build());
        }

        return tfs;
    }
}
