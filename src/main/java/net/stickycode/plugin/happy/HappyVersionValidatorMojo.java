package net.stickycode.plugin.happy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import okhttp3.OkHttpClient;
import okhttp3.Request;

@Mojo(name = "validate", threadSafe = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST)
public class HappyVersionValidatorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "/META-INF/sticky/happy-versions", required = true)
  private String[] versionFiles;

  @Parameter(defaultValue = "https://localhost", required = true)
  private String targetDomain;

  @Parameter(defaultValue = "UTF-8", required = true)
  private String characterSet;

  OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .writeTimeout(1, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build();

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    ApplicationValidationResults results = new ApplicationValidationResults();
    for (Application application : loadApplications(versionFiles)) {
      results.add(queueRequest(application));
    }

    while (results.running())
      try {
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    if (results.hasFailures())
      throw new MojoFailureException(results.failureMessage());
  }

  ApplicationValidationCallback queueRequest(Application application) {
    Request request = new Request.Builder()
      .url(applicationUrl(application.getContextPath()))
      .build();

    getLog().info("requesting " + request);
    ApplicationValidationCallback callback = new ApplicationValidationCallback(application.getVersion());
    client.newCall(request).enqueue(callback);
    return callback;
  }

  URL applicationUrl(String contextPath) {
    try {
      return new URL(getTargetDomain() + contextPath + "version");
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
      Enumeration<URL> urls = applicationUrls(path);
      while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        applications.add(loadApplication(url));
      }
    }
    return applications;
  }

  private Application loadApplication(URL url) throws MojoFailureException {
    getLog().info(url.toString());
    try (InputStream stream = url.openStream();) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
      String line = reader.readLine();
      if (line == null)
        throw new MojoFailureException("No contents found in " + url.toString());

      return new Application(line);

    }
    catch (IOException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to load application versions from " + versionFiles);
    }

  }

  private Enumeration<URL> applicationUrls(String path) throws MojoFailureException {
    try {
      getLog().info("loading versions from " + path);
      return getClass().getClassLoader().getResources(path);
    }
    catch (IOException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to load application versions " + path);
    }
  }

}
