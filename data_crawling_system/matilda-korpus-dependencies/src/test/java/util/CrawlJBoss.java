
package util;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Crawler-Plan:
 * 1) Crawl maven-central State_1, _2, _3, _4, _5, _6, _7, _8, _9 ==> jeder Eintrag sollte jetzt auf #Version# enden.
 * 1.5) Replace /r1.../ by #VERSION#
 * 2) Entferne Duplikate
 * 3) Extrahiere GroupId/ArtifactId
 * 4) Ersetze alle "/", bis auf das letzte durch "."
 * 5) Crawl MvnRepo and save to csv/mongodb like matilda-extractor
 */
public class CrawlJBoss {

    /**
     * STEP #2
     *
     * @throws Exception
     */
    @Test
    public void createGroupArtifactList() {
        String inputCsv = "crawlJBossState_11-final.csv";
        String outputCsv = "gaEntryList-JBOSS.csv";
        String[] mcDependencyList = readStateOfLastStep(inputCsv);

        // extrahiere GA
        for(int i = 0; i < mcDependencyList.length; i++) {
            String dep = mcDependencyList[i];
            if(dep.contains("#VERSION#")) {
                String ga = dep.replace("https://repository.jboss.org/nexus/content/repositories/releases/", "")
                        .replace("/#VERSION#", "")
                        .replace("//", "/")
                        .trim();
                //if(ga.chars().filter(num -> num == '/').count() > 1) {
                // Ersetze alle "/", bis auf das letzte durch "."
                ga = ga.replace("/", "§");

                if (ga.contains("§")) {
                    StringBuilder b = new StringBuilder(ga);
                    b.replace(ga.lastIndexOf("§"), ga.lastIndexOf("§") + 1, "/");
                    ga = b.toString();
                    if (ga.contains("§")) {
                        ga = ga.replace("§", ".");
                    }
                }
                //}
                mcDependencyList[i] = ga;
                /**                if(!newMcDependencyList.contains(ga)) {
                 *                     newMcDependencyList.add(ga);
                 *                 }
                 */
            }
        }

        // entferne duplikate
        mcDependencyList = Arrays.stream(mcDependencyList).distinct().toArray(String[]::new);

        // speichere GA in csv
        for(String gaEntry : mcDependencyList) { CrawlJBoss.appendToCsv(outputCsv, gaEntry); }
    }

    /**
     * STEP #1
     *
     * @throws Exception
     */
    @Test
    public void crawlJBoss() throws Exception {
        String inputCsv = "crawlJBossState_10.csv";
        String outputCsv = "crawlJBossState_11.csv";

        for(String htmlsite : readStateOfLastStep(inputCsv)) {
            try {
                boolean versionAlreadyReached = false;
                System.out.println(htmlsite);

                if (htmlsite.contains("#VERSION#")) {
                    CrawlJBoss.appendToCsv(outputCsv, htmlsite);
                } else {
                    String htmlpage = CrawlJBoss.getHtmlPage(htmlsite);
                    String[] uris = Pattern.compile("(?<=<a href=)(.+?)(?=\")").matcher(htmlpage)
                            .results().map(MatchResult::group).toArray(String[]::new); // "(?<=class=\"b tag\">)(.+?)(?=<\\/a>)"

                    for (String uri : uris) {
                        if (!uri.contains("../") && !uri.contains(".xml") && !versionAlreadyReached) {
                            uri = uri.replace("\"", "").trim();
                            if (uri != null && uri.toCharArray().length > 0 && !uri.contains("archetype")) {
                                if (uri.contains(".jar") || uri.contains(".jar.md5") || uri.contains(".sha1") || uri.contains(".md5")
                                        || uri.contains(".0") || uri.contains(".1") || uri.contains(".2") || uri.contains(".3") || uri.contains(".4")
                                        || uri.contains(".5") || uri.contains(".6") || uri.contains(".7") || uri.contains(".8") || uri.contains(".9")
                                        || uri.contains("-rev") || uri.contains(".rev") || uri.contains("v0.") || uri.contains("v1.")
                                        || uri.contains("v2.") || uri.contains("v3.") || uri.contains("v4.") || uri.contains("v5.")
                                        || uri.contains("v6.") || uri.contains("v7.") || uri.contains("v8.") || uri.contains("v9.")) {
                                    String reducedURI = htmlsite.substring(0,htmlsite.lastIndexOf("/"));
                                    CrawlJBoss.appendToCsv(outputCsv, reducedURI + "/#VERSION#");
                                    versionAlreadyReached = true;
                                } else if (Character.isDigit(uri.toCharArray()[0])) {
                                    CrawlJBoss.appendToCsv(outputCsv, htmlsite + "#VERSION#");
                                    versionAlreadyReached = true;
                                } else {
                                    CrawlJBoss.appendToCsv(outputCsv, uri);
                                }
                            }
                        }
                    }
                    Thread.sleep(3000 + new Random().nextInt(1000 - 1) + 1);
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    public synchronized static void appendToCsv(String fileName, String crawledUri) {
        BufferedOutputStream bos = null;
        try {
            URL resourceUrl = CrawlJBoss.class.getResource("/" + fileName);
            OutputStream output = new FileOutputStream(new File(resourceUrl.toURI()), true);
            bos = new BufferedOutputStream(output);

            //write an entry for the category
            bos.write((crawledUri +"\n").getBytes("UTF-8"));
            bos.flush();
        } catch (IOException e) {
            System.err.println("Exception on writing dependency to file: " + fileName);
        } catch (URISyntaxException e) {
            System.err.println("Couldn't cast URL to URI: " + fileName);
        }

        try {
            if(bos != null) {
                bos.close();
            }
        } catch (IOException e) {
            System.err.println("Not able to close file: " + fileName);
        }
    }

    public String[] readStateOfLastStep(String fileName) {
        List<String> crawlableUris = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("/" + fileName);
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);

        try (BufferedReader br = new BufferedReader(streamReader)) {
            String line = br.readLine();
            while (line != null) {
                crawlableUris.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error on initializing category and tag lists for dependencies! Not able to load init-csv.");
        }

        return crawlableUris.toArray(String[]::new);
    }

    private static String getHtmlPage(String url) throws Exception {
        HttpURLConnection hc = (HttpURLConnection) new URL(url).openConnection();

        hc.setRequestMethod("GET");
        hc.setRequestProperty("Content-Type", "application/json");
        hc.addRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        hc.addRequestProperty("Accept-Language", "en-US,en;q=0.5");
        hc.addRequestProperty("Connection", "keep-alive");
        hc.addRequestProperty("Host", "repo1.maven.org");
        hc.addRequestProperty("TE", "Trailers");
        hc.addRequestProperty("Upgrade-Insecure-Requests", "1");
        hc.addRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:75.0) Gecko/20100101 Firefox/75.0");

        if(hc.getResponseCode() < 400) {
            BufferedReader in = new BufferedReader(new InputStreamReader(hc.getInputStream()));
            String inputLine;
            StringBuilder builder = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                builder.append(inputLine.trim());
            }
            in.close();

            return builder.toString();
        } else if (hc.getResponseCode() == 403) {
            System.err.println("FORBIDDEN - Requests might be blocked by now for: " + url);
            throw new Exception("FORBIDDEN - Requests might be blocked by now for: " + url);
        } else {
            throw new Exception("URL not available: " + url);
        }
    }
}
