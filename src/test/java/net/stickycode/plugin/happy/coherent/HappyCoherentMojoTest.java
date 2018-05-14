package net.stickycode.plugin.happy.coherent;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

public class HappyCoherentMojoTest {

  @Test(expected = MojoFailureException.class)
  public void sanity() throws MojoExecutionException, MojoFailureException {
    new HappyCoherentMojo().execute();
  }

}
