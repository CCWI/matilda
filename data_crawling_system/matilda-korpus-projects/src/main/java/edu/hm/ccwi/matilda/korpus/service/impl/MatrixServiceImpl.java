package edu.hm.ccwi.matilda.korpus.service.impl;

import edu.hm.ccwi.matilda.korpus.service.AbstractMatrixService;
import edu.hm.ccwi.matilda.korpus.service.MatrixService;
import edu.hm.ccwi.matilda.korpus.service.model.Domain;
import edu.hm.ccwi.matilda.korpus.service.model.MatrixKorpusRow;
import org.apache.commons.lang3.SerializationUtils;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
public class MatrixServiceImpl extends AbstractMatrixService implements MatrixService {

    BufferedWriter writer;
    Map<String, Domain> libDomainMap;
    public List<MatrixKorpusRow> matrixKorpusRowList;
    List<String> distinctDependencyList;

    int sizeOfDependencies = 3;
    String[] blacklistedKeywords = {"logging", "log4j", "slf4j", "testing", "test", "unit", "utilities", "utility",
            "util", "commons", "common", "assertion", "assert", "mock", "mocking"};

    /**
     * @throws IOException
     */
    private void initDatalists() throws IOException {
        libDomainMap = categorizationService.getCategoriesTagsOfMvnRepo();
        matrixKorpusRowList = exportService.getMinimalKorpus(sizeOfDependencies);
        distinctDependencyList = getDistinctDependencyList(matrixKorpusRowList);
    }


    /**
     * @param filterByCategoryTags
     * @throws IOException
     */
    @Override
    public String[][] exportRPGADependencyMatrix(boolean filterByCategoryTags) throws IOException {
        initDatalists();
        String[][] finalFiltereddependencyMatrix = new String[0][0];
        if (distinctDependencyList != null && !distinctDependencyList.isEmpty()) {

            // CREATE MATRIX
            System.out.println("### Create first matrix: " + matrixKorpusRowList.size() + "projects x " + distinctDependencyList.size() + "libraries...");
            String[][] dependencyMatrix = createOriginalMatrix();

            System.out.println("### Find excludable - unused/removable dependencies...");
            List<Integer> removableColumns = findUnusedAndRemovableDependencies(dependencyMatrix);

            System.out.println("### Filter " + removableColumns.size() + " excluded and unused dependencies...");
            String[][] filtereddependencyMatrix = filterExcludabeLibraries(dependencyMatrix, removableColumns);

            System.out.println("### Find projects without dependencies...");
            List<Integer> removableRows = findRemovableProjects(filtereddependencyMatrix);

            System.out.println("### Filter " + removableRows.size() + " projects");
            finalFiltereddependencyMatrix = createRowFilteredMatrix(removableColumns, removableRows, filtereddependencyMatrix);

            // SAVE MATRIX TO CSV
            System.out.println("### Save matrix...");
            saveMatrixToCsv(finalFiltereddependencyMatrix, "exportedRPGADependencyMatrix.csv");
            System.err.println("### finished processing matrix...");
        }
        return finalFiltereddependencyMatrix;
    }

    /**
     * @param filterByCategoryTags
     * @throws IOException
     */
    public String[][] exportRPConsolidatedDependencyMatrix(boolean filterByCategoryTags) throws IOException {
        initDatalists();
        String[][] finalFiltereddependencyMatrix = new String[0][0];
        if (distinctDependencyList != null && !distinctDependencyList.isEmpty()) {

            // CREATE MATRIX
            System.out.println("### Create first matrix: " + matrixKorpusRowList.size() + "projects x " + distinctDependencyList.size() + "libraries...");
            String[][] dependencyMatrix = createOriginalMatrix();

            System.out.println("### Find excludable - unused/removable dependencies...");
            List<Integer> removableColumns = findUnusedAndRemovableDependencies(dependencyMatrix);

            System.out.println("### Filter " + removableColumns.size() + " excluded and unused dependencies...");
            String[][] filtereddependencyMatrix = filterExcludabeLibraries(dependencyMatrix, removableColumns);

            // CONSOLIDATE MATRIX
            System.out.println("### Consolidate project-modules...");
            finalFiltereddependencyMatrix = consolidateDependencyRows(filtereddependencyMatrix);

            System.out.println("### Find projects without dependencies...");
            List<Integer> removableRows = findRemovableProjects(filtereddependencyMatrix);

            removableRowsFromConsolidation.forEach(consolidatedRow -> {
                if(!removableRows.contains(consolidatedRow)) {
                    removableRows.add(consolidatedRow);
                }
            });

            System.out.println("### Filter " + removableRows.size() + " projects");
            finalFiltereddependencyMatrix = createRowFilteredMatrix(removableColumns, removableRows, filtereddependencyMatrix);

            // SAVE MATRIX TO CSV
            System.out.println("### Save matrix...");
            saveMatrixToCsv(finalFiltereddependencyMatrix, "exportedRPGADependencyMatrix.csv");
            System.err.println("### finished processing matrix...");
        }
        return finalFiltereddependencyMatrix;
    }

    @Override
    public void exportSelfSimilarityMatrix(boolean filterByCategoryTags, String similarity) throws IOException {
        String[][] finalFiltereddependencyMatrix = exportRPConsolidatedDependencyMatrix(true);
        String[][] selfSimilarityMatrix = new String[0][0];
        String[][] selfDistanceMatrix = new String[0][0];
        if (finalFiltereddependencyMatrix.length > 1) {
            System.out.println("### Create selfsimilarity matrix...");
            selfSimilarityMatrix = new String[finalFiltereddependencyMatrix.length][finalFiltereddependencyMatrix.length + 3];
            selfDistanceMatrix = new String[finalFiltereddependencyMatrix.length][finalFiltereddependencyMatrix.length + 3];
            selfSimilarityMatrix[0][0] = "id"; selfSimilarityMatrix[0][1] = "rp"; selfSimilarityMatrix[0][2] = "ga";
            selfDistanceMatrix[0][0] = "id"; selfDistanceMatrix[0][1] = "rp"; selfDistanceMatrix[0][2] = "ga";

            // write GA of rows in columns
            for (int row = 0; row < selfSimilarityMatrix.length; row++) {
                String s = finalFiltereddependencyMatrix[row][2];
                selfSimilarityMatrix[0][row + 2] = s;
                selfDistanceMatrix[0][row + 2] = s;
            }

            int count = 0;

            // calculate similarity of each cell by looking up GA in dependencyMatrix
            for (int row = 1; row < selfSimilarityMatrix.length; row++) {
                selfSimilarityMatrix[row][0] = finalFiltereddependencyMatrix[row][0];
                selfSimilarityMatrix[row][1] = finalFiltereddependencyMatrix[row][1];
                selfSimilarityMatrix[row][2] = finalFiltereddependencyMatrix[row][2];
                selfDistanceMatrix[row][0] = finalFiltereddependencyMatrix[row][0];
                selfDistanceMatrix[row][1] = finalFiltereddependencyMatrix[row][1];
                selfDistanceMatrix[row][2] = finalFiltereddependencyMatrix[row][2];
                for (int col = 3; col < selfSimilarityMatrix[row].length; col++) {
                    if(selfSimilarityMatrix[row][2] != null && selfSimilarityMatrix[0][col] != null) {
                        String[] rowVector = getFeatureVectorByGA(finalFiltereddependencyMatrix, selfSimilarityMatrix[row][2]); // = rowGA
                        String[] colVector = getFeatureVectorByGA(finalFiltereddependencyMatrix, selfSimilarityMatrix[0][col]); // = colGA
                        double[] result = calculateVectorSimilarity(similarity, rowVector, colVector);
                        selfSimilarityMatrix[row][col] = String.format("%.2f", result[0]);
                        selfDistanceMatrix[row][col] = String.format("%.2f", result[1]);
                    }
                }
                count++;
                if((count % 100) == 0) {
                    System.out.println("#### calculated row: " + count + "/"+selfSimilarityMatrix.length);
                }
            }

            while (selfSimilarityMatrix.length != selfSimilarityMatrix[0].length - 3) {
                System.out.println("##### Matrix is size: " + selfSimilarityMatrix.length + " x " + (selfSimilarityMatrix[0].length - 3));
                selfDistanceMatrix = adjustMatrixSize(selfSimilarityMatrix);
                selfDistanceMatrix = adjustMatrixSize(selfDistanceMatrix);
            }

            // SAVE MATRIX TO CSV
            System.out.println("### Save selfsimilarity/distance matrix... in size: " + selfSimilarityMatrix.length + " x " + selfSimilarityMatrix[0].length);
            saveMatrixToCsv(selfSimilarityMatrix, "exportedSelfSimilarityMatrix.csv");
            saveMatrixToCsv(selfDistanceMatrix, "exportedDistanceMatrix.csv");
            System.err.println("### finished processing selfsimilarity/distance matrix...");
        }
    }

    private String[][] adjustMatrixSize(String[][] matrix) {
        String[][] clonedMatrix = SerializationUtils.clone(matrix);
        for (int row = 0; row < matrix.length-2; row++) {
            for (int col = 0; col < matrix[row].length-3; col++) {
                clonedMatrix[row][col] = matrix[row][col];
            }
        }
        return clonedMatrix;
    }

    // TODO: NOT THREADSAFE
    List<Integer> removableRowsFromConsolidation = new ArrayList<>();

    private String[][] consolidateDependencyRows(String[][] finalFiltereddependencyMatrix) {
        String[][] consolidatedMatrix = SerializationUtils.clone(finalFiltereddependencyMatrix);

        // consolidate and add removable rows to list
        for (int searchRow = 0; searchRow < consolidatedMatrix.length; searchRow++) {
            for (int row = 0; row < consolidatedMatrix.length; row++) {
                if(consolidatedMatrix[searchRow][1].equalsIgnoreCase(consolidatedMatrix[row][1]) && searchRow != row &&
                        !removableRowsFromConsolidation.contains(row) && !removableRowsFromConsolidation.contains(searchRow)) {
                    for (int col = 0; col < consolidatedMatrix[row].length; col++) {
                        if(consolidatedMatrix[searchRow][col].equals("0") && consolidatedMatrix[row][col].equals("1")) {
                            consolidatedMatrix[searchRow][col] = "1";
                        }
                    }
                    removableRowsFromConsolidation.add(row);
                }
            }
        }


        return consolidatedMatrix;
    }

    private double[] calculateVectorSimilarity(String similarity, String[] rowVector, String[] colVector) {
        double similarityResult = 0.0; double a = 0; double b = 0; double c = 0;

        for (int i = 3; i < rowVector.length && i < colVector.length; i++) {
            if(rowVector[i].equals("0") && colVector[i].equals("0")) { continue; }
            if (rowVector[i].equals("1") && colVector[i].equals("1")) { a++; continue; }
            if (rowVector[i].equals("1") && colVector[i].equals("0")) { b++; continue; }
            if (rowVector[i].equals("0") && colVector[i].equals("1")) { c++; continue; }
        }

        if (similarity.equals("jaccard") && a != 0) {
            similarityResult = (a) / (a + b + c);
        } else if (similarity.equals("auch-3") && a != 0) {
            similarityResult = ((a*a) / ((a*a) + (b*b) + (c*c)));
        }

        return new double[]{similarityResult, 1.0 - similarityResult};
    }

    private String[] getFeatureVectorByGA(String[][] finalFiltereddependencyMatrix, String rowGA) {
        for (int searchRow = 1; searchRow < finalFiltereddependencyMatrix.length; searchRow++) {
            if (rowGA != null && rowGA.equalsIgnoreCase(finalFiltereddependencyMatrix[searchRow][2])) {
                return finalFiltereddependencyMatrix[searchRow];
            }
        }
        System.err.println("getFeatureVectorByGA --> NO ENTRY FOUND FOR: " + rowGA);
        return null;
    }


    /**
     * @param matrix
     * @throws IOException
     */
    private void saveMatrixToCsv(String[][] matrix, String csvName) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvName), "UTF-8"));
        writer.newLine();
        for (int j = 0; j < matrix.length; j++) {
            StringBuffer oneLine = new StringBuffer();
            for (int i = 0; i < matrix[j].length; i++) {
                oneLine.append(matrix[j][i]); oneLine.append(CSV_SEPARATOR);
            }
            writer.write(oneLine.toString()); writer.newLine();
            writer.flush();
        }
        writer.close();
    }

    /**
     * @param matrix
     * @throws IOException
     */
    private void saveMatrixExceptNullToCsv(String[][] matrix, String csvName) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(csvName), "UTF-8"));
        writer.newLine();
        for (int row = 0; row < matrix.length; row++) {
            boolean nullRow = true;
            StringBuffer oneLine = new StringBuffer();
            for (int col = 0; col < matrix[row].length; col++) {
                if(matrix[row][col] != null) {
                    oneLine.append(matrix[row][col]); oneLine.append(CSV_SEPARATOR);
                    nullRow = false;
                }
            }
            if(!nullRow) {
                writer.write(oneLine.toString()); writer.newLine();
            }
            writer.flush();
        }
        writer.close();
    }


    /**
     * @param removableColumns
     * @param removableRows
     * @param filtereddependencyMatrix
     * @return
     */
    private String[][] createRowFilteredMatrix(List<Integer> removableColumns, List<Integer> removableRows, String[][] filtereddependencyMatrix) {
        String[][] filtereddependencyMatrix2 = new String[(matrixKorpusRowList.size() + 2) - removableRows.size()][(6 + distinctDependencyList.size()) - removableColumns.size()];
        for (int row = 0; row <= 0; row++) {
            for (int col = 0; col < filtereddependencyMatrix[row].length; col++) {
                if (filtereddependencyMatrix[0][col] != null && !filtereddependencyMatrix[0][col].isEmpty()) {
                    filtereddependencyMatrix2[row][col] = filtereddependencyMatrix[row][col];
                }
            }
        }
        if (removableRows != null && !removableRows.isEmpty()) {
            int newMatrixRowDiff = 0;
            for (int row = 1; row < filtereddependencyMatrix.length; row++) {
                if (!removableRows.contains(row)) {
                    for (int col = 0; col < filtereddependencyMatrix[row].length; col++) {
                        if (filtereddependencyMatrix[0][col] != null && !filtereddependencyMatrix[0][col].isEmpty()) {
                            filtereddependencyMatrix2[row - newMatrixRowDiff][col] = filtereddependencyMatrix[row][col];
                        }
                    }
                } else {
                    newMatrixRowDiff++;
                }
            }
        }
        return filtereddependencyMatrix2;
    }


    /**
     * @param filtereddependencyMatrix
     * @return
     */
    private List<Integer> findRemovableProjects(String[][] filtereddependencyMatrix) {
        List<Integer> removableRows = new ArrayList();

        // check if project (row) has a dependency
        for (int row = 0; row < filtereddependencyMatrix.length; row++) {
            boolean projectUsesDependencies = false;
            for (int col = 0; col < filtereddependencyMatrix[row].length; col++) {
                if (filtereddependencyMatrix[row][col] != null && filtereddependencyMatrix[row][col].equals("1")) {
                    projectUsesDependencies = true; break;
                }
            }
            if (!projectUsesDependencies) {
                removableRows.add(row);
            }
        }

        // check if row is a duplicate
        List<String[]> distinctRowList = new ArrayList<>();
        distinctRowList.add(filtereddependencyMatrix[1]);
        for (int row = 2; row < filtereddependencyMatrix.length; row++) {
            for(String[] distinctRow : distinctRowList) {
                String[] distinctCompareRow = SerializationUtils.clone(distinctRow);
                distinctCompareRow[0] = null; distinctCompareRow[2] = null;
                String[] filtereddependencyCompareMatrixRow = SerializationUtils.clone(filtereddependencyMatrix[row]);
                filtereddependencyCompareMatrixRow[0] = null;
                filtereddependencyCompareMatrixRow[2] = null;
                if(Arrays.equals(distinctCompareRow, filtereddependencyCompareMatrixRow)) {
                    if(!removableRows.contains(row)) {
                        removableRows.add(row);
                    }
                }
            }
        }

        return removableRows;
    }


    /**
     * @param dependencyMatrix
     * @param removableColumns
     * @return
     */
    private String[][] filterExcludabeLibraries(String[][] dependencyMatrix, List<Integer> removableColumns) {
        String[][] filtereddependencyMatrix = new String[matrixKorpusRowList.size() + 1][((3 + distinctDependencyList.size()) - removableColumns.size()) + 3];
        for (int row = 0; row < dependencyMatrix.length; row++) {
            for (int col = 0; col < 3; col++) {
                filtereddependencyMatrix[row][col] = SerializationUtils.clone(dependencyMatrix[row][col]);
            }
        }
        if (removableColumns != null && !removableColumns.isEmpty()) {
            for (int row = 0; row < dependencyMatrix.length; row++) {
                int newMatrixColDiff = 0;
                for (int col = 3; col < dependencyMatrix[row].length; col++) {
                    if (!removableColumns.contains(col)) {
                        if (dependencyMatrix[0][col] != null && !dependencyMatrix[0][col].isEmpty()) {
                            filtereddependencyMatrix[row][col - newMatrixColDiff] = SerializationUtils.clone(dependencyMatrix[row][col]);
                        }
                    } else {
                        newMatrixColDiff++;
                    }
                }
            }
        }
        return filtereddependencyMatrix;
    }


    /**
     * @param dependencyMatrix
     * @return
     */
    private List<Integer> findUnusedAndRemovableDependencies(String[][] dependencyMatrix) {
        List<Integer> removableColumns = new ArrayList();

        System.out.println("###### Find removable (blacklisted) dependencies...");
        for (int j = 0; j < dependencyMatrix[0].length; j++) {
            String column = dependencyMatrix[0][j].toLowerCase();
            boolean blacklistedDepdenency = false;
            for (String blacklistedKeyword : blacklistedKeywords) {
                Domain domain = null;
                if (libDomainMap.containsKey(column)) {
                    domain = libDomainMap.get(column);
                }
                if (domain != null && domain.getMvnCategory() != null && !domain.getMvnCategory().isEmpty()
                        && domain.getMvnCategory().toLowerCase().contains(blacklistedKeyword)) {
                    blacklistedDepdenency = true;
                    break;
                } //check mvnRepoCategory
                if (domain != null && domain.getMvnTags() != null && !domain.getMvnTags().isEmpty()) {
                    for (String tag : domain.getMvnTags()) {
                        if (tag != null && !tag.isEmpty() && tag.toLowerCase().contains(blacklistedKeyword)) {
                            blacklistedDepdenency = true;
                            break;
                        }
                    }
                } //check mvnRepoTags
                if (column.contains(blacklistedKeyword.toLowerCase())) {
                    blacklistedDepdenency = true;
                    break;
                } //check GA

            }

            if (blacklistedDepdenency) {
                removableColumns.add(j);
            }
        }

        // Find unused dependencies
        System.out.println("###### Find unused dependencies...");
        for (int col = 0; col < dependencyMatrix[0].length; col++) {
            boolean usedDependency = false;
            for (int row = 0; row < dependencyMatrix.length; row++) {
                String cell = dependencyMatrix[row][col];
                if (cell != null && cell.equals("1")) {
                    usedDependency = true;
                }
            }
            if (!usedDependency && !removableColumns.contains(col)) {
                removableColumns.add(col);
            }
        }
        return removableColumns;
    }


    /**
     * Create a first Matrix.
     *
     * @return
     */
    private String[][] createOriginalMatrix() {
        String[][] dependencyMatrix = new String[matrixKorpusRowList.size() + 1][3 + distinctDependencyList.size()];
        // add columns
        dependencyMatrix[0][0] = "id";
        dependencyMatrix[0][1] = "rp";
        dependencyMatrix[0][2] = "ga";
        for (int y = 0; y < distinctDependencyList.size(); y++) {
            dependencyMatrix[0][y + 3] = distinctDependencyList.get(y);
        }

        // add rows
        for (int x = 0; x < matrixKorpusRowList.size(); x++) {
            dependencyMatrix[x + 1][0] = matrixKorpusRowList.get(x).getId(); //UUID.randomUUID().toString();
            dependencyMatrix[x + 1][1] = matrixKorpusRowList.get(x).getRp();
            dependencyMatrix[x + 1][2] = matrixKorpusRowList.get(x).getGa();

            // add vector of each entry
            for (int i = 0; i < matrixKorpusRowList.get(x).getDependencyGA().size(); i++) {
                String entryDependency = matrixKorpusRowList.get(x).getDependencyGA().get(i);
                for (int z = 0; z < distinctDependencyList.size(); z++) {
                    if (entryDependency != null && entryDependency.equalsIgnoreCase(distinctDependencyList.get(z))) {
                        dependencyMatrix[x + 1][z + 3] = "1";
                    } else {
                        if (dependencyMatrix[x + 1][z + 3] == null || dependencyMatrix[x + 1][z + 3].isEmpty() || !dependencyMatrix[x + 1][z + 3].equals("1")) {
                            dependencyMatrix[x + 1][z + 3] = "0";
                        }
                    }
                }

            }
        }
        return dependencyMatrix;
    }

    /**
     * @param matrixKorpusRowList
     * @return
     */
    private List<String> getDistinctDependencyList(List<MatrixKorpusRow> matrixKorpusRowList) {
        List<String> dependencyList = new ArrayList<>();
        matrixKorpusRowList.forEach(row -> row.getDependencyGA().forEach(dependency -> {
            if (!dependencyList.contains(dependency)) {
                dependencyList.add(dependency);
            }
        }));

        return dependencyList;
    }

    public void exportD3json() throws IOException {
        List<List<String>> matrix = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader("exportedSelfSimilarityMatrix.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(CSV_SEPARATOR);
                matrix.add(Arrays.asList(values));
            }
        }
        new JsonHeatmapUtil().createJson(matrix, "exportedSelfSimilarity.json");
    }

    @Override
    public void exportD3csv(String type) throws IOException {
        if("similarity".equalsIgnoreCase(type)) {
            List<List<String>> matrix = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader("exportedSelfSimilarityMatrix.csv"))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] values = line.split(CSV_SEPARATOR);
                    matrix.add(Arrays.asList(values));
                }
            }

            String[][] d3jsSimilarityMatrixAll = new String[((matrix.size()*matrix.get(1).size())*10)/16][3];
            String[][] d3jsSimilarityMatrix1 = new String[((matrix.size()*matrix.get(1).size())*10)/16][3];
            String[][] d3jsSimilarityMatrix2 = new String[((matrix.size()*matrix.get(1).size())*10)/16][3];
            d3jsSimilarityMatrixAll[0][0] = "projectA";
            d3jsSimilarityMatrixAll[0][1] = "projectB";
            d3jsSimilarityMatrixAll[0][2] = "similarity";
            d3jsSimilarityMatrix1[0][0] = "projectA";
            d3jsSimilarityMatrix1[0][1] = "projectB";
            d3jsSimilarityMatrix1[0][2] = "similarity";
            d3jsSimilarityMatrix2[0][0] = "projectA";
            d3jsSimilarityMatrix2[0][1] = "projectB";
            d3jsSimilarityMatrix2[0][2] = "similarity";
            int rowCounter = 1;
            for(int row = 2; row < matrix.size()-1; row++) {
                for(int col = 3; col < matrix.get(1).size()-1; col++) {
                    if(row >= col) {
                        d3jsSimilarityMatrixAll[rowCounter][0] = matrix.get(row).get(2);
                        d3jsSimilarityMatrixAll[rowCounter][1] = matrix.get(1).get(col);
                        d3jsSimilarityMatrixAll[rowCounter][2] = matrix.get(row).get(col);

                        if(Double.parseDouble(matrix.get(row).get(col)) > 0.00) {
                            d3jsSimilarityMatrix1[rowCounter][0] = matrix.get(row).get(2);
                            d3jsSimilarityMatrix1[rowCounter][1] = matrix.get(1).get(col);
                            d3jsSimilarityMatrix1[rowCounter][2] = matrix.get(row).get(col);
                        }

                        if(Double.parseDouble(matrix.get(row).get(col)) > 0.10) {
                            d3jsSimilarityMatrix2[rowCounter][0] = matrix.get(row).get(2);
                            d3jsSimilarityMatrix2[rowCounter][1] = matrix.get(1).get(col);
                            d3jsSimilarityMatrix2[rowCounter][2] = matrix.get(row).get(col);
                        }
                        rowCounter++;
                    } else {
                        break;
                    }
                }
            }

            saveMatrixExceptNullToCsv(d3jsSimilarityMatrixAll, "exportedD3jsData-all.csv");
            saveMatrixExceptNullToCsv(d3jsSimilarityMatrix1, "exportedD3jsData-1.csv");
            saveMatrixExceptNullToCsv(d3jsSimilarityMatrix2, "exportedD3jsData-2.csv");
        }
    }
}