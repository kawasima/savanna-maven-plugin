package com.example;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TextBlockSampleTest {

    @Test
    void usesTextBlock() {
        String json = """
                {"k":"v"}
                """;
        assertEquals("{\"k\":\"v\"}\n", json);
    }
}
