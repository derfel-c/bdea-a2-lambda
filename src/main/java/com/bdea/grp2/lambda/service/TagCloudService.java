package com.bdea.grp2.lambda.service;

import com.bdea.grp2.lambda.configuration.FolderPaths;
import com.kennycason.kumo.CollisionMode;
import com.kennycason.kumo.WordCloud;
import com.kennycason.kumo.WordFrequency;
import com.kennycason.kumo.bg.CircleBackground;
import com.kennycason.kumo.font.scale.SqrtFontScalar;
import com.kennycason.kumo.palette.ColorPalette;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.util.List;

@Service
public class TagCloudService {

    public void createTagCloud(List<WordFrequency> wordFrequencies, String fileName) {
        final Dimension dimension = new Dimension(600, 600);
        final WordCloud wordCloud = new WordCloud(dimension, CollisionMode.PIXEL_PERFECT);
        wordCloud.setPadding(2);
        wordCloud.setBackground(new CircleBackground(300));
        wordCloud.setColorPalette(new ColorPalette(new Color(0x4055F1), new Color(0x408DF1), new Color(0x40AAF1), new Color(0x40C5F1), new Color(0x40D3F1), new Color(0xFFFFFF)));
        wordCloud.setFontScalar(new SqrtFontScalar(8, 50));
        wordCloud.build(wordFrequencies);
        wordCloud.writeToFile(FolderPaths.TAG_CLOUDS + "/" + fileName + ".png");
    }
}
