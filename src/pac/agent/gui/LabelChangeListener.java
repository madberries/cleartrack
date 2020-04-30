package pac.agent.gui;

import java.util.EventListener;

/**
 * The listener interface for receiving LabelChangeEvent's.
 * 
 * @author jeikenberry
 */
public interface LabelChangeListener extends EventListener {
    
    /**
     * Indicates that a LabelNode has been added.
     * 
     * @param e LabelChangeEvent
     */
    public void labelAdded(LabelChangeEvent e);

    /**
     * Indicates that a LabelNode has been removed.
     * 
     * @param e LabelChangeEvent
     */
    public void labelRemoved(LabelChangeEvent e);
    
}
