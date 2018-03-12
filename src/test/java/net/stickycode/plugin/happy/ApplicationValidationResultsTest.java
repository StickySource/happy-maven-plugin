package net.stickycode.plugin.happy;

import static org.assertj.core.api.StrictAssertions.assertThat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.junit.Test;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApplicationValidationResultsTest {

  @Test
  public void empty() {
    assertThat(results().running()).isFalse();
    assertThat(results().hasFailures()).isFalse();
    assertThat(results().failureMessage()).isEqualTo("");
  }

  private ApplicationValidationResults results(ApplicationValidationCallback... callbacks) {
    ApplicationValidationResults results = new ApplicationValidationResults();
    for (ApplicationValidationCallback c : callbacks) {
      results.add(c);
    }
    return results;
  }

  @Test
  public void oneRunning() {
    assertThat(results(callback()).running()).isTrue();
  }

  @Test
  public void oneException() {
    ApplicationValidationCallback callback = callback();
    callback.onFailure(null, new IOException("oops"));
    ApplicationValidationResults results = results(callback);
    assertThat(results.running()).isFalse();
    assertThat(results.hasFailures()).isTrue();
    assertThat(results.failureMessage()).isEqualTo("oops");
  }

  @Test
  public void oneSuccess() throws IOException {
    ApplicationValidationCallback callback = callback();
    callback.onResponse(null, response("blah-1.2"));
    ApplicationValidationResults results = results(callback);
    assertThat(results.running()).isFalse();
    assertThat(results.hasFailures()).isFalse();
    assertThat(results.failureMessage()).isEqualTo("");
  }

  @Test
  public void oneFailure() throws IOException {
    ApplicationValidationCallback callback = callback();
    callback.onResponse(null, response("blah-2.7"));
    ApplicationValidationResults results = results(callback);
    assertThat(results.running()).isFalse();
    assertThat(results.hasFailures()).isTrue();
    assertThat(results.failureMessage()).isEqualTo("expected blah-1.2 but was blah-2.7");
  }

  private Response response(String result) {
    return new Response.Builder()
      .request(request())
      .protocol(Protocol.HTTP_2)
      .body(body(result))
      .message("oops")
      .code(500)
      .build();
  }

  private Request request() {
    return new Request.Builder()
      .url("http://localhost:999/version")
      .get()
      .build();
  }

  private ResponseBody body(String value) {
    try {
      return ResponseBody.create(
        MediaType.parse("test/plain; charset=UTF-8"),
        value.getBytes("UTF-8"));
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  private ApplicationValidationCallback callback() {
    return new ApplicationValidationCallback("blah-1.2");
  }

}
