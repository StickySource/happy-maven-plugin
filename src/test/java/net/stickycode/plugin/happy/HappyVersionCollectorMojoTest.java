package net.stickycode.plugin.happy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class HappyVersionCollectorMojoTest {

  @Test
  public void contextPath() {
    assertThat(mojo("some-application-v1").deriveContextPath()).isEqualTo("/application/v1");
    assertThat(mojo("some-application").deriveContextPath()).isEqualTo("/application");
    assertThat(mojo("some").deriveContextPath()).isEqualTo("/");
  }

  private HappyVersionCollectorMojo mojo(String artifactId) {
    return new HappyVersionCollectorMojo() {

      @Override
      String getArtifactId() {
        return artifactId;
      }
    };
  }

}
