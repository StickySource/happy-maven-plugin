package net.stickycode.plugin.happy.coherent;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import net.stickycode.bootstrap.StickyBootstrap;

@Mojo(name = "coherent", threadSafe = true, defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class HappyCoherentMojo
    extends AbstractMojo {

  @Parameter(required = true)
  private String[] packagesToScan = {};

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      StickyBootstrap context = StickyBootstrap.crank().scan(packagesToScan);
      context.start();
      context.shutdown();
    }
    catch (RuntimeException e) {
      throw new MojoFailureException("Failed to sanity check context", e);
    }
  }

}
