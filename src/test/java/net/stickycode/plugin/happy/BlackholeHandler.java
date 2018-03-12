package net.stickycode.plugin.happy;

import java.net.Socket;

public interface BlackholeHandler {

  void process(Socket client);

}
