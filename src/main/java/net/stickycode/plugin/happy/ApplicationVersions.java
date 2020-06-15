package net.stickycode.plugin.happy;

import static java.lang.String.join;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;

public class ApplicationVersions {

  private String[] versionFiles;

  private boolean validateApplicationsDirectly = true;

  List<Application> loadApplications() throws MojoFailureException {
    List<Application> applications = new ArrayList<>();
    for (String path : versionFiles) {
      applications.addAll(loadApplication(Paths.get(path)));
    }

    if (applications.isEmpty())
      throw new MojoFailureException("No applications found on paths " + join(",", versionFiles));

    return applications;
  }

  private List<Application> loadApplication(Path path) throws MojoFailureException {
    try (BufferedReader reader = Files.newBufferedReader(path);) {
      List<Application> applications = new ArrayList<>();
      while (reader.ready()) {
        String line = reader.readLine();
        applications.add(new Application(line));
      }

      if (applications.isEmpty())
        throw new MojoFailureException("No contents found in " + path.toString());

      return applications;

    }
    catch (IOException e) {
      throw new MojoFailureException("failed to load application versions from " + path, e);
    }

  }

  public boolean validateApplicationsDirectly() {
    return validateApplicationsDirectly;
  }

  public ApplicationVersions withVersionFiles(String... paths) {
    this.versionFiles = paths;
    return this;
  }
}
