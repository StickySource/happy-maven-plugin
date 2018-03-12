package net.stickycode.plugin.happy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "collect", threadSafe = true)
public class HappyVersionCollectorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(required = false)
  private String contextPath;

  @Parameter(defaultValue = "META-INF/sticky/happy-versions", required = true)
  private String path;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private String testDirectory;

  @Parameter(defaultValue = "UTF-8", required = true)
  private String characterSet;

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    File file = Paths.get(testDirectory, path).toFile();
    if (!file.getParentFile().mkdirs())
      throw new MojoFailureException("Failed to create directory " + file.getParentFile().getAbsolutePath());

    try (PrintWriter writer = new PrintWriter(file, characterSet);) {
      writer.println(deriveContextPath() + ":" + getArtifactId() + "-" + project.getVersion());
    }
    catch (FileNotFoundException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to write versions file " + file.getAbsolutePath());
    }
    catch (UnsupportedEncodingException e) {
      throw new MojoFailureException("Character set " + characterSet + " is not something that I understand");
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
