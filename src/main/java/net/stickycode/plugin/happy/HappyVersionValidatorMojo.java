package net.stickycode.plugin.happy;

import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static java.lang.String.join;

@Mojo(name = "validate", threadSafe = true, requiresProject = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class HappyVersionValidatorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${project.build.outputDirectory}/happy.versions", required = true)
  private String[] versionFiles = new String[] { "target/happy.versions" };

  @Parameter(defaultValue = "http://localhost", required = true)
  private String targetDomain = "http://localhost";

  @Parameter(defaultValue = "UTF-8", required = true)
  private String characterSet;

  @Parameter(defaultValue = "${descriptor}", required = true, readonly = true)
  private PluginDescriptor descriptor;

  @Parameter(defaultValue = "true", required = true)
  private boolean failBuild = true;

  @Parameter(defaultValue = "1000", required = true)
  private long connectTimeoutMillis = 1000;

  @Parameter(defaultValue = "1000", required = true)
  private long writeTimeoutMillis = 1000;

  @Parameter(defaultValue = "3000", required = true)
  private long readTimeoutMillis = 3000;

  @Parameter(defaultValue = "60", required = true)
  private long retryDurationSeconds = 3;

  @Parameter(defaultValue = "5", required = true)
  private long retryPeriodSeconds = 1;

  private OkHttpClient client;

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    setupHttpClient();

    Instant finish = Instant.now().plusSeconds(retryDurationSeconds);

    ApplicationValidationResults results = new ApplicationValidationResults();
    for (Application application : loadApplications(getVersionFiles())) {
      results.add(createRequest(application));
    }

    getLog().info(String.format("Validating for up to %ds, retrying every %ds as needed",
      retryDurationSeconds, retryPeriodSeconds));

    while (Instant.now().isBefore(finish) && !results.success())
      try {
        getLog().info(String.format("waiting on %d applications ", results.runningCount()));
        Thread.sleep(retryPeriodSeconds * 1000);
        if (Instant.now().isBefore(finish))
          for (ApplicationValidationCallback callback : results.failures()) {
            callback.reset();
            queueRequest(callback);
          }
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    getLog().info(results.toString());

    if (failBuild)
      if (results.hasFailures())
        throw new MojoFailureException(results.failureMessage());
  }

  String[] getVersionFiles() {
    return versionFiles;
  }

  MavenProject getProject() {
    return project;
  }

  void setupHttpClient() {
    getLog().info(String.format("http client timeouts connect:%dms read:%dms write:%dms",
      connectTimeoutMillis, readTimeoutMillis, writeTimeoutMillis));
    this.client = new OkHttpClient.Builder()
      .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS)
      .build();
  }

  void queueRequest(ApplicationValidationCallback callback) {
    Request request = new Request.Builder()
      .url(applicationUrl(callback.getContextPath()))
      .build();

    client.newCall(request).enqueue(callback);
  }

  ApplicationValidationCallback createRequest(Application application) {
    ApplicationValidationCallback callback = new ApplicationValidationCallback(application);
    queueRequest(callback);
    return callback;
  }

  URL applicationUrl(String contextPath) {
    try {
      URL url = new URL(getTargetDomain() + contextPath + "version");
      getLog().info(String.format("add url %s", url.toString()));
      return url;
    }
    catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  String getTargetDomain() {
    return targetDomain;
  }

  List<Application> loadApplications(String... paths) throws MojoFailureException {
    List<Application> applications = new ArrayList<>();
    for (String path : paths) {
      applications.addAll(loadApplication(Paths.get(path)));
    }

    if (applications.isEmpty())
      throw new MojoFailureException("No applications found on paths " + join(",", paths));

    getLog().info("Found applications " + applications);
    return applications;
  }

  private List<Application> loadApplication(Path path) throws MojoFailureException {
    getLog().info("loading application version from " + path.toString());
    try (BufferedReader reader = versionsFileReader(path);) {
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
      getLog().error(e);
      throw new MojoFailureException("failed to load application versions from " + path);
    }

  }

  BufferedReader versionsFileReader(Path path) throws IOException {
    return Files.newBufferedReader(path);
  }

}
