package pac.agent.gui;

import pac.org.objectweb.asm.tree.LabelNode;

/**
 * An event which indicates that a LabelNode has been added or removed.
 * 
 * @author jeikenberry
 */
public class LabelChangeEvent {
  private LabelNode label;

  public LabelChangeEvent(LabelNode label) {
    this.label = label;
  }

  /**
   * @return LabelNode associated with this event.
   */
  public LabelNode getLabel() {
    return label;
  }
}
