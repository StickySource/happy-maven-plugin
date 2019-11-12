package net.stickycode.plugin.happy;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HappyVersionValidatorMojoComponentTest {

  private ApplicationValidationCallback request(Blackhole blackhole) throws MalformedURLException, InterruptedException {
    HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo() {

      String getTargetDomain() {
        return "http://localhost:" + blackhole.getLocalPort();
      }
    };
    mojo.setupHttpClient();

    ApplicationValidationCallback queueRequest = mojo.createRequest(new Application("/:blah-1.2"));

    while (queueRequest.running())
      Thread.sleep(100);
    return queueRequest;
  }

  @Test(expected = MojoFailureException.class)
  public void noVersions() throws MojoFailureException {
    check("META-INF/sticky/no-versions");
  }

  @Test(expected = MojoFailureException.class)
  public void versionsFileMissing() throws MojoFailureException {
    check("META-INF/sticky/what-file");
  }

  @Test
  public void loadVersions() throws MojoFailureException {
    assertThat(check("META-INF/sticky/happy-versions")).contains(new Application("/:blah-1.2"));
    assertThat(check("META-INF/sticky/happy-versions")).contains(new Application("/foo:foo-1.1"));
  }

  private List<Application> check(String path) throws MojoFailureException {
    return new HappyVersionValidatorMojo() {

      // can't build the classpath from the maven context now
      // as we don't have one
      ClassLoader getClassloader() {
        return getClass().getClassLoader();
      };
    }.loadApplications(path);
  }

}
