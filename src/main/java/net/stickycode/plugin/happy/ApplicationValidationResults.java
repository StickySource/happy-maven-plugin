package net.stickycode.plugin.happy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationValidationResults {

  private List<ApplicationValidationCallback> callbacks = new ArrayList<>();

  public void add(ApplicationValidationCallback callback) {
    callbacks.add(callback);
  }

  public boolean running() {
    for (ApplicationValidationCallback c : callbacks) {
      if (c.running())
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
    return callbacks.stream()
      .filter(x -> !x.success())
      .map(x -> x.failureMessage())
      .collect(Collectors.joining("\n"));
  }

}
