package net.stickycode.plugin.happy;

import org.junit.Test;

public class ApplicationTest {

  @Test(expected=InvalidApplicationVersionException.class)
  public void nullBad() {
    new Application(null);
  }

  @Test(expected=InvalidApplicationVersionException.class)
  public void nothing() {
    new Application("");
  }

}
