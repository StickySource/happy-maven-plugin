package net.stickycode.plugin.happy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ApplicationValidationResults {

  private List<ApplicationValidationCallback> callbacks = new ArrayList<>();

  public void add(Application application) {

  }

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
      if (!c.running() && !c.success())
        return true;

    return false;
  }

  public String failureMessage() {
    return callbacks.stream()
      .filter(x -> !x.running())
      .filter(x -> !x.success())
      .map(x -> x.failureMessage())
      .collect(Collectors.joining("\n"));
  }

  public Long runningCount() {
    return callbacks.stream()
      .filter(x -> x.running())
      .collect(Collectors.counting());
  }

  @Override
  public String toString() {
    return callbacks.toString();
  }

  public List<ApplicationValidationCallback> failures() {
    return callbacks.stream()
      .filter(x -> !x.success())
      .collect(Collectors.toList());
  }

  public boolean success() {
    for (ApplicationValidationCallback c : callbacks) {
      if (c.running() || !c.success())
        return false;
    }

    return true;
  }

}
