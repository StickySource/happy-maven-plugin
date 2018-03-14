package net.stickycode.plugin.happy;

import java.io.BufferedReader;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

final class ApplicationValidationCallback
    implements Callback {

  private boolean running = true;

  private boolean success;

  private String failure;

  private Application application;

  public ApplicationValidationCallback(Application application) {
    super();
    this.application = application;
  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    BufferedReader reader = new BufferedReader(response.body().charStream());
    String line = reader.readLine();
    if (line.equals(getApplicationVersion()))
      this.success = true;
    else {
      this.success = false;
      this.failure = "expected " + getApplicationVersion() + " but was " + line;
    }
    this.running = false;
  }

  String getApplicationVersion() {
    return application.getVersion();
  }

  @Override
  public void onFailure(Call call, IOException failure) {
    this.failure = failure.getMessage();
    this.success = false;
    this.running = false;
  }

  public boolean running() {
    return running;
  }

  public boolean success() {
    return success;
  }

  public String failureMessage() {
    return failure;
  }

  @Override
  public String toString() {
    if (running())
      return "checking " + getApplicationVersion();

    if (success())
      return "validated " + getApplicationVersion();

    return failure;
  }

  public String getContextPath() {
    return application.getContextPath();
  }

  public void reset() {
    this.success = false;
    this.failure = null;
    this.running = true;
  }

}