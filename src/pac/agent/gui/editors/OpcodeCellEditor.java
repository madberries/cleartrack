package pac.agent.gui.editors;

import java.awt.Component;
import java.lang.reflect.InvocationTargetException;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellEditor;

import pac.ca.odell.glazedlists.GlazedLists;
import pac.ca.odell.glazedlists.swing.AutoCompleteSupport;
import pac.org.objectweb.asm.tree.AbstractInsnNode;

/**
 * CellEditor for editing the instructions opcode. Selection is done via a combo box. Any change
 * will result in removing the current instruction and added a new type of the selected opcode.
 * 
 * @author jeikenberry
 */
public class OpcodeCellEditor extends AbstractCellEditor implements TableCellEditor {
  private static final long serialVersionUID = -7580319437991574184L;

  /** The core component for selecting new instruction types */
  private OpcodeComboBox comboBox;

  public OpcodeCellEditor() {
    comboBox = new OpcodeComboBox();
    // comboBox.addActionListener(new ActionListener() {
    //   @Override
    //   public void actionPerformed(ActionEvent e) {
    //     fireEditingStopped();
    //   }
    // });

    // Install "content-assist-like" support for the opcode combo-box to make selecting items
    // faster.
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          AutoCompleteSupport.install(comboBox, GlazedLists.eventListOf(Opcode.values()));
        }
      });
    } catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void fireEditingStopped() {
    super.fireEditingStopped();
  }

  @Override
  public Object getCellEditorValue() {
    return comboBox.getSelectedItem();
  }

  @Override
  public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected,
      int row, int column) {
    if (value instanceof AbstractInsnNode) {
      int insnOpcode = ((AbstractInsnNode) value).getOpcode();
      for (Opcode opcode : Opcode.values()) {
        if (opcode.getOpcode() == insnOpcode) {
          comboBox.setSelectedItem(opcode);
          break;
        }
      }
    } else {
      // If the value is not of type AbstractInsnNode, i.e. null, then just select the NOP
      // instruction by default.
      comboBox.setSelectedItem(Opcode.NOP);
    }
    return comboBox;
  }

  /**
   * Opcodes copied from org.objectweb.asm.Opcodes, but converted into an enum type.
   * 
   * @author jeikenberry
   */
  public enum Opcode {
    AALOAD(50), // -
    AASTORE(83), // -
    ACONST_NULL(1), // -
    // ALOAD_0(42), // -
    // ALOAD_1(43), // -
    // ALOAD_2(44), // -
    ALOAD(25), // -
    // ALOAD_3(45), // -
    ANEWARRAY(189), // visitTypeInsn
    ARETURN(176), // -
    ARRAYLENGTH(190), // visitInsn
    // ASTORE_0(75), // -
    // ASTORE_1(76), // -
    // ASTORE_2(77), // -
    // ASTORE_3(78), // -
    ASTORE(58), // -
    ATHROW(191), // -
    BALOAD(51), // -
    BASTORE(84), // -
    BIPUSH(16), // visitIntInsn
    CALOAD(52), // -
    CASTORE(85), // -
    CHECKCAST(192), // visitTypeInsn
    D2F(144), // -
    D2I(142), // -
    D2L(143), // -
    DADD(99), // -
    DALOAD(49), // -
    DASTORE(82), // -
    DCMPG(152), // -
    DCMPL(151), // -
    DCONST_0(14), // -
    DCONST_1(15), // -
    DDIV(111), // -
    // DLOAD_0(38), // -
    // DLOAD_1(39), // -
    DLOAD(24), // -
    // DLOAD_2(40), // -
    // DLOAD_3(41), // -
    DMUL(107), // -
    DNEG(119), // -
    DREM(115), // -
    DRETURN(175), // -
    // DSTORE_0(71), // -
    // DSTORE_1(72), // -
    // DSTORE_2(73), // -
    // DSTORE_3(74), // -
    DSTORE(57), // -
    DSUB(103), // -
    DUP2(92), // -
    DUP2_X1(93), // -
    DUP2_X2(94), // -
    DUP(89), // -
    DUP_X1(90), // -
    DUP_X2(91), // -
    F2D(141), // -
    F2I(139), // -
    F2L(140), // -
    FADD(98), // -
    FALOAD(48), // -
    FASTORE(81), // -
    FCMPG(150), // -
    FCMPL(149), // -
    FCONST_0(11), // -
    FCONST_1(12), // -
    FCONST_2(13), // -
    FDIV(110), // -
    // FLOAD_0(34), // -
    // FLOAD_1(35), // -
    FLOAD(23), // -
    // FLOAD_2(36), // -
    // FLOAD_3(37), // -
    FMUL(106), // -
    FNEG(118), // -
    FREM(114), // -
    FRETURN(174), // -
    // FSTORE_0(67), // -
    // FSTORE_1(68), // -
    // FSTORE_2(69), // -
    // FSTORE_3(70), // -
    FSTORE(56), // -
    FSUB(102), // -
    GETFIELD(180), // -
    GETSTATIC(178), // visitFieldInsn
    GOTO(167), // -
    I2B(145), // -
    I2C(146), // -
    I2D(135), // -
    I2F(134), // -
    I2L(133), // visitInsn
    I2S(147), // -
    IADD(96), // -
    IALOAD(46), // visitInsn
    IAND(126), // -
    IASTORE(79), // visitInsn
    ICONST_0(3), // -
    ICONST_1(4), // -
    ICONST_2(5), // -
    ICONST_3(6), // -
    ICONST_4(7), // -
    ICONST_5(8), // -
    ICONST_M1(2), // -
    IDIV(108), // -
    IF_ACMPEQ(165), // -
    IF_ACMPNE(166), // -
    IFEQ(153), // visitJumpInsn
    IFGE(156), // -
    IFGT(157), // -
    IF_ICMPEQ(159), // -
    IF_ICMPGE(162), // -
    IF_ICMPGT(163), // -
    IF_ICMPLE(164), // -
    IF_ICMPLT(161), // -
    IF_ICMPNE(160), // -
    IFLE(158), // -
    IFLT(155), // -
    IFNE(154), // -
    IFNONNULL(199), IFNULL(198), // visitJumpInsn
    IINC(132), // visitIincInsn
    // ILOAD_0(26), // -
    // ILOAD_1(27), // -
    ILOAD(21), // visitVarInsn
    // ILOAD_2(28), // -
    // ILOAD_3(29), // -
    IMUL(104), // -
    INEG(116), // -
    INSTANCEOF(193), // -
    INVOKEINTERFACE(185), // -
    INVOKESPECIAL(183), // -
    INVOKESTATIC(184), // -
    INVOKEVIRTUAL(182), // visitMethodInsn
    IOR(128), // -
    IREM(112), // -
    IRETURN(172), // visitInsn
    ISHL(120), // -
    ISHR(122), // -
    // ISTORE_0(59), // -
    // ISTORE_1(60), // -
    // ISTORE_2(61), // -
    // ISTORE_3(62), // -
    ISTORE(54), // visitVarInsn
    ISUB(100), // -
    IUSHR(124), // -
    IXOR(130), // -
    JSR(168), // -
    L2D(138), // -
    L2F(137), // -
    L2I(136), // -
    LADD(97), // -
    LALOAD(47), // -
    LAND(127), // -
    LASTORE(80), // -
    LCMP(148), // -
    LCONST_0(9), // -
    LCONST_1(10), // -
    LDC(18), // visitLdcInsn
    // LDC2_W(20), // -
    // LDC_W(19), // -
    LDIV(109), // -
    // LLOAD_0(30), // -
    // LLOAD_1(31), // -
    LLOAD(22), // -
    // LLOAD_2(32), // -
    // LLOAD_3(33), // -
    LMUL(105), // -
    LNEG(117), // -
    LOOKUPSWITCH(171), // visitLookupSwitch
    LOR(129), // -
    LREM(113), // -
    LRETURN(173), // -
    LSHL(121), // -
    LSHR(123), // -
    // LSTORE_0(63), // -
    // LSTORE_1(64), // -
    // LSTORE_2(65), // -
    // LSTORE_3(66), // -
    LSTORE(55), // -
    LSUB(101), // -
    LUSHR(125), // -
    LXOR(131), // -
    MONITORENTER(194), // visitInsn
    MONITOREXIT(195), // -
    MULTIANEWARRAY(197), // visitMultiANewArrayInsn
    NEW(187), // visitTypeInsn
    NEWARRAY(188), // visitIntInsn
    NOP(0), // visitInsn
    POP2(88), // -
    POP(87), // -
    PUTFIELD(181), // -
    PUTSTATIC(179), // -
    RET(169), // visitVarInsn
    RETURN(177), // -
    SALOAD(53), // -
    SASTORE(86), // -
    SIPUSH(17), // -
    SWAP(95), // -
    TABLESWITCH(170);

    private int opcode;

    Opcode(int opcode) {
      this.opcode = opcode;
    }

    public int getOpcode() {
      return opcode;
    }
  }

  private class OpcodeComboBox extends JComboBox<Opcode> {

    private static final long serialVersionUID = -5465688539168963506L;

    public OpcodeComboBox() {
      super(Opcode.values());
      setMaximumRowCount(50);
    }

  }
}
