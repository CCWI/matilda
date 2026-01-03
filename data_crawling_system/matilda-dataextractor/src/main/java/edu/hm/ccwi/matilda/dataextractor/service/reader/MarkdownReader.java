package edu.hm.ccwi.matilda.dataextractor.service.reader;

import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import edu.hm.ccwi.matilda.base.util.IOHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class to handle documentation in markdown.
 * @author Max.Auch
 */
@Service
public class MarkdownReader {

    private static final Logger LOG = LoggerFactory.getLogger(MarkdownReader.class);

    private static final String DOC_FILE_NAME = ".md";

    public List<File> findAllMDInProject(String projectUri) {
        return new IOHandler().findSimilarFiles(DOC_FILE_NAME, new File(projectUri));
    }

    public boolean isMDInProject(String projectUri) {
        return new IOHandler().similarFilesAvailable(DOC_FILE_NAME, new File(projectUri));
    }

    public List<String> convertMarkdownToHtml(List<File> docList) throws IOException {
        List<String> htmlDocumentationReadme = new ArrayList<>();
        MutableDataSet options = new MutableDataSet();
        for (File mdFile : docList) {
            try {
                List<String> mdLineList = Files.readAllLines(mdFile.toPath(), StandardCharsets.UTF_8);

                // uncomment to set optional extensions
                options.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create(), StrikethroughExtension.create()));
                // uncomment to convert soft-breaks to hard breaks
                //options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");

                Parser parser = Parser.builder(options).build();
                HtmlRenderer renderer = HtmlRenderer.builder(options).build();

                String htmlDoc = "";
                for (String mdLine : mdLineList) {
                    Node document = parser.parse(mdLine);
                    String line = renderer.render(document);
                    if (!line.equals("")) {
                        htmlDoc = htmlDoc + " " + line;
                    }
                }
                htmlDoc = htmlDoc.trim();
                htmlDocumentationReadme.add(htmlDoc);
            } catch (MalformedInputException e) {
                LOG.error("Documentation seems to use a different character encoding than UTF-8 -> Documentation will be ignored.");
            }
        }
        return htmlDocumentationReadme;
    }
}
