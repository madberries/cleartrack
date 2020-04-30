package pac.agent.gui.models;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import pac.agent.gui.tables.BytecodeTable;

/**
 * Represents a hiearchy of a single branch, and all of the intersecting (or contained)
 * sub-branches.  These are ordered in this way, such that the jumps will be drawn without
 * overlapping edges.  A jump can have multiple offsets that we brance from, and these
 * edges do overlap to produce a neat representation of the jumps.
 * 
 * @author jeikenberry
 */
public class Jump {
    public static int INSET = 10;

    /** array of all the jump from offsets */
    private int[] offsets;

    /** the jump to offset */
    private int gotoOffset;

    /** min/max offset bounds of this jump */
    private int min, max;

    /** 
     * all child jumps (this either intersect the parent jump, or
     * are perfectly contained in the parent jump).
     */
    private Set<Jump> children;

    /**
     * Creates a jump with a single origin offset, branching to gotoOffset.
     * 
     * @param offset int of the row we jump from
     * @param gotoOffset int of the row we jump to
     */
    public Jump(int offset, int gotoOffset) {
        this.offsets = new int[] { offset };
        this.gotoOffset = gotoOffset;
        min = Math.min(offset, gotoOffset);
        max = Math.max(offset, gotoOffset);
        this.children = new HashSet<Jump>();
    }

    /**
     * The supplied jump will merge with this, iff the two jumps branch
     * to the same instruction.
     * 
     * @param jump Jump
     * @return true if jump was successfully merged with this
     */
    protected boolean mergeWithExistingJump(Jump jump) {
        if (gotoOffset == jump.gotoOffset) {
            for (int from : jump.offsets)
                addOffset(from);
            return true;
        }
        for (Jump childJump : children) {
            if (childJump.mergeWithExistingJump(jump))
                return true;
        }
        return false;
    }

    /**
     * Joins this jump with the appropriate sub-jump, presuming that the
     * jump intersects.
     * 
     * @param jump Jump
     * @return false if there is no intersecting Jump, otherwise true
     */
    protected boolean join(Jump jump) {
        //        System.out.println("join " + this + " with " + jump);

        //        if (jump.gotoOffset == gotoOffset) {
        //            for (int from : jump.offsets)
        //                addOffset(from);
        //            return false;
        //        }

        // CASE 1: disjoint case (far left or far right)
        if (jump.max < min || jump.min > max) {
            //            System.out.println("no intersecting jump");
            return false;
        }

        // CASE 2: jump lies perfectly inside this
        if (jump.min > min && jump.max < max) {
            for (Jump childJump : children) {
                if (childJump.join(jump))
                    return true;
            }
            children.add(jump); // no existing region, so add new child.
            return true;
        }

        // CASE 3: this lies perfectly inside jump
        if (jump.min < min && jump.max > max) {
            // swap jump with this
            Set<Jump> tmpSet = children;
            children = jump.children;
            jump.children = tmpSet;
            int tmp = min;
            min = jump.min;
            jump.min = tmp;
            tmp = max;
            max = jump.max;
            jump.max = tmp;
            tmp = gotoOffset;
            gotoOffset = jump.gotoOffset;
            jump.gotoOffset = tmp;
            int[] tmpArr = offsets;
            offsets = jump.offsets;
            jump.offsets = tmpArr;
            //            System.out.println("swapped jumps");
            return join(jump);
        }

        // CASE 4: either a jump into or jump out of this
        Iterator<Jump> childIter = children.iterator();
        while (childIter.hasNext()) {
            Jump child = childIter.next();
            if (jump.min <= child.min && jump.max >= max) {
                childIter.remove();
                jump.children.add(child);
            } else if ((child.min > min && child.min < max) || (child.max < max && child.max > min)) {
                childIter.remove();
                jump.children.add(child);
                jump.min = Math.min(jump.min, child.min);
                jump.max = Math.min(jump.max, child.max);
            }
        }
        children.add(jump);
        min = Math.min(min, jump.min);
        max = Math.max(max, jump.max);
        return true;
    }

    /**
     * Add a jump from offset and recompute the jump bounds.
     * 
     * @param fromOffset int
     */
    public void addOffset(int fromOffset) {
        int[] tmp = new int[offsets.length + 1];
        System.arraycopy(offsets, 0, tmp, 0, offsets.length);
        tmp[offsets.length] = fromOffset;
        offsets = tmp;
        Arrays.sort(offsets);
        min = Math.min(offsets[0], gotoOffset);
        max = Math.max(offsets[offsets.length - 1], gotoOffset);
    }

    /**
     * @return int[] of all branching from offsets.
     */
    public int[] getOffsets() {
        return offsets;
    }

    /**
     * @return int of the branch to offset.
     */
    public int getGotoOffset() {
        return gotoOffset;
    }

    public int getHeight() {
        int height = 0;
        for (Jump jump : children) {
            height = Math.max(height, jump.getHeight());
        }
        return height + 1;
    }

    /**
     * Draws a branch onto table at a particular level number and at the
     * supplied offset relative to the instruction number column (in pixels).
     * The level represents the ordering in which the branches are drawn to the
     * table (lower comes first).  This is to prevent overlap of branches.
     * 
     * @param g2 Graphics2D of the graphics context
     * @param table BytecodeTable table to draw to
     * @param level int of the level order to draw to.
     * @param offset int of the pixel offset, relative to the first column.
     */
    public void draw(Graphics2D g2, BytecodeTable table, int level, int offset) {
        //        System.out.println("draw " + this + " at level " + level);
        boolean first = true;
        for (int from : offsets) {
            Rectangle rect1 = table.getCellRect(from, 2, true);
            Rectangle rect2 = table.getCellRect(gotoOffset, 1, true);

            BasicStroke stroke = new BasicStroke(.5f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
            g2.setStroke(stroke);

            int h1 = rect1.y + (rect1.height / 2);
            int h2 = rect2.y + (rect2.height / 2);
            int x = offset + (level * INSET);

            // FIXME Apparently clicking on a cell will only cause a repaint on
            // that row
            // Color color;
            // if (jump.from() == selection || jump.to() == selection)
            // color = Color.GREEN;
            // else
            // color = Color.BLACK;
            // g2.setColor(color);

            // Overlapping with anti-aliasing on can cause some weird darkening
            // effects, so let's prevent the overlap...
            if (first) {
                GeneralPath shape = new GeneralPath();
                shape.moveTo(rect1.x, h1);
                shape.lineTo(x, h1);
                shape.lineTo(x, h2);
                shape.lineTo(rect2.x, h2);
                g2.draw(shape);

                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(rect2.x - 10, h2 - 5);
                arrow.lineTo(rect2.x, h2);
                arrow.lineTo(rect2.x - 10, h2 + 5);
                g2.fill(arrow);
                first = false;
            } else {
                GeneralPath shape = new GeneralPath();
                shape.moveTo(rect1.x, h1);
                shape.lineTo(x, h1);
                g2.draw(shape);
            }
        }

        for (Jump childJump : children) {
            // System.out.println("enter child " + childJump);
            childJump.draw(g2, table, level + 1, offset);
        }
    }

    public int toHtml(StringBuilder buffer, int level) {
        int maxLevel = level;
        for (int from : offsets) {
            buffer.append("      drawJump(" + from + ", " + gotoOffset + ", " + level + ");\n");
        }
        for (Jump child : children) {
            maxLevel = Math.max(maxLevel, child.toHtml(buffer, level + 1));
        }
        return maxLevel;
    }

    @Override
    public String toString() {
        return "" + Arrays.toString(offsets) + " -> " + gotoOffset + " = " + children;
    }
}
