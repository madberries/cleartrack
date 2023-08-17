package pac.agent.gui.models;

import java.util.LinkedList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.w3c.dom.CharacterData;

/**
 * This model represents a method (or constructor) node and it's children, for use with the Analysis
 * tree viewer.
 * 
 * @author jeikenberry
 */
public class AnalysisModel implements TreeModel {
  private List<TreeModelListener> listeners;
  private Node root;

  public AnalysisModel() {
    listeners = new LinkedList<TreeModelListener>();
  }

  /**
   * Set the root node to operate on. The node must be an Element, whose name is one of:
   * "class-constructor", "constructor", or "method".
   * 
   * @param root Node
   */
  public void setRoot(Node root) {
    Node oldRoot = this.root;
    this.root = root;
    // Determine the correct path to issue the tree structure change.
    if (oldRoot == null) {
      if (root == null)
        return;
      oldRoot = root;
    }
    for (TreeModelListener listener : listeners) {
      listener.treeStructureChanged(new TreeModelEvent(this, new Object[] {oldRoot}));
    }
  }

  @Override
  public Object getRoot() {
    return root;
  }

  @Override
  public Object getChild(Object parent, int index) {
    NodeList children = ((Node) parent).getChildNodes();
    if (index < 0 || index >= children.getLength())
      return null;
    int k = 0;
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Text || child instanceof CharacterData)
        continue; // do not include text, only include tags
      if (k++ == index)
        return child;
    }
    return null;
  }

  @Override
  public int getChildCount(Object parent) {
    NodeList children = ((Node) parent).getChildNodes();
    int count = 0;
    for (int i = 0; i < children.getLength(); i++) {
      Node child = children.item(i);
      if (child instanceof Text || child instanceof CharacterData)
        continue; // Do not include text, only tags.
      count++;
    }
    return count;
  }

  @Override
  public boolean isLeaf(Object node) {
    return getChildCount(node) == 0;
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // This tree is not editable (other than swapping out the root node itself).
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    if (parent == null || child == null)
      return -1;
    NodeList children = ((Node) parent).getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      if (children.item(i) == child)
        return i;
    }
    return -1;
  }

  @Override
  public void addTreeModelListener(TreeModelListener l) {
    listeners.add(l);
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l) {
    listeners.remove(l);
  }
}
