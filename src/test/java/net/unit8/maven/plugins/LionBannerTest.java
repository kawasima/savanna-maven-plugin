package net.unit8.maven.plugins;

import net.unit8.maven.plugins.smell.SmellType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LionBannerTest {
    @Test
    void containsWadaWhenSkipTesting() {
        assertThat(new LionBanner().roar(SmellType.SKIP_TESTING.getPropertyKey()))
                .contains("t_wada");
    }

    @Test
    void containsWadaWhenNoTest() {
        assertThat(new LionBanner().roar(SmellType.NO_TEST.getPropertyKey()))
                .contains("t_wada");
    }

    @Test
    void everySmellTypeHasABundleEntry() {
        LionBanner banner = new LionBanner();
        for (SmellType type : SmellType.values()) {
            assertThat(banner.roar(type.getPropertyKey()))
                    .as("bundle entry for %s", type.name())
                    .contains("t_wada");
        }
    }
}
