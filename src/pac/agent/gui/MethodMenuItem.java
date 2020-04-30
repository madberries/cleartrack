package pac.agent.gui;

import java.awt.Font;

import javax.swing.JRadioButtonMenuItem;

import pac.org.objectweb.asm.tree.MethodNode;

/**
 * Represents menu item in the Methods menu.  Each MethodMenuItem corresponds
 * to a MethodNode object.
 * 
 * @author jeikenberry
 */
public class MethodMenuItem extends JRadioButtonMenuItem {
    private static final long serialVersionUID = -2568436328674407262L;

    private MethodNode methodNode;

    public MethodMenuItem(MethodNode methodNode) {
        super(methodNode.name + methodNode.desc);

        this.methodNode = methodNode;

        if (methodNode.name.equals("<clinit>"))
            setFont(getFont().deriveFont(Font.ITALIC));
        else if (methodNode.name.equals("<init>"))
            setFont(getFont().deriveFont(Font.BOLD));

        setActionCommand(methodNode.name + methodNode.desc);
    }

    /**
     * @return MethodNode that operates this MethodMenuItem
     */
    public MethodNode getMethodNode() {
        return methodNode;
    }
}
