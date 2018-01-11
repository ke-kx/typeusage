package de.tud.stg.analysis;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public class DatasetReader {

    public List<ObjectTrace> readObjects(String filename) throws Exception {
        return readObjects(filename, ObjectTrace.class);
    }

    //TODO replace this tokenizer with a string split oä -> also get rid of TU setters and have propper warnings if input is missformed
    public <T extends TypeUsage> List<T> readObjects(String filename, Class<T> clasz) throws Exception {
        List<T> readObjects = new LinkedList<T>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer tokenizer = new StringTokenizer(line, " ");

                T obj = clasz.newInstance();

                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    if (token.startsWith("location:")) {
                        obj.setLocation(token);
                    } else if (token.startsWith("context:")) {
                        obj.setContext(token);
                    } else if (token.startsWith("type:")) {
                        obj.setType(token);
                    } else if (token.startsWith("call:")) {
                        obj.addCall(token);
                    } else if (token.startsWith("extend:")) {
                        /* nothing */
                    } else {
                        obj.calls.add(token);
                    }
                } // end while
                readObjects.add(obj);
            }
        } catch (IOException e) {
            System.err.println("Problem loading dataset!");
            e.printStackTrace();
        }

        return readObjects;
    }

}
