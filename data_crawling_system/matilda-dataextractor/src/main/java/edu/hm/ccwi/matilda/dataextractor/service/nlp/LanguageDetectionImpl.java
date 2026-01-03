package edu.hm.ccwi.matilda.dataextractor.service.nlp;

import com.google.common.base.Optional;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class LanguageDetectionImpl implements LanguageDetection {

    private static final Logger LOG = LoggerFactory.getLogger(LanguageDetectionImpl.class);

    public Optional<LdLocale> analyzeForLanguageCode(String text) throws IOException {
        LanguageDetector languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(new LanguageProfileReader().readAllBuiltIn()).minimalConfidence(0.50).build();

        //create a text object factory
        TextObjectFactory textObjectFactory = text.length() > 300
                ? CommonTextObjectFactories.forDetectingOnLargeText() : CommonTextObjectFactories.forDetectingShortCleanText();

        //query
        return languageDetector.detect(textObjectFactory.forText(text));
    }

    public String getLanguageCode(String text) {
        int counter = 30;
        try {
            Optional<LdLocale> ldLocaleOptional = analyzeForLanguageCode(text);
            while ((!ldLocaleOptional.isPresent()) && counter > 0) {
                Thread.sleep(300);
                counter--;
            }
            LdLocale ldLocale = ldLocaleOptional.orNull();
            if(ldLocale != null) {
                return ldLocale.getLanguage();
            }
        } catch (IOException e) {
            LOG.error("Error while detecting language of text", e);
        } catch (InterruptedException e) {
            LOG.error("Error while making Thread sleep while detecting language of text", e);
        }

        return null;
    }
}