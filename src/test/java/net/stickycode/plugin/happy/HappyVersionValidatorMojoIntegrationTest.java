package net.stickycode.plugin.happy;

import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.assertj.core.util.Lists;
import org.junit.Test;

public class HappyVersionValidatorMojoIntegrationTest {

  @Test(expected = MojoFailureException.class)
  public void checkFail() throws MojoExecutionException, MojoFailureException, DependencyResolutionRequiredException {
    HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo() {

      @Override
      List<ApplicationVersions> getApplicationVersions() {
        return Lists.newArrayList(new ApplicationVersions().withVersionFiles("src/test/resources/happy.versions"));
      }
    };
    mojo.execute();
  }

  @Test
  public void emptyDoesNotFail() throws MojoExecutionException, MojoFailureException, DependencyResolutionRequiredException {
    HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo() {

      @Override
      List<ApplicationVersions> getApplicationVersions() {
        return Lists.emptyList();
      }
    };
    mojo.execute();
  }

}
