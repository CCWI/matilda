package edu.hm.ccwi.matilda.korpus.service;

import edu.hm.ccwi.matilda.korpus.service.model.MatrixKorpusRow;

import java.io.IOException;
import java.util.List;

public interface ExportService {

    void exportMinimalKorpus() throws IOException;

    List<MatrixKorpusRow> getMinimalKorpus(int sizeOfDependencies);

}
