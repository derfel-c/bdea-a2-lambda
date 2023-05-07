package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.WordTokenizer;
import com.bdea.grp2.lambda.model.TermId;
import com.bdea.grp2.lambda.model.Tf;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.nlp.FrequencyAnalyzer;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class TfService implements Serializable {

    public List<Tf> extractTfsFromFile(byte[] content, String fileName) throws Exception {
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
