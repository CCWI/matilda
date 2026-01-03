package edu.hm.ccwi.matilda.korpus.rest;

import edu.hm.ccwi.matilda.korpus.service.KorpusService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;

@RestController
@RequestMapping("/analyze")
public class KorpusResource {

    private final KorpusService korpusService;

    public KorpusResource(KorpusService korpusService) {
        this.korpusService = korpusService;
    }

    @RequestMapping(value = "/qs", method = RequestMethod.GET)
    public String qsAnalysesOnKorpus() {
        return korpusService.qsAnalyses();
    }

}