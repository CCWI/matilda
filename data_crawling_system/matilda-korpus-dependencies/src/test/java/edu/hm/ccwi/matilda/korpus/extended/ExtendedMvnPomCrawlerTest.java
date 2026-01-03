package edu.hm.ccwi.matilda.korpus.extended;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExtendedMvnPomCrawlerTest {

    String xml;

    @BeforeEach
    void setUp() {
    }

    @Test
    void parseFullPomXML() {
        // Arrange
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project><modelVersion>4.0.0</modelVersion><groupId>abbotGroup</groupId><artifactId>abbotArtifact</artifactId><version>1.4.0</version><name>Abbot Java GUI Test Library</name><description>Abbot provides a wrapper around java.awt.Robot to make testing AWT and Swing Applications easier</description><url>http://abbot.sf.net/</url><licenses><license><name>EPL</name><url>https://www.eclipse.org/legal/epl-v10.html</url></license></licenses><developers><developer><name>Gerard Davisonr</name><email>gerard.davison@oracle.com</email><organization>Oralce</organization><organizationUrl>http://www.oracle.com</organizationUrl></developer></developers><scm><connection>scm:svn://svn.code.sf.net/p/abbot/svn/trunkabbot/trunk/</connection><developerConnection>scm:svn://svn.code.sf.net/p/abbot/svn/trunkabbot/trunk/</developerConnection><url>http://sourceforge.net/p/abbot/svn/HEAD/tree/abbot/trunk/</url></scm><dependencies><dependency><groupId>junit</groupId><artifactId>junit</artifactId><version>4.8.2</version></dependency></dependencies></project>";

        // Act
        CrawlLibraryPom crawlLibraryPom = new ExtendedMvnPomCrawler().parsePomXML(xml);
        System.out.println(crawlLibraryPom.toString());

        // Assert
        Assertions.assertNotNull(crawlLibraryPom);
        Assertions.assertEquals(crawlLibraryPom.getGroupId(), "abbotGroup");
        Assertions.assertEquals(crawlLibraryPom.getDependencies().size(), 1);
    }

    @Test
    void parseEmptyPomXML() {
        // Arrange
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><project><modelVersion>4.0.0</modelVersion></project>";

        // Act
        CrawlLibraryPom crawlLibraryPom = new ExtendedMvnPomCrawler().parsePomXML(xml);
        System.out.println(crawlLibraryPom.toString());

        // Assert
        Assertions.assertNotNull(crawlLibraryPom);
        Assertions.assertNull(crawlLibraryPom.getGroupId());
        Assertions.assertNull(crawlLibraryPom.getDependencies());
    }
}