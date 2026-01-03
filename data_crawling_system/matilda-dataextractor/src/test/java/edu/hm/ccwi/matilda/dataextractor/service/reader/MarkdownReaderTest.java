package edu.hm.ccwi.matilda.dataextractor.service.reader;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkdownReaderTest {

    String path = "src/test/resources/";

    @Test
    public void convertMarkdownToHtmlTest() throws IOException {
        MarkdownReader mdReader = new MarkdownReader();
        List<String> htmlMap = mdReader.convertMarkdownToHtml(mdReader.findAllMDInProject(path));
        String overallHtml = htmlMap.get(0) + htmlMap.get(1);

        assertTrue(overallHtml.contains("<p>An h1 header</p>"));
    }
}
