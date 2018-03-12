package net.stickycode.plugin.happy;

@SuppressWarnings("serial")
public class InvalidApplicationVersionException
    extends RuntimeException {

  public InvalidApplicationVersionException(String line) {
    super("Expected /context/path:application-name-X.Y but got " + line);
  }

}
