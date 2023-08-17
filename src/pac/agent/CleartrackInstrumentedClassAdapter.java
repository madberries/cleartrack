package pac.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.tree.AnnotationNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.util.SSVersion;

/**
 * All this class does is mark a class as being instrumented with an annotation so that we can
 * determine whether or not we've already instrumented this class.
 * 
 * @author jeikenberry
 */
public class CleartrackInstrumentedClassAdapter extends ClassNode {
  private ClassVisitor cv;
  private List<String> options;

  public CleartrackInstrumentedClassAdapter(ClassVisitor cv, String[] options) {
    super(Opcodes.ASM5);
    this.cv = cv;
    this.options = Arrays.asList(options);
  }

  @Override
  public void visitEnd() {
    AnnotationNode annotNode = null;
    if (this.invisibleAnnotations == null) {
      this.invisibleAnnotations = new LinkedList<AnnotationNode>();
    } else {
      for (AnnotationNode annotation : this.invisibleAnnotations) {
        if (annotation.desc.equals("Lpac/inst/InstrumentedClass;")) {
          annotNode = annotation;
          break;
        }
      }
    }

    if (annotNode == null) {
      annotNode = new AnnotationNode("Lpac/inst/InstrumentedClass;");
      annotNode.values = new LinkedList<Object>();
      annotNode.values.add("cleartrack_rev");
      annotNode.values.add(SSVersion.revision);
      annotNode.values.add("cleartrack_opts");
      annotNode.values.add(options);
      this.invisibleAnnotations.add(annotNode);
    } else {
      annotNode.values.set(1, SSVersion.revision);
      @SuppressWarnings("unchecked")
      List<String> oldOptions = (List<String>) annotNode.values.get(3);
      oldOptions.addAll(options);
      Collections.sort(oldOptions);
    }

    accept(cv);
  }
}
