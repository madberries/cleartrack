package pac.agent.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.swing.JTree;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pac.agent.gui.models.AnalysisModel;
import pac.agent.gui.renderers.AnalysisCellRenderer;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.MethodNode;

/**
 * The tree representation of the codehawk analysis XML file. The viewer will find and load the XML
 * for the entire class, then simply update the model when methods are loading into the tree.
 * 
 * TODO: This class should just go away, along with the analysis view.
 * 
 * @author jeikenberry
 */
public class Analysis extends JTree {

  private static final long serialVersionUID = -2942319198130708880L;
  private Document document;

  public Analysis(ClassNode classNode)
      throws ParserConfigurationException, SAXException, IOException {
    super(new AnalysisModel());

    // Locate the analysis for classNode.
    final File file = new File("codehawk", classNode.name);
    File parent = file.getParentFile();
    String[] matched = parent.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.startsWith(file.getName() + "_");
      }
    });

    try {
      if (matched != null && matched.length != 0 && matched[0] != null) {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = builder.parse(new File(parent, matched[0]));
      }
    } finally {
      setCellRenderer(new AnalysisCellRenderer());
    }
  }

  /**
   * Load methodNode into the tree from it's respective method/constructor XML element node.
   * 
   * @param methodNode MethodNode
   */
  public void setMethod(MethodNode methodNode) {
    if (document == null || methodNode == null)
      return;
    try {
      Node methodEle = findMethodElement(methodNode);
      ((AnalysisModel) getModel()).setRoot(methodEle);
      invalidate();
      revalidate();
      repaint();
    } catch (ParserConfigurationException | SAXException | IOException e1) {
      e1.printStackTrace();
    }
  }

  private Node findMethodElement(MethodNode methodNode)
      throws ParserConfigurationException, SAXException, IOException {
    boolean isConstructor = methodNode.name.equals("<init>");
    boolean isClassInit = methodNode.name.equals("<clinit>");
    String eleName;
    if (isClassInit)
      eleName = "class-constructor";
    else if (isConstructor)
      eleName = "constructor";
    else
      eleName = "method";
    NodeList methods = document.getElementsByTagName(eleName);
    if (isClassInit)
      return methods.getLength() > 0 ? methods.item(0) : null;
    for (int i = 0; i < methods.getLength(); i++) {
      Element methodEle = (Element) methods.item(i);
      if (!isConstructor && !methodEle.getAttributes().getNamedItem("name").getNodeValue()
          .equals(methodNode.name)) {
        continue; // method names don't match
      }

      // Check signatures:
      // -----------------
      // NodeList sigNodes = methodEle.getElementsByTagName("signature");
      // Element sigNode = (Element) sigNodes.item(0);
      // MethodSignature signature = MethodSignature.readXml(sigNode);
      // if (signature.getDescriptor().equals(methodNode.desc)) {
      //   return methodEle; // method found
      // }
    }
    return null;
  }

  /**
   * @return true iff we parsed the codehawk analysis file.
   */
  public boolean foundAnalysis() {
    return document != null;
  }
}
