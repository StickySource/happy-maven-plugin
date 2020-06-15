package net.stickycode.plugin.happy;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import okhttp3.OkHttpClient;
import okhttp3.Request;

@Mojo(name = "validate", threadSafe = true, requiresProject = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class HappyVersionValidatorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(required = true)
  private List<ApplicationVersions> applicationVersions;

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
    for (ApplicationVersions versions : getApplicationVersions()) {
      for (Application application : versions.loadApplications()) {
        if (versions.validateApplicationsDirectly())
          results.add(createRequest(application));
        else
          results.add(application);
      }
    }

    getLog().info("Found applications " + results);

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


  List<ApplicationVersions> getApplicationVersions() {
    return applicationVersions;
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

}
