package net.stickycode.plugin.happy;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import okhttp3.OkHttpClient;
import okhttp3.Request;

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

}
