package pac.inst.taint;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;

@InstrumentationClass("java/nio/channels/ServerSocketChannel")
public final class ServerSocketChannelInstrumentation {

  // INSTRUMENTATION METHODS

  @InstrumentationMethod
  public static final SocketChannel accept(ServerSocketChannel serverChannel) throws IOException {
    SocketChannel channel = serverChannel.accept();
    SocketChannelInstrumentation.addServersideSocketChannel(channel);
    return channel;
  }

}
