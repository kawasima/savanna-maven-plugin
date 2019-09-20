package net.unit8.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Objects;

@Mojo(name = "roar", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class RoarMojo extends AbstractMojo {
    @Parameter(property = "savanna.roarToNoTests", defaultValue = "true")
    private boolean roarToNoTests;

    @Parameter(property = "savanna.roarToSkipTesting", defaultValue = "true")
    private boolean roarToSkipTesting;

    private LionBanner banner = new LionBanner();

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (roarToSkipTesting && Objects.nonNull(System.getProperty("skipTests"))) {
            getLog().warn(banner.roarToSkipTesting());
            throw new MojoFailureException("");
        }
        if (roarToSkipTesting && Objects.equals(System.getProperty("maven.test.skip"), "true")) {
            getLog().warn(banner.roarToSkipTesting());
            throw new MojoFailureException("");
        }
    }
}
