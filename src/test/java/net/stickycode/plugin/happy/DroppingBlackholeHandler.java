package net.stickycode.plugin.happy;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.Socket;

public class DroppingBlackholeHandler
    implements BlackholeHandler {

  @Override
  public void process(Socket client) {
    try {
      BufferedInputStream s = new BufferedInputStream(client.getInputStream());
      byte[] bytes = new byte[1024];
      for (;;) {
        s.read(bytes);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
