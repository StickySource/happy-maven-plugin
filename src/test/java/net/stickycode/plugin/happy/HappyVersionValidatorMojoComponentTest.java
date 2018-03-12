package net.stickycode.plugin.happy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.StrictAssertions.assertThat;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class HappyVersionValidatorMojoComponentTest {

  @Test(timeout = 1000)
  public void requestFailure() throws MalformedURLException, InterruptedException {
    HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo() {

      String getTargetDomain() {
        return "http://localhost:7878";
      }
    };

    ApplicationValidationCallback queueRequest = mojo.queueRequest(new Application("/:blah-1.2"));
    while (queueRequest.running())
      Thread.sleep(100);

    assertThat(queueRequest.success()).isFalse();
  }

  @Test(timeout = 10000)
  public void requestClose() throws InterruptedException, IOException {
    try (Blackhole blackhole = new Blackhole();) {
      ApplicationValidationCallback queueRequest = request(blackhole);
      assertThat(queueRequest.success()).isFalse();
    }
  }

  @Test(timeout = 10000)
  public void requestTimeout() throws InterruptedException, IOException {
    try (Blackhole blackhole = new Blackhole(new DroppingBlackholeHandler());) {
      ApplicationValidationCallback queueRequest = request(blackhole);
      assertThat(queueRequest.success()).isFalse();
    }
  }

  private ApplicationValidationCallback request(Blackhole blackhole) throws MalformedURLException, InterruptedException {
    HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo() {

      String getTargetDomain() {
        return "http://localhost:" + blackhole.getLocalPort();
      }
    };

    ApplicationValidationCallback queueRequest = mojo.queueRequest(new Application("/:blah-1.2"));

    while (queueRequest.running())
      Thread.sleep(100);
    return queueRequest;
  }

  @Test(expected = MojoFailureException.class)
  public void noVersions() throws MojoFailureException {
    check("META-INF/sticky/no-versions");
  }

  @Test
  public void loadVersions() throws MojoFailureException {
    assertThat(check("META-INF/sticky/happy-versions")).contains(new Application("/:blah-1.2"));
  }

  private List<Application> check(String path) throws MojoFailureException {
    return new HappyVersionValidatorMojo().loadApplications(path);
  }

}
