package edu.hm.ccwi.matilda.korpus.util;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CsvUtils {

    public synchronized static void createOrOverwriteNewCsv(String fileName) {
        URL resourceUrl = CsvUtils.class.getResource("/" + fileName);
        if(resourceUrl == null) {
            File f = new File(String.valueOf(CsvUtils.class.getResource("/")), fileName);
            new File(f, fileName);
        } else {
            File file = new File(String.valueOf(resourceUrl), fileName);
            file.delete();
            createOrOverwriteNewCsv(fileName);
        }
    }

    public synchronized static void appendToCsv(String fileName, String line, boolean addSpace) {
        BufferedOutputStream bos = null;
        try {
            URL resourceUrl = CsvUtils.class.getResource("/" + fileName);
            bos = new BufferedOutputStream(new FileOutputStream(new File(resourceUrl.toURI()), true));

            //write an entry for the category
            bos.write((line +"\n").getBytes("UTF-8"));
            if(addSpace) {
                bos.write("\n".getBytes("UTF-8"));
            }
            bos.flush();
        } catch (IOException e) {
            System.err.println("Exception on writing dependency to file: " + fileName);
        } catch (URISyntaxException e) {
            System.err.println("Couldn't cast URL to URI: " + fileName);
        }

        try {
            if(bos != null) { bos.close(); }
        } catch (IOException e) {
            System.err.println("Not able to close file: " + fileName);
        }
    }

    public String[] readCsv(String fileName) {
        List<String> lines = new ArrayList<>();
        InputStream is = getClass().getResourceAsStream("/" + fileName);
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);

        try (BufferedReader br = new BufferedReader(streamReader)) {
            String line = br.readLine();
            while (line != null) {
                lines.add(line);
                line = br.readLine();
            }
        } catch (IOException e) {
            System.err.println("Error while reading csv.");
        }

        return lines.toArray(String[]::new);
    }

    public static List<List<String>> readCsv(String fileName, String delimiter) {
        List<List<String>> lines = new ArrayList<>();
        InputStream is = CsvUtils.class.getResourceAsStream("/" + fileName);
        InputStreamReader streamReader = new InputStreamReader(is, StandardCharsets.UTF_8);

        try (BufferedReader br = new BufferedReader(streamReader)) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> values = Arrays.asList(line.split(delimiter));
                lines.add(values);
            }
        } catch (IOException e) {
            System.err.println("Error while reading csv.");
        }

        return lines;
    }

    public static String[][] toArray(List<List<String>> list) {
        String[][] r = new String[list.size()][];
        int i = 0;
        for (List<String> next : list) {
            r[i++] = next.toArray(new String[next.size()]);
        }
        return r;
    }
}
