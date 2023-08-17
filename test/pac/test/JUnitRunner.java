package pac.test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import pac.util.Ansi;

import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class JUnitRunner extends JUnitCore {

  public static void main(String... args) {
    Class<?>[] testClasses = new Class<?>[args.length];
    int maxNameLen = 0;
    for (int i = 0; i < args.length; i++) {
      try {
        testClasses[i] = Class.forName(args[i]);

        // Find the maximum length test name.
        for (Method method : testClasses[i].getMethods()) {
          Test testAnnot = method.getAnnotation(Test.class);
          if (testAnnot != null) {
            maxNameLen = Math.max(maxNameLen, method.getName().length());
          }
        }
      } catch (ClassNotFoundException cnfe) {
        System.err.println("WARNING: unable to find class " + args[i]);
      }
    }

    File reportDir = new File("reports");
    reportDir.mkdir();
    if (!reportDir.exists()) {
      System.err.println("ERROR: unable to create directory: " + reportDir);
      System.exit(1);
    }

    JUnitCore runner = new JUnitRunner();
    XmlRunListener runListener = new XmlRunListener(maxNameLen);
    runner.addListener(runListener);

    int testCount = 0;
    int failureCount = 0;
    long runtime = 0;
    List<Failure> failures = new LinkedList<Failure>();

    // Run through all the test in serial.
    for (Class<?> testClass : testClasses) {
      runListener.setCurrentTest(testClass.getName());
      Result r = runner.run(testClass);
      testCount += r.getRunCount();
      failureCount += r.getFailureCount();
      runtime += r.getRunTime();
      failures.addAll(r.getFailures());
    }

    // Print out the details to any failures that were encountered.
    if (failureCount > 0) {
      System.out.println();
      System.out.println(
          "********************************************************************************");
      System.out.println("LIST OF FAILURES");
      System.out.println(
          "********************************************************************************\n");

      int count = 0;
      for (Failure failure : failures) {
        Description desc = failure.getDescription();
        System.out.printf("  %02d) %s.%s():%n%n", ++count, desc.getClassName(),
            desc.getMethodName());
        try {
          // Properly format the exception to the console.
          ByteArrayOutputStream buf = new ByteArrayOutputStream();
          failure.getException().printStackTrace(new PrintStream(buf));
          BufferedReader in = new BufferedReader(new StringReader(buf.toString()));
          String line;
          while ((line = in.readLine()) != null) {
            System.out.println("        " + line);
          }
        } catch (IOException e) {
          // Don't think it's possible to see this exception here.
        }
        System.out.println();
      }

      System.out.println(
          "********************************************************************************\n");
    }

    System.out.println("Total execution time: " + formatTime(runtime) + "s");
    System.out.println("Total number of tests: " + testCount);
    System.out.println("Total number of failures: " + failureCount);

    System.exit(failureCount);
  }

  /**
   * Formats the time specified in the form of '#.###`.
   * 
   * @param time (in milliseconds).
   * @return formatted time (in seconds).
   */
  public static final String formatTime(double time) {
    return String.format("%.3f", time / 1000d);
  }

  /**
   * Utility class for producing ant-like XML JUnit reports that are compatible with Hudson.
   * 
   * @author jeikenberry
   */
  public static class XmlRunListener extends RunListener {
    private static final Ansi failedAnsi = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.RED, null);
    private static final Ansi passedAnsi = new Ansi(Ansi.Attribute.BRIGHT, Ansi.Color.GREEN, null);

    private static PrintStream consoleOut = System.out, consoleErr = System.err;
    private static ByteArrayOutputStream stdout = new ByteArrayOutputStream(),
        stderr = new ByteArrayOutputStream();
    private static DocumentBuilder builder;
    static {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      try {
        builder = dbf.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }

    private Document xmlDoc;
    private String currentTest;
    private int maxNameLen;

    public XmlRunListener(int maxNameLen) {
      this.maxNameLen = maxNameLen;
    }

    @Override
    public void testRunStarted(Description description) throws Exception {
      // Set stdout and stderr so that we capture the test output/error.
      stdout.reset();
      stderr.reset();
      System.setOut(new PrintStream(stdout));
      System.setErr(new PrintStream(stderr));

      // Reset the document and add the root node.
      xmlDoc = builder.newDocument();
      SimpleDateFormat format = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss");
      Element root = xmlDoc.createElement("testsuite");
      root.setAttribute("errors", "0");
      root.setAttribute("failures", "0");
      root.setAttribute("hostname", InetAddress.getLocalHost().getHostName());
      root.setAttribute("name", currentTest);
      root.setAttribute("tests", "0");
      root.setAttribute("time", "0");
      root.setAttribute("timestamp", format.format(new Date()).replace(' ', 'T'));
      xmlDoc.appendChild(root);

      consoleOut.println(currentTest);
      consoleOut.println(currentTest.replaceAll(".", "-"));
    }

    public void setCurrentTest(String name) {
      currentTest = name;
    }

    private void addOutput(String tagName, String output) {
      Node outNode = xmlDoc.createElement(tagName);
      // Remove all non-printable characters from the output.
      Node cdata =
          xmlDoc.createCDATASection(output.replaceAll("[\\x00\\x08\\x0B\\x0C\\x0E-\\x1F]", ""));
      outNode.appendChild(cdata);
      xmlDoc.getChildNodes().item(0).appendChild(outNode);
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
      Element testSuite = (Element) xmlDoc.getChildNodes().item(0);
      testSuite.setAttribute("time", formatTime(result.getRunTime()));
      addOutput("system-out", stdout.toString());
      addOutput("system-err", stderr.toString());

      writeReport(new File("reports", "TEST-" + currentTest + ".xml"));

      consoleOut.println();

      // Reset back to console stdout/stderr.
      System.setOut(consoleOut);
      System.setErr(consoleErr);
    }

    public void writeReport(File reportFile) throws Exception {
      Transformer tf = TransformerFactory.newInstance().newTransformer();
      tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      tf.setOutputProperty(OutputKeys.INDENT, "yes");
      tf.transform(new DOMSource(xmlDoc), new StreamResult(new PrintWriter(reportFile)));
    }

    @Override
    public void testStarted(Description description) throws Exception {
      String methodName = description.getMethodName();
      int numOfSpaces = maxNameLen - methodName.length();
      int testCount = incrementCount((Element) xmlDoc.getChildNodes().item(0), "tests");
      consoleOut.printf("  %02d) %s", testCount, methodName);
      for (int i = 0; i < numOfSpaces; i++)
        consoleOut.print('.');
      consoleOut.flush();

      Element testSuite = (Element) xmlDoc.getChildNodes().item(0);
      Element testCase = xmlDoc.createElement("testcase");
      testSuite.appendChild(testCase);
      testCase.setAttribute("classname", description.getClassName());
      testCase.setAttribute("name", description.getMethodName());
      testCase.setAttribute("time", "" + System.currentTimeMillis());
    }

    private int incrementCount(Element node, String name) {
      String val = node.getAttribute(name);
      int count = Integer.parseInt(val) + 1;
      node.setAttribute(name, "" + count);
      return count;
    }

    @Override
    public void testFinished(Description description) throws Exception {
      long end = System.currentTimeMillis();
      NodeList testCases = xmlDoc.getElementsByTagName("testcase");
      Element currentTestCase = (Element) testCases.item(testCases.getLength() - 1);
      long start = Long.parseLong(currentTestCase.getAttribute("time"));
      String runtime = formatTime(end - start);
      currentTestCase.setAttribute("time", runtime);

      NodeList failures = currentTestCase.getElementsByTagName("failure");
      NodeList errors = currentTestCase.getElementsByTagName("error");

      // Report whether the test passed or failed.
      String result;
      if (failures.getLength() == 0 && errors.getLength() == 0) {
        result = passedAnsi.colorize("passed");
      } else {
        result = failedAnsi.colorize("failed");
      }
      consoleOut.printf("...%s [%ss]%n", result, runtime);
    }

    @Override
    public void testFailure(Failure failure) throws Exception {
      Throwable t = failure.getException();
      String tagName;
      if (t instanceof AssertionError)
        tagName = "failure";
      else
        tagName = "error";

      // Increment the total number of failures.
      incrementCount((Element) xmlDoc.getChildNodes().item(0), tagName + "s");

      NodeList testCases = xmlDoc.getElementsByTagName("testcase");
      Element currentTestCase = (Element) testCases.item(testCases.getLength() - 1);
      Element failureEle = xmlDoc.createElement(tagName);
      failureEle.setAttribute("message", t.getMessage());
      failureEle.setAttribute("type", t.getClass().getName());
      ByteArrayOutputStream buf = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(buf));
      failureEle.setTextContent(buf.toString());
      currentTestCase.appendChild(failureEle);
    }
  }
}
