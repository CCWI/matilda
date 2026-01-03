
package edu.hm.ccwi.matilda.korpus.extended;

import edu.hm.ccwi.matilda.base.model.enumeration.LibCategory;
import edu.hm.ccwi.matilda.persistence.mongo.model.GACategoryTag;
import edu.hm.ccwi.matilda.base.util.StringHandler;
import edu.hm.ccwi.matilda.korpus.util.MongoUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class ExtendedMvnPomCrawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedMvnPomCrawler.class);
    private static final String CATEGORY_MAPPING_CSV = "category-mapping.csv";
    private static final String INPUT_DATA_DIRECTORY = "crawled/";
    private static final String ADDITIONAL_INFO_DATABASE_COLLECTION = "gACategoryTagAddManualTags";
    private static final String TARGET_COLLECTION_NAME = "gALibraryExtendedCorpus";

    private static Set<String> overallGALibrarySet;

    private Set<String> uriSet;

    static {
        overallGALibrarySet = new HashSet<>();
    }

    public ExtendedMvnPomCrawler() {
        uriSet = new HashSet<>();
    }

    public static void main(String[] args) {

                //done: "xaa", "xab", "xac", "xaf", "xai", "xal", "xao", "xar", "xau", "xax", "xba", "xbd", "xbg",
                //      "xbj", "xbm", "xbp", "xbs", "xbv", "xby", "xcb", "xce", "xch", "xck", "xcn", "xcq", "xct",
                //      "xcw", "xcz", "xdc", "xdf", "xdi", "xdl", "xdo", "xdr", "xdu", "xdx", "xea", "xed", "xeg",
                //      "xej", "xem", "xep", "xes", "xev", "xey", "xfb", "xfe", "xfh", "xfk", "xfn", "xfq", "xft",
                //      "xfw", "xfz", "xad", "xag", "xaj", "xam", "xap", "xas", "xav", "xay", "xbb", "xbe", "xbh",
                //      "xbk", "xbn", "xbq", "xbt", "xbw", "xbz", "xcc", "xcf", "xci", "xcl", "xco", "xcr", "xcu",
                //      "xcx", "xda", "xdd", "xdg", "xdj", "xdm", "xdp", "xds", "xdv", "xdy", "xeb", "xee", "xeh",
                //      "xek", "xen", "xeq", "xet", "xew", "xez", "xfc", "xff", "xfi", "xfl", "xfo", "xfr", "xfu",
                //      "xfx", "xga", "xae", "xah", "xak", "xan", "xaq", "xat", "xaw", "xaz", "xbc", "xbf", "xbi",
                //      "xbl", "xbo", "xbr", "xbu", "xbx", "xca", "xcd", "xcg", "xcj", "xcm", "xcp", "xcs", "xcv",
                //      "xcy", "xdb", "xde", "xdh", "xdk", "xdn", "xdq", "xdt", "xdw", "xdz",
        String[] files = {"xec", "xef", "xei", "xel", "xeo", "xer", "xeu", "xex", "xfa", "xfd", "xfg", "xfj", "xfm", "xfp", "xfs", "xfv", "xfy"};

        List<GACategoryTag> gACategoryTagAddManualTags = new MongoUtils<>(GACategoryTag.class, ADDITIONAL_INFO_DATABASE_COLLECTION).retrieveCollectionFromDB();

        createExisitingCorpusGASet();

        ExtendedMvnPomCrawler extendedMvnPomCrawler = new ExtendedMvnPomCrawler();
        int counter = 0;
        for (String file : files) {
            counter++;
            System.err.println(" #################################################################################### ");
            System.err.println(" #################################################################################### ");
            System.err.println( counter + " / 92"); //157
            System.err.println(" FILE TO CRAWL: " + file);
            System.err.println(" #################################################################################### ");
            System.err.println(" #################################################################################### ");

            List<CrawlLibraryPom> libraryExtendedCorpus = extendedMvnPomCrawler.extendedCrawlingOnCsv(INPUT_DATA_DIRECTORY + file);
            List<CrawlLibraryPom> completeLibraryExtendedCorpus = new ArrayList<>();
            Set<String> gaIdSet = new HashSet<>();
            for (CrawlLibraryPom extendedCorpus : libraryExtendedCorpus) {
                for (GACategoryTag gACategoryTagAddManualTag : gACategoryTagAddManualTags) {
                    if (gACategoryTagAddManualTag.getGroup().equalsIgnoreCase(extendedCorpus.getGroupId()) &&
                            gACategoryTagAddManualTag.getArtifact().equalsIgnoreCase(extendedCorpus.getArtifactId())) {
                        String id = extendedCorpus.getGroupId() + ":" + extendedCorpus.getArtifactId();
                        if(gaIdSet.contains(id)) { continue; } else { gaIdSet.add(id); }
                        CrawlLibraryPom clp = extendedMvnPomCrawler.extendCorpusEntryByExisitingCorpusInfo(extendedCorpus, gACategoryTagAddManualTag);
                        if(!overallGALibrarySet.contains(clp.getGroupId() + ":" + clp.getArtifactId())) {
                            overallGALibrarySet.add(clp.getGroupId() + ":" + clp.getArtifactId());
                            completeLibraryExtendedCorpus.add(clp);
                        }
                    }
                }
            }

            // save List of completeLibraryExtendedCorpus
            System.out.println("   =========> Save " + completeLibraryExtendedCorpus.size() + " new unique libraries");
            if (CollectionUtils.isNotEmpty(completeLibraryExtendedCorpus)) {
                // for (CrawlLibraryPom extendedCorpus : completeLibraryExtendedCorpus) { System.out.println(extendedCorpus.toString()); }
                new MongoUtils<CrawlLibraryPom>(CrawlLibraryPom.class, TARGET_COLLECTION_NAME).saveDatasetToMongoDB(completeLibraryExtendedCorpus);
            }
        }
    }

    private static void createExisitingCorpusGASet() {
        List<CrawlLibraryPom> savedExtendedCorpus = new MongoUtils<CrawlLibraryPom>(CrawlLibraryPom.class, TARGET_COLLECTION_NAME).retrieveCollectionFromDB();
        if(CollectionUtils.isNotEmpty(savedExtendedCorpus)) {
            for (CrawlLibraryPom extendedCorpus : savedExtendedCorpus) {
                overallGALibrarySet.add(extendedCorpus.getGroupId() + ":" + extendedCorpus.getArtifactId());
            }
        }
    }

    private CrawlLibraryPom extendCorpusEntryByExisitingCorpusInfo(CrawlLibraryPom extendedCorpus, GACategoryTag gACategoryTagAddManualTag) {
        extendedCorpus.setId(gACategoryTagAddManualTag.getGroup() + ":" + gACategoryTagAddManualTag.getArtifact());
        extendedCorpus.setMvnRepoCategory(gACategoryTagAddManualTag.getCategory());
        extendedCorpus.setTagList(gACategoryTagAddManualTag.getTags());
        extendedCorpus.setMatildaCategory(categoryMapping(gACategoryTagAddManualTag.getCategory()));

        return extendedCorpus;
    }

    private String categoryMapping(String category) {
        if(StringUtils.isNotEmpty(category)) {
            category = StringHandler.stripForCategoryEnum(category);
            return LibCategory.valueOf(category).getMatildaCategory();
        } else {
            return null;
        }
    }

    private String stripStringForCsv(String content) {
        return content.replace(",", "").replace(";", ".").trim();
    }

    public List<CrawlLibraryPom> extendedCrawlingOnCsv(String inputCsv) {
        List<CrawlLibraryPom> crawlLibraryPomList = new ArrayList<>();

        int counter = 0;
        String[] csvLines = readCsv(inputCsv);
        for(String htmlsite : csvLines) {
            long start = System.nanoTime();
            counter++;
            System.out.print(counter + "/" + csvLines.length + " :: ");
            if(uriSet.contains(htmlsite)) { continue; } else { uriSet.add(htmlsite); } // skip duplicates
            try {
                String htmlpage = ExtendedMvnPomCrawler.getHtmlPage(htmlsite);
                String[] uris = Pattern.compile("(?<=<a href=)(.+?)(?=\")").matcher(htmlpage).results().map(MatchResult::group).toArray(String[]::new);
                htmlsite = htmlsite.replace("#VERSION#", "").trim();

                List<String> version_uris = new ArrayList<>();
                for (String uri : uris) {
                    if (uri != null && !uri.contains("../") && !uri.contains(".xml")) {
                        uri = uri.replace("\"", "").trim();
                        if (uri.toCharArray().length > 0 && !uri.contains("archetype")) {
                            if (uri.contains(".jar") || uri.contains(".jar.md5") || uri.contains(".sha1")
                                 || uri.contains(".md5") || uri.contains("-rev") || uri.contains(".rev")) {
                                continue;
                            }
                            // System.out.println(uri);
                            if(uri.contains(".0") || uri.contains(".1") || uri.contains(".2") || uri.contains(".3") || uri.contains(".4")
                                    || uri.contains(".5") || uri.contains(".6") || uri.contains(".7") || uri.contains(".8")
                                    || uri.contains(".9") || uri.contains("v0.") || uri.contains("v1.") || uri.contains("v2.")
                                    || uri.contains("v3.") || uri.contains("v4.") || uri.contains("v5.") || uri.contains("v6.")
                                    || uri.contains("v7.") || uri.contains("v8.") || uri.contains("v9.") || uri.contains("0")
                                    || uri.substring(0, 1).contains("1") || uri.substring(0, 1).contains("2") || uri.substring(0, 1).contains("3")
                                    || uri.substring(0, 1).contains("4") || uri.substring(0, 1).contains("5") || uri.substring(0, 1).contains("6")
                                    || uri.substring(0, 1).contains("7") || uri.substring(0, 1).contains("8") || uri.substring(0, 1).contains("9")
                                    || uri.contains("release-")) {
                                version_uris.add(htmlsite + uri);
                            }
                        }
                    }
                }

                if(CollectionUtils.isNotEmpty(version_uris)) {
                    String lastVersionSite = version_uris.get(version_uris.size() - 1);
                    String lastVersionSiteHtmlpage = ExtendedMvnPomCrawler.getHtmlPage(lastVersionSite);
                    String[] lastVersionSiteUris = Pattern.compile("(?<=<a href=)(.+?)(?=\")").matcher(lastVersionSiteHtmlpage).results()
                            .map(MatchResult::group).toArray(String[]::new);
                    for (String versionSiteUris : lastVersionSiteUris) {
                        if(versionSiteUris.contains(".pom") && !versionSiteUris.contains(".md5") && !versionSiteUris.contains(".sha1")
                                && !versionSiteUris.contains(".asc") && !versionSiteUris.contains(".asc.sha256")
                                && !versionSiteUris.contains(".asc.sha512") && !versionSiteUris.contains(".sha256")
                                && !versionSiteUris.contains(".sha512")) {
                            versionSiteUris = versionSiteUris.replace("\"", "").trim();
                            System.out.print("   " + lastVersionSite + versionSiteUris);

                            Thread.sleep(1500 + new Random().nextInt(1000 - 1) + 1);
                            String pomPage = ExtendedMvnPomCrawler.getHtmlPage(lastVersionSite + versionSiteUris);
                            // System.out.println("----> " + pomPage);

                            crawlLibraryPomList.add(parsePomXML(pomPage));
                        }
                    }
                }
                Thread.sleep(2500 + new Random().nextInt(1000 - 5) + 1);
            } catch (Exception e) {
                // ignore
            }
            long end = System.nanoTime();
            System.out.println("  =======> " + (end - start)/ 1000000000 + " seconds");
        }

        return crawlLibraryPomList;
    }

    public CrawlLibraryPom parsePomXML(String pomXml) {
        Document doc = Jsoup.parse(pomXml, "", Parser.xmlParser());
        CrawlLibraryPom crawlLibraryPom = new CrawlLibraryPom();
        crawlLibraryPom.setStrippedPom(stripStringForCsv(pomXml));

        if(doc.getElementsByTag("project") != null && !doc.getElementsByTag("project").isEmpty()) {
            Element project = doc.getElementsByTag("project").first();
            crawlLibraryPom.setGroupId(retrieveStringFromFirstXmlElement(project, "groupId"));
            crawlLibraryPom.setArtifactId(retrieveStringFromFirstXmlElement(project, "artifactId"));
            crawlLibraryPom.setName(retrieveStringFromFirstXmlElement(project, "name"));
            crawlLibraryPom.setDescription(retrieveStringFromFirstXmlElement(project, "description"));
            crawlLibraryPom.setUrl(retrieveStringFromFirstXmlElement(project, "url"));

            // dependencies
            Elements projectDependencies = project.getElementsByTag("dependencies");
            if(projectDependencies != null && !projectDependencies.isEmpty()) {
                List<CrawlLibraryPomDependency> crawlLibraryPomDependencies = new ArrayList<>();
                for (Element projectDependency : projectDependencies) {
                    CrawlLibraryPomDependency crawlLibraryPomDependency = new CrawlLibraryPomDependency();
                    crawlLibraryPomDependency.setGroupId(retrieveStringFromFirstXmlElement(projectDependency, "groupId"));
                    crawlLibraryPomDependency.setArtifactId(retrieveStringFromFirstXmlElement(projectDependency,"artifactId"));
                    crawlLibraryPomDependency.setVersion(retrieveStringFromFirstXmlElement(projectDependency, "version"));
                    crawlLibraryPomDependencies.add(crawlLibraryPomDependency);
                }
                crawlLibraryPom.setDependencies(crawlLibraryPomDependencies);
            }

            // developer
            Elements projectDevelopers = project.getElementsByTag("developers");
            if(projectDevelopers != null && !projectDevelopers.isEmpty()) {
                List<CrawlLibraryPomDeveloper> crawlLibraryPomDevelopers = new ArrayList<>();
                for (Element projectDeveloper : projectDevelopers) {
                    CrawlLibraryPomDeveloper crawlLibraryPomDeveloper = new CrawlLibraryPomDeveloper();
                    crawlLibraryPomDeveloper.setName(retrieveStringFromFirstXmlElement(projectDeveloper, "name"));
                    crawlLibraryPomDeveloper.setEmail(retrieveStringFromFirstXmlElement(projectDeveloper, "email"));
                    crawlLibraryPomDeveloper.setOrganization(retrieveStringFromFirstXmlElement(projectDeveloper, "organization"));
                    crawlLibraryPomDevelopers.add(crawlLibraryPomDeveloper);
                }
                crawlLibraryPom.setDevelopers(crawlLibraryPomDevelopers);
            }
        }

        return crawlLibraryPom;
    }

    public String retrieveStringFromFirstXmlElement(Element element, String searchString) {
        Elements foundElement = element.getElementsByTag(searchString);
        if(foundElement != null && !foundElement.isEmpty()) {
            return foundElement.first().text();
        }
        return null;
    }

    public synchronized static void appendToCsv(String fileName, String crawledUri) {
        BufferedOutputStream bos = null;
        try {
            URL resourceUrl = ExtendedMvnPomCrawler.class.getResource("/" + fileName);
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
            if(bos != null) { bos.close(); }
        } catch (IOException e) {
            System.err.println("Not able to close file: " + fileName);
        }
    }

    public String[] readCsv(String fileName) {
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
