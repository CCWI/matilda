package edu.hm.ccwi.matilda.korpus.rest;

import edu.hm.ccwi.matilda.korpus.service.ExportService;
import edu.hm.ccwi.matilda.korpus.service.MatrixService;
import org.springframework.web.bind.annotation.*;

import javax.inject.Inject;
import java.io.IOException;

@RestController
@RequestMapping("/export")
public class MatrixResource {

    @Inject
    ExportService exportService;
    @Inject
    MatrixService matrixService;

    @RequestMapping(value = "/bachelor", method = RequestMethod.GET)
    public void exportMinimalBachelorKorpus() throws IOException {
        exportService.exportMinimalKorpus();
    }

    @RequestMapping(value = "/RPGADependencyMatrix", method = RequestMethod.GET)
    public void exportRPGADependencyMatrix() throws IOException {
        matrixService.exportRPGADependencyMatrix(true);
    }

    @RequestMapping(value = "/selfSimilarityMatrix/{similarity}", method = RequestMethod.GET)
    public void exportSelfSimilarityMatrix(@PathVariable("similarity") String similarity) throws IOException {
        matrixService.exportSelfSimilarityMatrix(true, similarity);
    }

    @RequestMapping(value = "/d3csv/{type}", method = RequestMethod.GET)
    public void exportD3csv(@PathVariable("type") String type) throws IOException {
        matrixService.exportD3csv(type);
    }

    @RequestMapping(value = "/d3js", method = RequestMethod.GET)
    public void exportD3csv() throws IOException {
        matrixService.exportD3json();
    }
}