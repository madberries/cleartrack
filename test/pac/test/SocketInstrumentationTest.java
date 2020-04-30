package pac.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class SocketInstrumentationTest {

    @Test
    public void testSocketInputStream() {
        try {
            Socket client = new Socket();
            client.connect(new InetSocketAddress("www.google.com", 80), 500);
            InputStream s = client.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(s));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
            out.println("GET http://www.google.com HTTP/1.0\n\n");
            String str = in.readLine();
            client.close();
            Assert.assertTrue("string read from socket is not tainted", TaintUtils.isTainted(str));
        } catch (IOException e) {
            Assert.fail(e.toString());
        }
    }
    
}
