package net.unit8.maven.plugins;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LionBannerTest {
    @Test
    void containsWadaWhenSkipTesting() {
        assertThat(new LionBanner().roarToSkipTesting())
                .contains("t_wada");
    }

    @Test
    void containsWadaWhenNoTests() {
        assertThat(new LionBanner().roarToNoTests())
                .contains("t_wada");
    }

}