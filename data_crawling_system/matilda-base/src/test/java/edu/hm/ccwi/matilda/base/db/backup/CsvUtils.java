package edu.hm.ccwi.matilda.base.db.backup;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CsvUtils {

    public synchronized static void appendToCsv(Class clazz, String fileName, String line, boolean addSpace) {
        BufferedOutputStream bos = null;
        try {
            URL resourceUrl = clazz.getResource("/" + fileName);
            bos = new BufferedOutputStream(new FileOutputStream(new File(resourceUrl.toURI()), true));

            //write an entry for the category
            bos.write((line +"\n").getBytes(StandardCharsets.UTF_8));
            if(addSpace) {
                bos.write("\n".getBytes(StandardCharsets.UTF_8));
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

    public synchronized static List<String[]> readFromCsv(String fileName, String delimiter) throws IOException {
        List<String[]> list = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
                new FileReader(CsvUtils.class.getClassLoader().getResource(fileName).getFile()));
        String line;
        while ((line = reader.readLine()) != null) {
            list.add(line.split(delimiter));
        }
        reader.close();
        return list;
    }
}
