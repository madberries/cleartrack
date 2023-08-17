package pac.agent;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import pac.org.objectweb.asm.tree.analysis.Frame;
import pac.org.objectweb.asm.util.Textifier;
import pac.org.objectweb.asm.util.TraceMethodVisitor;
import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.tree.analysis.Analyzer;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.util.CheckClassAdapter;

/**
 * Replaces CheckClassAdapter to utilize the CleartrackVerifier (instead of the standard
 * SimpleVerifier). This will enable accurate verification by making queries to the ClassHierarchy.
 * 
 * @author jeikenberry
 */
public class CleartrackCheckClassAdapter extends CheckClassAdapter {

  public CleartrackCheckClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  /**
   * Verifies the bytecode of the underlying class of {@code cr}.
   * 
   * @param cr a <code>ClassReader</code> that contains bytecode for the analysis.
   * @param loader a <code>ClassLoader</code> which will be used to load referenced classes. This is
   *        useful if you are verifying multiple interdependent classes.
   * @param dump true if bytecode should be printed out not only when errors are found.
   * @param pw write where results going to be printed.
   */
  public static void verify(final ClassReader cr, final ClassLoader loader, final boolean dump,
      final PrintWriter pw) {
    ClassNode cn = new ClassNode(Opcodes.ASM5);
    cr.accept(new CheckClassAdapter(cn, false), ClassReader.SKIP_DEBUG);

    Type syperType = cn.superName == null ? null : Type.getObjectType(cn.superName);
    List<MethodNode> methods = cn.methods;

    List<Type> interfaces = new ArrayList<Type>();
    for (Iterator<String> i = cn.interfaces.iterator(); i.hasNext();) {
      interfaces.add(Type.getObjectType(i.next().toString()));
    }

    for (int i = 0; i < methods.size(); ++i) {
      MethodNode method = methods.get(i);
      CleartrackVerifier verifier = new CleartrackVerifier(Type.getObjectType(cn.name), syperType,
          interfaces, (cn.access & Opcodes.ACC_INTERFACE) != 0);
      Analyzer<BasicValue> a = new Analyzer<BasicValue>(verifier);
      if (loader != null) {
        verifier.setClassLoader(loader);
      }
      try {
        a.analyze(cn.name, method);
        if (!dump) {
          continue;
        }
      } catch (Exception e) {
        e.printStackTrace(pw);
      }
      printAnalyzerResult(method, a, pw);
    }
    pw.flush();
  }

  static void printAnalyzerResult(MethodNode method, Analyzer<BasicValue> a, final PrintWriter pw) {
    Frame<BasicValue>[] frames = a.getFrames();
    Textifier t = new Textifier();
    TraceMethodVisitor mv = new TraceMethodVisitor(t);

    pw.println(method.name + method.desc);
    for (int j = 0; j < method.instructions.size(); ++j) {
      method.instructions.get(j).accept(mv);

      StringBuffer s = new StringBuffer();
      Frame<BasicValue> f = frames[j];
      if (f == null) {
        s.append('?');
      } else {
        for (int k = 0; k < f.getLocals(); ++k) {
          s.append(getShortName(f.getLocal(k).toString())).append(' ');
        }
        s.append(" : ");
        for (int k = 0; k < f.getStackSize(); ++k) {
          s.append(getShortName(f.getStack(k).toString())).append(' ');
        }
      }
      while (s.length() < method.maxStack + method.maxLocals + 1) {
        s.append(' ');
      }
      pw.print(Integer.toString(j + 100000).substring(1));
      pw.print(" " + s + " : " + t.text.get(t.text.size() - 1));
    }
    for (int j = 0; j < method.tryCatchBlocks.size(); ++j) {
      method.tryCatchBlocks.get(j).accept(mv);
      pw.print(" " + t.text.get(t.text.size() - 1));
    }
    pw.println();
  }

  private static String getShortName(final String name) {
    int n = name.lastIndexOf('/');
    int k = name.length();
    if (name.charAt(k - 1) == ';') {
      k--;
    }
    return n == -1 ? name : name.substring(n + 1, k);
  }

}
