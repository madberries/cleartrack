package pac.inst.taint;

import java.io.DataOutputStream;
import java.io.IOException;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass("java/io/DataOutputStream")
public final class DataOutputStreamInstrumentation extends OutputStreamInstrumentation {

    // INSTANCE METHODS

    @InstrumentationMethod
    public static final void writeBytes(DataOutputStream dout, String s, Ret ret) throws IOException {
        s = SocketInstrumentation.checkSocketOutput(dout, "DataOutputStream.writeBytes(String)", s, ret);
        dout.writeBytes(s, ret);
    }

    @InstrumentationMethod
    public static final void writeChars(DataOutputStream dout, String s, Ret ret) throws IOException {
        s = SocketInstrumentation.checkSocketOutput(dout, "DataOutputStream.writeChars(String)", s, ret);
        dout.writeChars(s, ret);
    }

    @InstrumentationMethod
    public static final void writeUTF(DataOutputStream dout, String s, Ret ret) throws IOException {
        s = SocketInstrumentation.checkSocketOutput(dout, "DataOutputStream.writeUTF(String)", s, ret);
        dout.writeUTF(s, ret);
    }

}
