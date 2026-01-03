package edu.hm.ccwi.matilda.dataextractor.service.nlp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LanguageDetectionTest {

    @Test
    public void testLanguageDetectionGetLanguageCode() {
        String languageCode = new LanguageDetectionImpl().getLanguageCode("software zum crawlen.");
        assertTrue(languageCode.equals("de"));
    }
}
