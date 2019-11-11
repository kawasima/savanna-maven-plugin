package net.unit8.maven.plugins;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Objects;

@Mojo(name = "roar", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class RoarMojo extends AbstractMojo {
    @Parameter(property = "savanna.roarToNoTests", defaultValue = "true")
    private boolean roarToNoTests;

    @Parameter(property = "savanna.roarToSkipTesting", defaultValue = "true")
    private boolean roarToSkipTesting;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    private LionBanner banner = new LionBanner();

    public void execute() throws MojoExecutionException, MojoFailureException {
        boolean skipTestsInConfig = project.getBuildPlugins()
                .stream()
                .filter(p -> Objects.equals(p.getGroupId(), "org.apache.maven.plugins")
                        && Objects.equals(p.getArtifactId(), "maven-surefire-plugin"))
                .findAny()
                .map(p -> (Xpp3Dom)p.getConfiguration())
                .filter(Objects::nonNull)
                .map(dom -> dom.getChild("skipTests"))
                .filter(Objects::nonNull)
                .map(dom -> Objects.equals(dom.getValue(), "true"))
                .orElse(false);

        if (roarToSkipTesting && (skipTestsInConfig || Objects.nonNull(System.getProperty("skipTests")))) {
            getLog().warn(banner.roarToSkipTesting());
            throw new MojoFailureException("");
        }
        if (roarToSkipTesting && Objects.equals(System.getProperty("maven.test.skip"), "true")) {
            getLog().warn(banner.roarToSkipTesting());
            throw new MojoFailureException("");
        }
    }
}
