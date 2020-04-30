package pac.inst.taint;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;

@InstrumentationClass("java/net/ServerSocket")
public final class ServerSocketInstrumentation {

    // INSTRUMENTATION METHODS

    @InstrumentationMethod
    public static final Socket accept(ServerSocket server) throws IOException {
        // longer should be trusting files by
        // default.
        Socket socket = server.accept();
        socket.ss_server = true;
        SocketInstrumentation.setSocketTimeout(socket, true);
        return socket;
    }
    
}
