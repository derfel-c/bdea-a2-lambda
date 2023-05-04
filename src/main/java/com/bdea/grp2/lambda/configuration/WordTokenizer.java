package com.bdea.grp2.lambda.configuration;

import java.util.Arrays;
import java.util.List;

public class WordTokenizer implements com.kennycason.kumo.nlp.tokenizer.api.WordTokenizer {
    @Override
    public List<String> tokenize(String sentence) {
        return Arrays.asList(sentence.split("(?U)[^\\p{Alpha}0-9']+"));
    }
}
