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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

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
import okhttp3.Response;

@Mojo(name = "validate", threadSafe = true, requiresProject = true, defaultPhase = LifecyclePhase.INTEGRATION_TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class HappyVersionValidatorMojo
    extends AbstractMojo {

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "META-INF/sticky/happy-versions", required = true)
  private String[] versionFiles = new String[] { "META-INF/sticky/happy-versions" };

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

  private ClassLoader classloader;

  private OkHttpClient client;

  @Override
  public void execute()
      throws MojoExecutionException, MojoFailureException {
    buildClasspath();
    setupHttpClient();

    Instant finish = Instant.now().plusSeconds(retryDurationSeconds);

    Set<String> expectedVersion = new HashSet<>();

    for (Application application : loadApplications(versionFiles)) {
      expectedVersion.add(application.getVersion());
    }

    getLog().info(String.format("Validating for up to %ds, retrying every %ds as needed",
      retryDurationSeconds, retryPeriodSeconds));

    boolean allApplicationVersionMatch = false;
    while (Instant.now().isBefore(finish) && !allApplicationVersionMatch)
      try {
        Thread.sleep(retryPeriodSeconds * 1000);
        if (Instant.now().isBefore(finish)) {
          Set<String> runtimeVersions = getRuntimeAppVersions();
          List<String> unmatchedVersions = expectedVersion.stream()
            .filter(version -> !runtimeVersions.contains(version))
            .collect(Collectors.toList());

          if (unmatchedVersions.isEmpty()) {
            getLog().info("All expected versions matched");
            allApplicationVersionMatch = true;
          }
          else {
            getLog().info(String.format("Unmatched versions: [ %s ]", String.join(",", unmatchedVersions)));
          }
        }
      }
      catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
  }

  private Set<String> getRuntimeAppVersions() throws MojoFailureException {
    Request request = new Request.Builder()
      .url(getTargetDomain() + "/versions")
      .build();
    try {
      getLog().info(String.format("Calling %s", request.url()));
      Response response = client.newCall(request).execute();
      if (response.body() == null || response.body().string().isEmpty()) {
        throw new MojoFailureException(String.format("Null or empty response from %s", request.url()));
      }
      String content = response.body().string();
      getLog().info("version info read from " + targetDomain + ":");
      getLog().info(content);
      String[] lines = content.split("\n");
      return new HashSet<>(Arrays.asList(lines));
    }
    catch (IOException e) {
      throw new MojoFailureException(String.format("Error calling %s ", request.url()), e);
    }
  }

  void buildClasspath() throws MojoFailureException {
    try {
      List<String> classpathElements = getProject().getCompileClasspathElements();
      classpathElements.add(getProject().getBuild().getOutputDirectory());
      classpathElements.add(getProject().getBuild().getTestOutputDirectory());
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

  MavenProject getProject() {
    return project;
  }

  void setupHttpClient() {
    getLog().info(String.format("http client timeouts connect:%dms read:%dms write:%dms",
      connectTimeoutMillis, readTimeoutMillis, writeTimeoutMillis));
    OkHttpClient.Builder builder = new OkHttpClient.Builder()
      .connectTimeout(connectTimeoutMillis, TimeUnit.MILLISECONDS)
      .writeTimeout(writeTimeoutMillis, TimeUnit.MILLISECONDS)
      .readTimeout(readTimeoutMillis, TimeUnit.MILLISECONDS);

    ignoreSSL(builder);

    this.client = builder
      .build();
  }

  void ignoreSSL(OkHttpClient.Builder builder) {
    // Create a trust manager that does not validate certificate chains
    final TrustManager[] trustAllCerts = new TrustManager[] {
      new X509TrustManager() {

        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException {
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
            throws CertificateException {
        }

        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
          return new java.security.cert.X509Certificate[] {};
        }
      }
    };

    // Install the all-trusting trust manager
    try {
      final SSLContext sslContext = SSLContext.getInstance("SSL");
      sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
      // Create an ssl socket factory with our all-trusting manager
      final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

      builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
      builder.hostnameVerifier(new HostnameVerifier() {

        @Override
        public boolean verify(String hostname, SSLSession session) {
          return true;
        }
      });
    }
    catch (KeyManagementException | NoSuchAlgorithmException e) {
      getLog().error("Failed to apply SSL ignorance. ", e);
    }
  }

  ApplicationValidationCallback createRequest(Application application) {
    ApplicationValidationCallback callback = new ApplicationValidationCallback(application);
    return callback;
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
        applications.addAll(loadApplication(url));
      }
    }

    if (applications.isEmpty())
      throw new MojoFailureException("No applications found on paths " + join(",", paths));

    getLog().info("Found applications " + applications);
    return applications;
  }

  private List<Application> loadApplication(URL url) throws MojoFailureException {
    getLog().info("loading application version from " + url.toString());
    try (InputStream stream = url.openStream();) {
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

      List<Application> applications = new ArrayList<>();
      while (reader.ready()) {
        String line = reader.readLine();
        applications.add(new Application(line));
      }

      if (applications.isEmpty())
        throw new MojoFailureException("No contents found in " + url.toString());

      return applications;

    }
    catch (IOException e) {
      getLog().error(e);
      throw new MojoFailureException("failed to load application versions from " + versionFiles);
    }

  }

  private Enumeration<URL> applicationUrls(String path) throws MojoFailureException {
    try {
      getLog().debug("loading versions for resources called " + path);
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
