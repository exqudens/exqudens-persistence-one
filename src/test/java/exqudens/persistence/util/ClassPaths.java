package exqudens.persistence.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class ClassPaths {

    public static String toString(String classpath) {
        return toList(classpath).stream().collect(Collectors.joining(System.lineSeparator()));
    }

    public static List<String> toList(String classpath) {
        try (InputStream inputStream = ClassPaths.class.getClassLoader().getResourceAsStream(classpath)) {
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                    return bufferedReader.lines().collect(Collectors.toList());
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private ClassPaths() {
        super();
    }

}
