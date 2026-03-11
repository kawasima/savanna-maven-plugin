package net.unit8.maven.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LionBanner {
    private static final String body;
    private static final ResourceBundle bundle;
    static {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                LionBanner.class.getResourceAsStream("/META-INF/savanna/lion.txt")))) {
            body = reader.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        bundle = ResourceBundle.getBundle("META-INF/savanna/roaring");
    }

    public String roar(String key) {
        return String.format(body, bundle.getString(key));
    }

    public String roarToNoTests() {
        return roar("noTests");
    }

    public String roarToSkipTesting() {
        return roar("skipTesting");
    }

    @Override
    public String toString() {
        return body;
    }
}
