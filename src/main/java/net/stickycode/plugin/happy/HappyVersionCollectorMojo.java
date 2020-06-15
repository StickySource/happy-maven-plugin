package net.stickycode.plugin.happy;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

@Mojo(name = "collect", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_RESOURCES, requiresProject = true)
public class HappyVersionCollectorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(required = false)
  private String contextPath;

  @Parameter(defaultValue = "sticky/happy.versions", required = true)
  private String path;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private String targetDirectory;

  @Parameter(defaultValue = "UTF-8", required = true)
  private String characterSet;

  @Component
  private BuildContext buildContext;

  @Component
  private MavenProjectHelper projectHelper;

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    File file = Paths.get(targetDirectory, path).toFile();
    if (!file.getParentFile().exists())
      if (!file.getParentFile().mkdirs())
        throw new MojoFailureException("Failed to create directory " + file.getParentFile().getAbsolutePath());

    try (PrintWriter writer = new PrintWriter(outputWriter(file));) {
      writer.println(deriveContextPath() + ":" + getArtifactId() + "-" + project.getVersion());
    }

    getLog().info("application version stored in " + file.getAbsolutePath());
    projectHelper.attachArtifact(this.project, "versions", "happy", file);
  }

  private OutputStreamWriter outputWriter(File file) throws MojoFailureException {
    try {
      return new OutputStreamWriter(buildContext.newFileOutputStream(file), characterSet);
    }
    catch (UnsupportedEncodingException e) {
      getLog().error(e);
      throw new MojoFailureException("Character set " + characterSet + " is not something that I understand");
    }
    catch (IOException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to write versions file " + file.getAbsolutePath());
    }
  }

  String deriveContextPath() {
    if (contextPath != null)
      return contextPath;

    int indexOfHyphen = getArtifactId().indexOf("-");
    if (indexOfHyphen == -1)
      return "/";

    return getArtifactId().substring(indexOfHyphen).replaceAll("-", "/");
  }

  String getArtifactId() {
    return project.getArtifactId();
  }

}
