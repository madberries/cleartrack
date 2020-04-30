package pac.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import pac.inst.taint.FileInputStreamInstrumentation;
import pac.util.TaintUtils;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

public class TaintTrackTest {

    private void parseSax(String xmlPath, boolean shouldTrust)
            throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        InputStream xmlInput = new FileInputStream(xmlPath);
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new TaintSaxHandler(shouldTrust);
        saxParser.parse(xmlInput, handler);
    }

    private static void parseDom(String xmlPath, boolean shouldTrust)
            throws FileNotFoundException, SAXException, IOException, ParserConfigurationException {
        Document d = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new FileInputStream(xmlPath));
        OutputFormat format = new OutputFormat(d);
        format.setIndenting(true);

        //print xml for console 
        //XMLSerializer serializer = new XMLSerializer(System.out, format); 

        //save xml in string var 
        StringWriter writer = new StringWriter();
        XMLSerializer serializer = new XMLSerializer(writer, format);

        //process
        serializer.serialize(d);

        String xmlText = writer.toString();

        BufferedReader in = new BufferedReader(new StringReader(xmlText));
        String line;
        String trust = shouldTrust ? "TRUSTED" : "TAINTED";
        System.out.println("SHOULD BE ALL " + trust + ":");
        while ((line = in.readLine()) != null) {
            System.out.print(TaintUtils.createTaintDisplayLines(line));
            if (line.startsWith("<?")) {
                Assert.assertTrue("line from xml file should be trusted", TaintUtils.isTrusted(line));
                continue; // this line will always be trusted
            }
            Assert.assertTrue("line from xml file should be " + trust, TaintUtils.isTrusted(line) == shouldTrust);
        }
        System.out.println();
    }

    private static void parsePretty(String xmlContent)
            throws ParserConfigurationException, SAXException, IOException, TransformerException {
        Source xmlInput = new StreamSource(new StringReader(xmlContent));
        StringWriter stringWriter = new StringWriter();
        StreamResult xmlOutput = new StreamResult(stringWriter);
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 4);
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(xmlInput, xmlOutput);

        // TODO figure out why taint xml doesn't become tainted
        // xml when prettified
        boolean isTrusted = TaintUtils.isTrusted(xmlContent);
        BufferedReader in = new BufferedReader(new StringReader(xmlOutput.getWriter().toString()));
        String line;
        System.out.println("SHOULD BE ALL " + (isTrusted ? "TRUSTED" : "TAINTED") + ":");
        while ((line = in.readLine()) != null) {
            System.out.print(TaintUtils.createTaintDisplayLines(line));
        }
        System.out.println();
    }

    @Test
    public void xmlParseTest() throws ParserConfigurationException, SAXException, IOException, TransformerException {
        String filename1 = "test/pac/test/users.xml"; // always trusted
        String filename2 = "build.xml";
        boolean shouldTrust2 = FileInputStreamInstrumentation.shouldTrustContent(new File(filename2));
        parseSax(filename1, true);
        parseSax(filename2, shouldTrust2);
        parseDom(filename1, true);
        parseDom(filename2, shouldTrust2);

        String xmlContent = TaintUtils.trust("<root><child>aaa</child><child/></root>");
        parsePretty(xmlContent);
        TaintUtils.taint(xmlContent);
        parsePretty(xmlContent);
    }

    @Test
    public void xmlConversion() {
        List<String> fileList = getSmallDataSet();

        for (String fileName : fileList) {
            List<List<String>> rows = StatementInjectionTest.readCSV(fileName, "\t");
            for (List<String> arow : rows) {
                for (String cell : arow) {
                    assertTrue("Each cell read from file system should be tainted", TaintUtils.isTainted(cell));
                    //					TaintValues.taint(cell);
                    //					assertTrue("cell marked as tained should be tainted", TaintValues.isTainted(cell));
                }
            }
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            XMLEncoder toxml = new XMLEncoder(os);
            toxml.writeObject(rows);
            toxml.close();
            String asXml = os.toString();
            //			System.out.println(asXml);
            ByteArrayInputStream is = new ByteArrayInputStream(asXml.getBytes());

            XMLDecoder fromXml = new XMLDecoder(is);
            @SuppressWarnings("unchecked")
            List<List<String>> rowsback = (List<List<String>>) fromXml.readObject();

            for (List<String> arow : rowsback) {
                for (String cell : arow) {
                    /*
                     * FIXME once static analysis is fully working, we should really be checking to
                     * ensure that each cell is tainted.
                     */
                    assertTrue("Each cell marked as tainted before xml roundtrip should be tainted after roundtrip",
                               !TaintUtils.isTrusted(cell));
                }
            }

            fromXml.close();
        }
    }

    public List<String> getSmallDataSet() {
        List<String> fileList = new ArrayList<String>();
        fileList.add("test/pac/test/sql-inject-data/taint-track-1.tsv");
        fileList.add("test/pac/test/sql-inject-data/taint-track-2.tsv");
        fileList.add("test/pac/test/sql-inject-data/taint-track-3.tsv");
        fileList.add("test/pac/test/sql-inject-data/taint-track-4.tsv");
        fileList.add("test/pac/test/sql-inject-data/taint-track-5.tsv");
        for (String path : fileList) {
            // we must taint the paths otherwise content will be trusted now.
            TaintUtils.taint(path);
        }
        return fileList;
    }

    public List<String> getLargeDataSet() {
        List<String> fileList = new ArrayList<String>();
        fileList.add("test/pac/test/sql-inject-data/sql-stacked.tsv");
        fileList.add("test/pac/test/sql-inject-data/sql-boolean-blind.tsv");
        fileList.add("test/pac/test/sql-inject-data/sql-error.tsv");
        fileList.add("test/pac/test/sql-inject-data/sql-timed.tsv");
        //		fileList.add("test/pac/test/sql-inject-data/sql-union2.tsv");
        for (String path : fileList) {
            // we must taint the paths otherwise content will be trusted now.
            TaintUtils.taint(path);
        }
        return fileList;
    }

    // code for working with json.
    @Test
    public void jsonConversionSmallInput() {
        List<String> fileList = getSmallDataSet();
        jsonConversion(fileList);
    }

    @Test
    public void jsonConversionLargeInput() {
        List<String> fileList = getLargeDataSet();
        jsonConversion(fileList);
    }

    void jsonConversion(List<String> fileList) {
        for (String fileName : fileList) {
            long start = System.currentTimeMillis();
            List<List<String>> rows = StatementInjectionTest.readCSV(fileName, "\t");

            String asJson = null;
            byte asBytes[] = null;
            for (List<String> arow : rows) {
                for (String cell : arow) {
                    assertTrue("Each cell read from file system should be tainted", TaintUtils.isTainted(cell));
                }
            }

            ObjectMapper om = new ObjectMapper();
            try {
                asJson = om.writeValueAsString(rows);
                System.out.println("Json string length: " + asJson.length());
                //				 System.out.println(TaintValues.isTracked(asJson) +
                //				 " tainted\n" + asJson);
                assertFalse(TaintUtils.isTrusted(asJson));

                asBytes = om.writeValueAsBytes(rows);
                assertFalse(ByteArrayTaint.isTrusted(asBytes, 0, asBytes.length - 1));
            } catch (JsonGenerationException e1) {
                e1.printStackTrace();
            } catch (JsonMappingException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            System.out.println("Execution time(s): " + (System.currentTimeMillis() - start) / 1000);
        }
    }

    @Test
    public void jacksonOMTest() {
        ObjectMapper om = new ObjectMapper();
        System.out.println("Object mapper: " + om);
    }

    @Test
    public void testTaintedSplitter() {
        HashMap<String, String> injectiontypes = StatementInjectionTest.getInjectionData();

        for (Map.Entry<String, String> e : injectiontypes.entrySet()) {
            String fileName = e.getValue();
            List<List<String>> rows = StatementInjectionTest.readCSV(fileName, "\t");

            for (List<String> arow : rows) {
                for (String cell : arow) {
                    assertTrue("Each cell read from file system should be tainted", TaintUtils.isTainted(cell));
                }
            }

        }
    }

    @Test
    public void lineFromFile() {
        String fn = "test/pac/test/sql-inject-data/sql-boolean-blind.tsv";
        boolean shouldTrust = FileInputStreamInstrumentation.shouldTrustContent(new File(fn));
        System.out.println("Reading CSV data from: " + fn);
        BufferedReader in = null;

        try {
            in = new BufferedReader(new FileReader(fn));
            String aline;

            while ((aline = in.readLine()) != null) {
                //		    	System.out.println(TaintValues.isTainted(aline) + " = is tainted line " + aline);
                assertTrue("line read from a file should be marked as tainted",
                           TaintUtils.isTainted(aline) != shouldTrust);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Test
    public void fileURIURL() throws IOException, URISyntaxException {
        String fileName = TaintUtils.trust("test/pac/test/sql-inject-data/sql-boolean-blind.tsv");
        File f = new File(fileName).getCanonicalFile();
        String fpath = f.getPath();
        System.out.println(TaintUtils.createTaintDisplayLines(fpath));
        Assert.assertTrue("file path should be trusted", TaintUtils.isTrusted(fpath));

        String fabspath = f.getAbsolutePath();
        System.out.println(TaintUtils.createTaintDisplayLines(fabspath));
        Assert.assertTrue("absolute file path should be trusted", TaintUtils.isTrusted(fabspath));

        localFileToURI(f);

        URI uri = f.toURI();
        String uriPath = uri.getPath();
        System.out.println(TaintUtils.createTaintDisplayLines(uriPath));
        Assert.assertTrue("uri path should be trusted", TaintUtils.isTrusted(uriPath));

        URL u = uri.toURL();
        String surl = u.toString();

        System.out.println(TaintUtils.createTaintDisplayLines(surl));
        Assert.assertTrue("url.toString should be trusted", TaintUtils.isTrusted(surl));
    }

    @Test
    public void trackURITaint() throws URISyntaxException {
        String slashified_fn = "/home/prakash/mit/cleartrack/Instrumentation/test/pac/test/sql-inject-data/sql-boolean-blind.tsv";
        URI uri = new URI("file", null, slashified_fn, (String) null);
        String uriPath = uri.getPath();
        System.out.println(TaintUtils.createTaintDisplayLines(uriPath));
        Assert.assertTrue("uri path should be trusted", TaintUtils.isTrusted(uriPath));
    }

    @Test
    public void urlSaxInputStream() throws IOException, SAXException, ParserConfigurationException {
        String fileName = TaintUtils.trust("build.xml");
        File f = new File(fileName).getCanonicalFile();
        URL u = f.toURI().toURL();
        String s = u.toString();

        System.out.println(TaintUtils.createTaintDisplayLines(s));
        Assert.assertTrue("Should be Trusted. URL: " + s, TaintUtils.isTrusted(s));

        parse(s);
    }

    private void parse(String fileURL) throws SAXException, IOException, ParserConfigurationException {
        // Create a JAXP "parser factory" for creating SAX parsers
        javax.xml.parsers.SAXParserFactory spf = SAXParserFactory.newInstance();

        // Now use the parser factory to create a SAXParser object
        // Note that SAXParser is a JAXP class, not a SAX class
        javax.xml.parsers.SAXParser sp = spf.newSAXParser();
        // Create a SAX input source for the file argument
        org.xml.sax.InputSource input = new InputSource(fileURL);
        // Finally, tell the parser to parse the input and notify the handler
        sp.parse(input, new DefaultHandler());
    }

    public void localFileToURI(File inFile) throws URISyntaxException {
        File f = inFile.getAbsoluteFile();
        String sp = slashify(f.getPath(), f.isDirectory());

        if (sp.startsWith("//")) {
            sp = "//" + sp;
        }
        Assert.assertTrue("Slashified Path should be trusted", TaintUtils.isTrusted(sp));
        URI uri = new URI("file", null, sp, (String) null);
        String uriPath = uri.getPath();
        System.out.println(TaintUtils.createTaintDisplayLines(uriPath));
        Assert.assertTrue("uri path should be trusted", TaintUtils.isTrusted(uriPath));
    }

    private static String slashify(String path, boolean isDirectory) {
        String p = path;
        if (File.separatorChar != '/')
            p = p.replace(File.separatorChar, '/');
        if (!p.startsWith("/"))
            p = "/" + p;
        if (!p.endsWith("/") && isDirectory)
            p = p + "/";
        return p;
    }

    public class TaintSaxHandler extends DefaultHandler {
        private String assertMsg;
        private boolean shouldTrust;

        public TaintSaxHandler(boolean shouldTrust) {
            this.shouldTrust = shouldTrust;
            this.assertMsg = shouldTrust ? "All content from trusted XML should be trusted"
                    : "All content from tainted XML should be tainted";
        }

        @Override
        public void startDocument() throws SAXException {
        }

        @Override
        public void endDocument() throws SAXException {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            Assert.assertTrue(assertMsg, uri == null || uri.equals("") || TaintUtils.isTrusted(uri) == shouldTrust);
            Assert.assertTrue(assertMsg, localName == null || localName.equals("")
                    || TaintUtils.isTrusted(localName) == shouldTrust);
            Assert.assertTrue(assertMsg,
                              qName == null || qName.equals("") || TaintUtils.isTrusted(qName) == shouldTrust);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
        }

        @Override
        public void characters(char ch[], int start, int length) throws SAXException {
            if (length <= 0)
                return;

            // blocks consisting of only newlines will be entirely
            // trusted...
            int idx = start;
            int end = start + length - 1;
            while (idx <= end && ch[start] == '\n') {
                idx++;
            }
            if (idx > end)
                return;

            Assert.assertTrue(assertMsg, CharArrayTaint.isTrusted(ch, start, start + length - 1) == shouldTrust);
        }

        @Override
        public void ignorableWhitespace(char ch[], int start, int length) throws SAXException {
        }
    }
}
