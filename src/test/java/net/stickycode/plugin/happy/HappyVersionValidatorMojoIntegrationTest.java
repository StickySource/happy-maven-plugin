package net.stickycode.plugin.happy;

import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class HappyVersionValidatorMojoIntegrationTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  MavenProject project;

  @InjectMocks
  HappyVersionValidatorMojo mojo = new HappyVersionValidatorMojo();

  @Test(expected = MojoFailureException.class)
  public void checkFail() throws MojoExecutionException, MojoFailureException, DependencyResolutionRequiredException {
    when(project.getCompileClasspathElements()).thenReturn(new ArrayList<>());
    when(project.getBuild().getOutputDirectory()).thenReturn("target/classes");
    when(project.getBuild().getTestOutputDirectory()).thenReturn("target/test-classes");
    mojo.execute();
  }

}
