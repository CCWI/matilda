package edu.hm.ccwi.matilda.korpus.service;

import java.io.FileNotFoundException;
import java.io.IOException;

public interface MatrixService {

    String[][] exportRPGADependencyMatrix(boolean filterByCategoryTags) throws IOException;

    void exportSelfSimilarityMatrix(boolean filterByCategoryTags, String similarity) throws IOException;

    void exportD3csv(String type) throws IOException;

    void exportD3json() throws IOException;
}
