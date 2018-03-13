package net.stickycode.plugin.happy;

import static java.lang.String.join;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
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

import okhttp3.OkHttpClient;
import okhttp3.Request;

@Mojo(name = "validate", threadSafe = true, requiresProject = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class HappyVersionValidatorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "META-INF/sticky/happy-versions", required = true)
  private String[] versionFiles;

  @Parameter(defaultValue = "http://localhost", required = true)
  private String targetDomain;

  @Parameter(defaultValue = "UTF-8", required = true)
  private String characterSet;

  @Parameter(defaultValue = "${descriptor}", required = true)
  private PluginDescriptor descriptor;

  @Parameter(defaultValue = "true", required = true)
  private boolean failBuild;

  private ClassLoader classloader;

  OkHttpClient client = new OkHttpClient.Builder()
    .connectTimeout(1, TimeUnit.SECONDS)
    .writeTimeout(1, TimeUnit.SECONDS)
    .readTimeout(3, TimeUnit.SECONDS)
    .build();

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    buildClasspath();

    ApplicationValidationResults results = new ApplicationValidationResults();
    for (Application application : loadApplications(versionFiles)) {
      results.add(queueRequest(application));
    }

    while (results.running())
      try {
        getLog().info(String.format("waiting on %d applications ", results.runningCount()));
        Thread.sleep(100);
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }

    getLog().info(results.toString());

    if (failBuild)
      if (results.hasFailures())
        throw new MojoFailureException(results.failureMessage());
  }

  private void buildClasspath() throws MojoFailureException {
    try {
      List<String> classpathElements = project.getCompileClasspathElements();
      classpathElements.add(project.getBuild().getOutputDirectory());
      classpathElements.add(project.getBuild().getTestOutputDirectory());
      List<URL> urls = classpathElements.stream().map(x -> {
        try {
          return new File(x).toURI().toURL();
        }
        catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }).collect(Collectors.toList());
      this.classloader = new URLClassLoader(urls.toArray(new URL[classpathElements.size()]), getClass().getClassLoader());
    }
    catch (DependencyResolutionRequiredException e) {
      throw new MojoFailureException("Dependencies not resolved", e);
    }
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

    if (applications.isEmpty())
      throw new MojoFailureException("No applications found on paths " + join(",", paths));

    getLog().info("Found applications " + applications);
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
      return getClassloader().getResources(path);
    }
    catch (IOException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to load application versions " + path);
    }
  }

  ClassLoader getClassloader() {
    return classloader;
  }

}
