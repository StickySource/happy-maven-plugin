package net.stickycode.plugin.happy;

import java.io.IOException;
import java.net.Socket;

public class ImmediateCloseBlackholeHandler
    implements BlackholeHandler {

  @Override
  public void process(Socket client) {
    try {
      client.close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
