package pac.agent.gui.components;

import pac.org.objectweb.asm.tree.AbstractInsnNode;

/**
 * All JTable components that represent some sort of AbstractInsnNode object type should implement
 * this interface.
 * 
 * @author jeikenberry
 */
public interface InstructionComponent {

  /**
   * Set's the current instruction node.
   * 
   * @param inst AbstractInsnNode
   */
  public void setInstruction(AbstractInsnNode inst);

}
