package edu.hm.ccwi.matilda.korpus.service.impl;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

public class JsonHeatmapUtil {

    BufferedWriter writer;

    public void createJson(List<List<String>> matrix, String jsonName) throws IOException {
        writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(jsonName), "UTF-8"));
        writer.write("{\"nodes\":[");

        for (int row = 2; row < matrix.size(); row++) {
            if(matrix.size()-1 == row) {
                writer.append("{\"name\":\"" + matrix.get(row).get(1) + "\",\"group\":1}");
            } else {
                writer.append("{\"name\":\"" + matrix.get(row).get(1) + "\",\"group\":1},");
            }
        }
        writer.write("], \"links\":[");

        for (int row = 2; row < matrix.size(); row++) {
            for (int col = 4; col < matrix.get(row).size(); col++) {
                String source = String.valueOf(row - 1);
                String target = String.valueOf(col - 3);
                String value = "0";
                if(matrix.get(row).get(col) != null && !matrix.get(row).get(col).equalsIgnoreCase("null") && ((int) ((Double.valueOf(matrix.get(row).get(col)).doubleValue()) * 100.00)) >= 10) {
                    double matrixValue = Double.valueOf(matrix.get(row).get(col)).doubleValue();
                    value = String.valueOf((int) (matrixValue * 100.00));
                    writer.append("{\"source\":" + source + ",\"target\":" + target + ",\"value\":" + value + "},");
                    writer.flush();
                }
            }
        }
        writer.append("]}");
        writer.close();
        System.err.println("Finished printing JsonHeatmapData.");
    }
}
