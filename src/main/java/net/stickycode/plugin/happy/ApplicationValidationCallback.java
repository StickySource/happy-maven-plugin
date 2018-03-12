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

  private String applicationVersion;

  public ApplicationValidationCallback(String applicationVersion) {
    super();
    this.applicationVersion = applicationVersion;
  }

  @Override
  public void onResponse(Call call, Response response) throws IOException {
    BufferedReader reader = new BufferedReader(response.body().charStream());
    String line = reader.readLine();
    if (line.equals(applicationVersion))
      this.success = true;
    else {
      this.success = false;
      this.failure = "expected " + applicationVersion + " but was " + line;
    }
    this.running = false;
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
}