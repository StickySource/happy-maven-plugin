package net.stickycode.plugin.happy;

import java.util.List;

public class ApplicationValidationResults {

  private List<ApplicationValidationCallback> callbacks;

  public void add(ApplicationValidationCallback callback) {
    callbacks.add(callback);
  }

  public boolean running() {
    for (ApplicationValidationCallback c : callbacks) {
      if (!c.running())
        return true;
    }
    
    return false;
  }

  public boolean hasFailures() {
    for (ApplicationValidationCallback c : callbacks)
      if (!c.success())
        return true;

    return false;
  }

  public String failureMessage() {
    StringBuilder b = new StringBuilder();
    for (ApplicationValidationCallback c : callbacks)
      if (!c.success())
        b.append(c.failureMessage()).append('\n');

    return b.toString();
  }

}
