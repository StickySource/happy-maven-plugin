package net.stickycode.plugin.happy;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Blackhole
    implements Runnable, Closeable {

  private ServerSocket socket;
  private BlackholeHandler handler;

  public Blackhole() {
    this(new ImmediateCloseBlackholeHandler());
  }

  public Blackhole(BlackholeHandler handler) {
    this.handler = handler;
    start();
  }

  public Blackhole start() {
    try {
      socket = new ServerSocket(0);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
    Thread thread = new Thread(this);
    thread.setDaemon(false);
    thread.start();

    return this;
  }

  @Override
  protected void finalize() throws Throwable {
    stop();
  }

  private void stop() {
    try {
      close();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void run() {
    try {
      Socket client = socket.accept();
      handler.process(client);
      stop();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  public int getLocalPort() {
    return socket.getLocalPort();
  }
}
