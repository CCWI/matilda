package edu.hm.ccwi.matilda.korpus.service.impl;

import edu.hm.ccwi.matilda.korpus.model.CrawledDocumentation;
import edu.hm.ccwi.matilda.korpus.service.DocumentService;
import edu.hm.ccwi.matilda.korpus.service.ExportService;
import edu.hm.ccwi.matilda.korpus.service.model.MatrixKorpusRow;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledDocumentationRepository;
import edu.hm.ccwi.matilda.korpus.sink.mongo.CrawledSoftwareRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentServiceImpl implements DocumentService {

    private static final String CSV_SEPARATOR = ";";

    String[] filterTagsInclContent = {"pre", "code", "toolchain", "dependency", "dependencies", "repository",
            "repositories", "exclusions", "kbd", "groupId", "artifactId", "version", "resources", "dependencyManagement"};
    String[] filterStartEndTagsExclContent = {"a", "b", "strong", "em", "li", "ul", "ol", "thead", "tbody", "table",
            "tr", "th", "td", "img", "div", "font", "del"};
    String[] filterSelfclosingTags = {"hr", "br"};

    @Inject
    CrawledSoftwareRepository mongoRepos;
    @Inject
    CrawledDocumentationRepository mongoDocs;
    @Inject
    ExportService exportService;

    BufferedWriter writer;

    public void exportMinimalDocumentKorpus() throws IOException {
        List<MatrixKorpusRow> dependencyMatrixKorpusRows = exportService.getMinimalKorpus(3);
        List<String> distinctCsvList = new ArrayList<>();
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("expDocumentKorpus.csv"), "UTF-8"));
        int pageNumber = 0;
        int pageLimit = 100;
        Page<CrawledDocumentation> page;
        writer.write("commitId; projectName; documentFile");
        writer.newLine();
        do {
            page = mongoDocs.findAll(PageRequest.of(pageNumber, pageLimit));
            for (CrawledDocumentation crawledDocumentation : page) {
                dependencyMatrixKorpusRows.forEach(x -> {
                    try {
                        if (x.getId().equals(crawledDocumentation.getCommitId())) {
                            if (crawledDocumentation.getDocumentationFileList() != null && !crawledDocumentation.getDocumentationFileList().isEmpty()) {
                                StringBuilder concatReadmefile = new StringBuilder();
                                int counter = 0;
                                for (String readmefile : crawledDocumentation.getDocumentationFileList()) {
                                    counter++;
                                    readmefile = filterReadmefile(readmefile);
                                    if (counter <= 1 || concatReadmefile.toString().toLowerCase().trim().contains(readmefile.toLowerCase().trim())) {
                                        concatReadmefile.append(" ");
                                        concatReadmefile.append(readmefile);
                                    }
                                    concatReadmefile.append(CSV_SEPARATOR);
                                }
                                boolean duplicate = false;
                                for(String csvEntry : distinctCsvList) {
                                    if(csvEntry.equalsIgnoreCase(concatReadmefile.toString())) {
                                        duplicate = true;
                                    }
                                }
                                if(!duplicate) {
                                    distinctCsvList.add(concatReadmefile.toString());
                                    writer.write(crawledDocumentation.getCommitId()); writer.write(CSV_SEPARATOR);
                                    writer.write(x.rp); writer.write(CSV_SEPARATOR);
                                    writer.write(concatReadmefile.toString());
                                    writer.newLine();
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOG.error("Error processing document", e);
                    }
                });
            }
            pageNumber++;
        } while (!page.isLast());

        writer.flush();
        writer.close();
    }

    private String filterReadmefile(String readmefile) {
        readmefile = readmefile.replaceAll("[\\.$|;]", ".")
                               .replaceAll("\t", " ")
                               .replaceAll("\n", " ");

        readmefile = readmefile.replaceAll("<ahref", "<a href");
        readmefile = readmefile.replaceAll("<imgsrc", "<img src");

        for (String filterTag : filterSelfclosingTags) { readmefile = removeSimpleSelfclosingTags(readmefile, filterTag); }
        for (String filterTag : filterTagsInclContent) { readmefile = removeAllTagsInclContent(readmefile, filterTag); }
        for (String filterTag : filterStartEndTagsExclContent) { readmefile = removeAllTagsExclContent(readmefile, filterTag); }

        readmefile = removeComments(readmefile);
        readmefile = removeEmptyParagraphs(readmefile);
        readmefile = removeCharRows(readmefile);
        readmefile = removeEmptyParagraphs(readmefile);
        readmefile = removeCharRows(readmefile);
        readmefile = removeTextTagDetails(readmefile);

        return readmefile;
    }

    private String removeTextTagDetails(String readmefile) {
        String[] tags = {"p", "h1", "h2", "h3", "h4", "h5"};

        for(String tag : tags) {
            String searchstring = "<"+tag+"([^>].+?)>";
            Pattern p = Pattern.compile(searchstring);

            do {
                Matcher m = p.matcher(readmefile);
                if (m.find()) {
                    String removableString = "<" + tag + m.group(1) + ">";
                    LOG.debug("Removable string found: {}", removableString);
                    readmefile = readmefile.replace(removableString, "<"+tag+">");
                } else {
                    break;
                }
            } while (true);
        }
        return readmefile;
    }

    private String removeComments(String readmefile) {
        String searchstring = "<!--([^>].+?)-->";
        Pattern p = Pattern.compile(searchstring);

        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<!--" + m.group(1) + "-->";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, " ");
            } else {
                break;
            }
        } while (true);

        searchstring = "<!-([^>].+?)->";
        p = Pattern.compile(searchstring);

        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<!-" + m.group(1) + "->";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, " ");
            } else {
                break;
            }
        } while (true);

        return readmefile;
    }

    private String removeParagraphTagDetails(String readmefile) {
        String searchstring = "<p([^>].+?)>";
        Pattern p = Pattern.compile(searchstring);

        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<p" + m.group(1) + ">";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, "<p>");
            } else {
                break;
            }
        } while (true);
        return readmefile;
    }

    private String removeEmptyParagraphs(String readmefile) {
        readmefile = replaceStepByStep(readmefile, "/*", " ");
        readmefile = replaceStepByStep(readmefile, "*/", " ");

        readmefile = replaceStepByStep(readmefile, "<p>    </p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p>   </p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p>  </p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p> </p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p></p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p>.</p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p>=</p>", " ");
        readmefile = replaceStepByStep(readmefile, "<p>. ", "<p>");
        readmefile = replaceStepByStep(readmefile, "<p>.", "<p>");

        readmefile = replaceStepByStep(readmefile, "[  ]", " ");
        readmefile = replaceStepByStep(readmefile, "[ ]", " ");
        readmefile = replaceStepByStep(readmefile, "[]", " ");

        readmefile = replaceStepByStep(readmefile, "   .", ".");
        readmefile = replaceStepByStep(readmefile, "  .", ".");
        readmefile = replaceStepByStep(readmefile, " .", ".");

        readmefile = replaceStepByStep(readmefile, "   !", "!");
        readmefile = replaceStepByStep(readmefile, "  !", "!");
        readmefile = replaceStepByStep(readmefile, " !", "!");

        readmefile = replaceStepByStep(readmefile, "    ;", ";");
        readmefile = replaceStepByStep(readmefile, "   ;", ";");
        readmefile = replaceStepByStep(readmefile, "  ;", ";");
        readmefile = replaceStepByStep(readmefile, " ;", ";");

        readmefile = replaceStepByStep(readmefile, "   <", "<");
        readmefile = replaceStepByStep(readmefile, "  <", "<");
        readmefile = replaceStepByStep(readmefile, " <", "<");

        readmefile = replaceStepByStep(readmefile, "&gt", "");
        readmefile = replaceStepByStep(readmefile, "&lt", "");

        readmefile = replaceStepByStep(readmefile, ".!", ".");
        readmefile = replaceStepByStep(readmefile, ",.", ".");
        readmefile = replaceStepByStep(readmefile, ";.", ";");

        return readmefile;
    }

    private String replaceStepByStep(String readmefile, String replacableString, String replacingString) {
        do {
            readmefile = readmefile.replace(replacableString, replacingString);
        } while (readmefile.contains(replacableString));
        return readmefile;
    }


    private String removeCharRows(String readmefile) {
        do {
            readmefile = readmefile.replace("  ", " ");
        } while (readmefile.contains("  "));
        do {
            readmefile = readmefile.replace("==", "=");
        } while (readmefile.contains("=="));
        do {
            readmefile = readmefile.replace("##", "#");
        } while (readmefile.contains("##"));
        do {
            readmefile = readmefile.replace("..", ".");
        } while (readmefile.contains(".."));
        do {
            readmefile = readmefile.replace(",,", ",");
        } while (readmefile.contains(",,"));
        do {
            readmefile = readmefile.replace(";;", ";;");
        } while (readmefile.contains(";;"));
        do {
            readmefile = readmefile.replace("--", "-");
        } while (readmefile.contains("--"));

        return readmefile;
    }

    private String removeSimpleSelfclosingTags(String readmefile, String filterTag) {
        do {
            readmefile = readmefile.replaceAll("<" + filterTag + "/>", " ");
            readmefile = readmefile.replaceAll("<" + filterTag + " />", " ");
        } while (readmefile.contains("<" + filterTag + "/>") || readmefile.contains("<" + filterTag + " />"));


        String searchstring = "<" + filterTag + "(.+?)" + "/>";
        Pattern p = Pattern.compile(searchstring);
        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<" + filterTag + m.group(1);
                System.out.println("removable String found: " + removableString + "/>");
                readmefile = readmefile.replace(removableString + "/>", " ");
                readmefile = readmefile.replace(removableString + " />", " ");
            } else {
                break;
            }
        } while (readmefile.contains(filterTag));
        return readmefile;
    }

    private String removeAllTagsExclContent(String readmefile, String filterTag) {
        String searchstring = "<" + filterTag + "([^>]*)>";
        Pattern p = Pattern.compile(searchstring);

        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<" + filterTag + m.group(1) + ">";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, " ");
            } else {
                break;
            }
        } while (readmefile.contains(filterTag));
        readmefile = readmefile.replaceAll("</" + filterTag + ">", ".");

        searchstring = "<" + filterTag + "(.+?)>";
        p = Pattern.compile(searchstring);

        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<" + filterTag + m.group(1) + ">";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, " ");

            } else {
                break;
            }
        } while (readmefile.contains(filterTag));

        return readmefile;
    }

    private String removeAllTagsInclContent(String readmefile, String filterTag) {
        String searchstring = "<" + filterTag + "(.+?)</" + filterTag + ">";
        Pattern p = Pattern.compile(searchstring); //(\\S+)
        do {
            Matcher m = p.matcher(readmefile);
            if (m.find()) {
                String removableString = "<" + filterTag + m.group(1) + "</" + filterTag + ">";
                System.out.println("removable String found: " + removableString);
                readmefile = readmefile.replace(removableString, " ");
            } else {
                break;
            }
        } while (readmefile.contains(filterTag));
        return readmefile;
    }

    public static void main(String[] args) {
        String test = "=====> <h1 aefjnia weofij>Header:</h1><p align=\"right\">Use it as a maven dependency:</p> <pre><code class=\"language-xml\"></code></pre> " +
                "<dependency>  <code>asdf</code><img src=\"http:...\" alt=\"craft-atom\">     </dependency> <toolchain>aefe</toolchain> <hr/> <a href=\"...\" alt=\"\"> " +
                "Or you just click here </a> <hr /> <code class=\"language-xml\"></code> <img src=\"http:...\" alt=\"craft-atom\" />" +
                "<strong>for some more text.</strong><img />  <ul><li> UL-LI </li></ul> <ahref=\"...\">...</a> <imgsrc>...</img> \n" +
                "    <ol><li> OL-LI </li></ol> <em> EM </em> <thead> [NOT REMOVABLE] </thead> \n" +
                "    <tbody> [NOT REMOVABLE] </tbody> <table> [NOT REMOVABLE] </table></img> <!-- comment <tag></tag>-->" +
                "<!-- comment <tag></tag>-->ugfzj<!- comment <tag></tag>->";
        String filteredTest = new DocumentServiceImpl().filterReadmefile(test);

        System.err.println("result: " + filteredTest);
    }
}