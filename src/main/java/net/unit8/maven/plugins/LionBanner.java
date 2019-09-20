package net.unit8.maven.plugins;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class LionBanner {
    private static String body;
    static {
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(
                LionBanner.class.getResourceAsStream("/META-INF/savanna/lion.txt")))) {
            body = reader.lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String roarToNoTests() {
        final ResourceBundle bundle = ResourceBundle.getBundle("META-INF/savanna/roaring");
        return String.format(body, bundle.getString("noTest"));
    }
    public String roarToSkipTesting() {
        final ResourceBundle bundle = ResourceBundle.getBundle("META-INF/savanna/roaring");
        return String.format(body, bundle.getString("skipTesting"));
    }

    @Override
    public String toString() {
        return body;
    }
}
