package net.unit8.maven.plugins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LionBannerTest {
    @Test
    void containsWada() {
        System.out.println(new LionBanner().toString());
    }

}