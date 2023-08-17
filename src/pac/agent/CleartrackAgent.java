package pac.agent;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.ClassWriter;

public class CleartrackAgent {
  public static boolean agentLoaded, hierarchyLoaded;

  public static void premain(String args, Instrumentation inst) {
    agentLoaded = true;
    hierarchyLoaded = false;
    inst.addTransformer(new DynamicInstrumentationTransformer(), false);
  }

  private static class DynamicInstrumentationTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain, byte[] classfileBuffer)
        throws IllegalClassFormatException {
      // Dynamic classes will not be loaded on the bootstrap class path, so do not worry about this
      // case.
      if (loader == null)
        return classfileBuffer;

      // Not sure if we really want to instrument these JVM created classes.
      if (className.contains("$Proxy") || className.startsWith("sun/")
          || className.startsWith("com/sun/"))
        return classfileBuffer;

      ClassReader cr = new ClassReader(classfileBuffer);
      ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
      ClassVisitor visitor = cw;

      // TODO: Investigate why this must be loaded late?
      synchronized (ClassHierarchy.class) {
        if (!hierarchyLoaded) {
          try {
            ClassHierarchy.loadInstance();
            hierarchyLoaded = true;
          } catch (FileNotFoundException e) {
            System.err.println("WARNING: unable to read ClassHierarchy object from disk.");
          } catch (IOException e) {
            System.err.println("WARNING: unable to read ClassHierarchy object from disk.");
          } catch (ClassNotFoundException e) {
            System.err.println("WARNING: unable to locate ClassHierarchy class.");
          }
        }
      }

      visitor = new CleartrackInstrumentationAdapter(visitor);
      visitor = new CleartrackTaintClassAdapter(visitor, false, true);

      cr.accept(visitor, 0);
      return cw.toByteArray();
    }

  }
}
