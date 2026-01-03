package edu.hm.ccwi.matilda.korpus.rest;

import edu.hm.ccwi.matilda.korpus.service.DocumentService;
import edu.hm.ccwi.matilda.korpus.service.ExportService;
import edu.hm.ccwi.matilda.korpus.service.MatrixService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.inject.Inject;
import java.io.IOException;

@RestController
@RequestMapping("/docs")
public class DocumentResource {

    @Inject
    DocumentService documentService;

    @RequestMapping(value = "/readme", method = RequestMethod.GET)
    public void exportReadmeKorpus() throws IOException {
        documentService.exportMinimalDocumentKorpus();
    }

}