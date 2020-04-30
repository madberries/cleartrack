package pac.test;

import java.util.Map;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;

public class SystemInstrumentationTest {

    @Test
    public void arraycopyTest() {
        byte[] foo = TaintUtils.trust("01234567890123").getBytes();
        ByteArrayTaint.taint(foo, 3, 5);
        byte[] bar = new byte[10];

        System.arraycopy(foo, 0, bar, 0, 10);

        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bar, 0, 2));
        Assert.assertTrue("tainted region not marked as tainted", ByteArrayTaint.isTainted(bar, 3, 7));
        Assert.assertFalse("untainted region marked as tainted", ByteArrayTaint.isTainted(bar, 8, 9));
    }

    @Test
    public void mapTest() {
        final Map<String, String> map = System.getenv();

        if (map != null) {
            for (Entry<String, String> entry : map.entrySet()) {
                final String key = entry.getKey();
                final String value = entry.getValue();

                Assert.assertFalse("System.getenv has key " + key + " whose value " + value + " has unknown trust",
                                   TaintUtils.hasUnknown(value));
            }
        }
    }

    @Test
    public void getPropertyTest() {
        String key = "user.dir";
        String def = "/afs";
        String val;

        //***
        // Test System.getProperty(key) with untrusted key
        TaintUtils.taint(key);
        val = System.getProperty(key);
        Assert.assertTrue("System.getProperty(untrusted String) returned a property value", "".equals(val));

        //***
        // Test System.getProperty(key, def) with untrusted key and trusted def - def should be returned
        TaintUtils.taint(key);
        val = System.getProperty(key, def);
        Assert.assertTrue("System.getProperty(untrusted String) returned a property value", def.equals(val));

        //***
        // Test System.getProperty(key, def) with untrusted key and untrusted def - "" should be returned
        TaintUtils.taint(def);
        val = System.getProperty(key, def);
        Assert.assertTrue("System.getProperty(untrusted String) returned a property value", "".equals(val));

        // ***
        // Test System.getProperty(key) with trusted key
        TaintUtils.trust(key);
        val = System.getProperty(key);
        Assert.assertFalse("System.getProperty(trusted String) did not return the property value", "".equals(val));

        //***
        // Test System.getProperty(key, def) with trusted key
        TaintUtils.trust(def);
        val = System.getProperty(key, def);
        Assert.assertFalse("System.getProperty(trusted String, String def) did not return the property value",
                           "".equals(val));

    } // getPropertyTest()

    // TODO add tests for System.setIn instrumentation
}
