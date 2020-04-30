package pac.test;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

public class RandomAccessFileInstrumentationTest {

    @Test
    public void RandomAccessFileTest() {
        try {
            // ***
            // Test RandomAccessFile(File,String) when File location is not trusted that created RandomAccessFile is then tainted
            File file = File.createTempFile("prefix", "suffix");
            String mode = "rw";
            RandomAccessFile raf = new RandomAccessFile(file, mode); // FileNotFoundException
            Assert.assertTrue("RandomAccessFile(File) should create a tainted RandomAccessFile. file is from untrusted location",
                              TaintUtils.isTainted(raf));

            // ***
            // Test RandomAccessFile.getFD() that the returned FileDescriptor is  is tainted
            // because RandomAccessFile is tainted.
            FileDescriptor fd = raf.getFD(); // IOException
            Assert.assertTrue("RandomAccessFile.getFD where RandomAccessFile is tainted. The FileDescriptor should be tainted",
                              TaintUtils.isTainted(fd));

            raf.writeChars("A lot of characters filling this file.\n"); // IOException
            raf.writeChars("Some characters prettier than others.\n"); // IOException
            raf.seek(0); // IOException

            // ***
            // Test RandomAccessFile.read(byte[]) from tainted RandomAccessFile. Check that
            // the bytes read are tainted.
            byte[] bytes = new byte[4];
            raf.read(bytes); // IOException
            Assert.assertTrue("RandomAccessFile.read(byte[]) where RandomAccessFile is tainted. All bytes read should be tainted.",
                              ByteArrayTaint.isTainted(bytes, 0, 3));

            // ***
            // Test RandomAccessFile.read(byte[],int,int) from tainted RandomAccessFile.
            // Check that the bytes read are tainted, and that unwritten to areas in byte
            // array are not tainted.
            byte[] bytes_2 = new byte[30];
            raf.read(bytes_2, 4, 20);
            Assert.assertFalse("RandomAccessFile.read(byte[],int,int) where RandomAccessFile is tainted. Unfilled area of array should unknown.",
                               ByteArrayTaint.isTainted(bytes_2, 0, 4 - 1));
            Assert.assertTrue("RandomAccessFile.read(byte[],int,int) where RandomAccessFile is tainted. All bytes read should be tainted.",
                              ByteArrayTaint.isTainted(bytes_2, 4, 4 + 20 - 1));
            Assert.assertFalse("RandomAccessFile.read(byte[],int,int) where RandomAccessFile is tainted. Unfilled area of array should unknown.",
                               ByteArrayTaint.isTainted(bytes_2, 4 + 20, bytes_2.length - 1));

            // ***
            // Test RandomAccessFile.readFully(byte[]) from tainted RandomAccessFile.
            // Check that the byte array is filled with tainted chars.
            raf.seek(0);
            byte[] bytes_3 = new byte[10];
            raf.readFully(bytes_3); // IOException
            Assert.assertTrue("RandomAccessFile.readFully(byte[]) Where RandomAccessFile is tainted. All bytes read should be tainted.",
                              ByteArrayTaint.isTainted(bytes_3, 0, bytes_3.length - 1));

            // ***
            // Test RandomAccessFile.readFully(byte[],int,int) from tainted RandomAccessFile.
            // Check that the bytes read are tainted, and that unwritten to areas in byte
            // array are not tainted.
            byte[] bytes_4 = new byte[30];
            raf.read(bytes_4, 4, 20);
            Assert.assertFalse("RandomAccessFile.readFully(byte[],int,int) where RandomAccessFile is tainted. Unfilled area of array should unknown.",
                               ByteArrayTaint.isTainted(bytes_4, 0, 4 - 1));
            Assert.assertTrue("RandomAccessFile.readFully(byte[],int,int) where RandomAccessFile is tainted. All bytes read should be tainted.",
                              ByteArrayTaint.isTainted(bytes_4, 4, 4 + 20 - 1));
            Assert.assertFalse("RandomAccessFile.readFully(byte[],int,int) where RandomAccessFile is tainted. Unfilled area of array should unknown.",
                               ByteArrayTaint.isTainted(bytes_4, 4 + 20, bytes_4.length - 1));

            raf.close();

            // ***
            // Test RandomAccessFile(File,String) when File location IS trusted that created
            // RandomAccessFile is then trusted.
            file = new File("/etc/hosts");
            mode = "r";
            raf = new RandomAccessFile(file, mode); // FileNotFoundException
            Assert.assertTrue("RandomAccessFile(File) should create a tainted RandomAccessFile. file is from untrusted location",
                              TaintUtils.isTrusted(raf));

            // ***
            // Test RandomAccessFile.getFD() that the returned FileDescriptor is trusted
            // since RandomAccessFile is trusted.
            fd = raf.getFD(); // IOException
            Assert.assertTrue("RandomAccessFile.getFD where RandomAccessFile is trusted. The FileDescriptor should be trusted",
                              TaintUtils.isTrusted(fd));

            // ***
            // Test RandomAccessFile.read(byte[]) from trusted RandomAccessFile. Check that
            // the bytes read are trusted.
            bytes = new byte[4];
            raf.read(bytes); // IOException
            Assert.assertTrue("RandomAccessFile.read(byte[]) where RandomAccessFile is trusted. All bytes read should be trusted.",
                              ByteArrayTaint.isTrusted(bytes, 0, 3));

            // ***
            // Test RandomAccessFile.read(byte[],int,int) from trusted RandomAccessFile.
            // Check that the bytes read are trusted. That unwritten to areas in byte array
            // are not trusted.
            bytes_2 = new byte[30];
            raf.read(bytes_2, 4, 20);
            Assert.assertTrue("RandomAccessFile.read(byte[],int,int) where RandomAccessFile is trusted. Unfilled area of array should be trusted.",
                              ByteArrayTaint.isTrusted(bytes_2, 0, 4 - 1));

            // ***
            // Test RandomAccessFile.readFully(byte[]) from trusted RandomAccessFile. Check
            // that the bytes array is filled with trusted chars.
            raf.seek(0);
            bytes_3 = new byte[10];
            raf.readFully(bytes_3); // IOException
            Assert.assertTrue("RandomAccessFile.readFully(byte[]) Where RandomAccessFile is trusted. All bytes read should be trusted.",
                              ByteArrayTaint.isTrusted(bytes_3, 0, bytes_3.length - 1));

            // ***
            // Test RandomAccessFile.readFully(byte[],int,int) from trusnted RandomAccessFile.
            // Check that the bytes read are trusted. That unwritten to areas in byte array
            // are not tainted.
            bytes_4 = new byte[30];
            raf.read(bytes_4, 4, 20);
            Assert.assertTrue("RandomAccessFile.readFully(byte[],int,int) where RandomAccessFile is trusted. Unfilled area of array should trusted.",
                              ByteArrayTaint.isTrusted(bytes_4, 0, bytes.length - 1));

            raf.close();
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }
    
}
