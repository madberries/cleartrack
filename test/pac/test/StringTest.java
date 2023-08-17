package pac.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

import pac.util.TaintUtils;

public class StringTest {
  private static Random rand = new Random();
  private StringBuffer buffer;

  public static final int MAX_LEN = 12, MAX_ITERS = 20000;

  public enum Action {
    INSERT, APPEND, DELETE, DELETE_CHAR, REVERSE, REPLACE;

    public static Action randomAction() {
      int pick = rand.nextInt(Action.values().length);
      return Action.values()[pick];
    }
  }

  @Test
  public void simpleContains() throws IOException {
    Map<String, Object> _classpaths = new HashMap<String, Object>();
    StringBuffer sbuf = new StringBuffer();
    sbuf.append("*\n");
    sbuf.append("All\n");

    BufferedReader buf = new BufferedReader(new StringReader(sbuf.toString()));
    String line = null;

    while ((line = buf.readLine()) != null) {
      if (!_classpaths.containsKey(line))
        _classpaths.put(line, new Object());
    }
    Set<String> parsed = _classpaths.keySet();
    Assert.assertTrue("Should contain All. Got " + parsed, parsed.contains("All"));

    Set<String> expected = new TreeSet<String>();
    expected.add("*");
    expected.add("All");
    assertEquals("Should contain All", expected, parsed);
  }

  @Test
  public void complicatedContains() throws IOException {
    StringBuffer sbuf = new StringBuffer();

    sbuf.append("# default\n");
    sbuf.append("$(jetty.home)/lib/spec.zip\n");
    sbuf.append("\n");
    sbuf.append("[*]\n");
    sbuf.append("$(jetty.home)/lib/io.jar\n");
    sbuf.append("$(jetty.home)/lib/util.jar\n");
    sbuf.append("\n");
    sbuf.append("[All,server,default]\n");
    sbuf.append("$(jetty.home)/lib/core.jar\n");
    sbuf.append("$(jetty.home)/lib/server.jar\n");
    sbuf.append("$(jetty.home)/lib/http.jar\n");
    sbuf.append("\n");
    sbuf.append("[All,xml,default]\n");
    sbuf.append("$(jetty.home)/lib/xml.jar\n");
    sbuf.append("\n");
    sbuf.append("[All,logging]\n");
    sbuf.append("$(jetty.home)/lib/LOGGING.JAR\n");

    Set<String> parsed = parse(new StringReader(sbuf.toString()));

    Set<String> expected = new HashSet<String>();
    expected.add("*");
    expected.add("All");
    expected.add("xml");
    expected.add("default");
    expected.add("server");
    expected.add("logging");

    assertEquals("Must have same ids", expected, parsed);
  }

  private Set<String> parse(Reader reader) throws IOException {
    BufferedReader buf = new BufferedReader(reader);
    String line = null;
    List<String> options = new ArrayList<String>();
    Map<String, Object> _classpaths = new HashMap<String, Object>();

    while ((line = buf.readLine()) != null) {
      String trim = line.trim();
      if (trim.startsWith("[") && trim.endsWith("]")) {
        String identifier = trim.substring(1, trim.length() - 1);

        // Normal case: section identifier (possibly separated by commas).
        options = Arrays.asList(identifier.split(","));
        for (String optionId : options) {
          if (!_classpaths.containsKey(optionId))
            _classpaths.put(optionId, new Object());
        }
        // System.out.println(_classpaths);
      }

    }
    return _classpaths.keySet();
  }

  private void assertEquals(String msg, Collection<String> expected, Collection<String> actual) {
    Assert.assertTrue(msg + " : expected should have an entry", expected.size() >= 1);
    Assert.assertEquals(msg + " : size", expected.size(), actual.size());
    for (String expectedVal : expected) {
      if (!actual.contains(expectedVal)) {
        System.out.println("actual: " + actual);
        System.out.println("Does not contain expectedVal = " + expectedVal);
        System.out.println("actual.contains(expectedVal) is " + actual.contains(expectedVal));
      }
      Assert.assertTrue(msg + " : should contain <" + expectedVal + ">",
          actual.contains(expectedVal));
    }
  }

  @Test
  public void threadTest() {
    buffer = new StringBuffer();
    buffer.append(randomString());
    MyThread[] threads = new MyThread[10];
    for (int i = 0; i < threads.length; i++) {
      threads[i] = new MyThread("" + (i + 1));
      threads[i].start();
    }
    for (int i = 0; i < threads.length; i++) {
      try {
        threads[i].join();
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  @Test
  public void scannerTest() {
    Exception caughtEx = null;
    try (Scanner s = new Scanner(new File("/etc/passwd"), "UTF-8").useDelimiter("\\A");) {
      while (s.hasNext()) {
        Assert.assertTrue("String scanned from a tainted source is not tainted",
            TaintUtils.isTainted(s.next()));
      }
    } catch (FileNotFoundException e) {
      caughtEx = e;
    }
    if (caughtEx != null)
      Assert.fail("There is no /etc/passwd file: " + caughtEx);
  }

  public static String randomString(int len) {
    int val = TaintUtils.random(rand);
    while (val == 0)
      val = TaintUtils.random(rand);
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < len; i++)
      buf.appendCodePoint(val);
    return buf.toString();
  }

  public static String randomString() {
    return randomString(rand.nextInt(MAX_LEN));
  }

  /**
   * Perform sanity check to ensure that taint is as we expect it and throw an assertion if we find
   * any discrepancies.
   */
  private void checkBuffer() {

    synchronized (buffer) {
      final int length = buffer.length();
      for (int offset = 0; offset < length;) {
        final int codepoint = buffer.codePointAt(offset);
        Assert.assertTrue("the codepoint (" + ((int) codepoint) + ") at offset " + offset
            + " does not match the taint.", TaintUtils.taintMatchesValue(codepoint));
        offset += Character.charCount(codepoint);
      }
    }
  }

  public class MyThread extends Thread {
    public MyThread(String name) {
      super(name);
    }

    @Override
    public void run() {
      for (int i = 1; i <= MAX_ITERS; i++) {
        try {
          int len = buffer.length();
          int start = (len == 0) ? 0 : rand.nextInt(len);
          int end = start + ((len - start <= 0) ? 0 : rand.nextInt(len - start));
          Action action = Action.randomAction();
          String str = randomString();
          switch (action) {
            case INSERT:
              buffer.insert(start, str);
              break;
            case APPEND:
              buffer.append(str);
              break;
            case REPLACE:
              buffer.replace(start, end, str);
              break;
            case DELETE:
              if (len <= 0)
                continue;
              buffer.delete(start, end);
              break;
            case DELETE_CHAR:
              if (len <= 0)
                continue;
              buffer.deleteCharAt(start);
              break;
            case REVERSE:
              buffer.reverse();
              break;
            default:
              // buffer.setCharAt(start, str.codePointAt(0));
              break;
          }
          checkBuffer();
          if (i % 2000 == 0)
            System.out.println("Thread " + getName() + " finished " + i + " StringBuffer tests...");
        } catch (StringIndexOutOfBoundsException e) {
          // We can get a StringIndexOutOfBoundsException since we do not synchronize when we
          // acquire the length of the StringBuffer.
        }
      }
    }
  }
}
