package net.stickycode.plugin.happy;

public class Application {

  private String contextPath;

  private String version;

  public Application(String line) {
    if (line == null)
      throw new InvalidApplicationVersionException(line);

    String[] split = line.split(":");
    if (split.length != 2)
      throw new InvalidApplicationVersionException(line);

    this.contextPath = split[0];
    this.version = split[1];
  }

  public String getContextPath() {
    return contextPath;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return version + "@" + contextPath;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((contextPath == null) ? 0 : contextPath.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Application other = (Application) obj;
    if (contextPath == null) {
      if (other.contextPath != null)
        return false;
    }
    else
      if (!contextPath.equals(other.contextPath))
        return false;
    if (version == null) {
      if (other.version != null)
        return false;
    }
    else
      if (!version.equals(other.version))
        return false;
    return true;
  }

}
