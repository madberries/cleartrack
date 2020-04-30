package pac.agent;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.ClassWriter;
import pac.org.objectweb.asm.MethodWriterDelegate;
import pac.org.objectweb.asm.Type;

/**
 * Use this subclass of ClassWriter so that stack frames will be
 * reconstructed (if necessary).
 * 
 * @author jeikenberry
 */
public class CleartrackClassWriter extends ClassWriter {
    public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

    public CleartrackClassWriter(int flags, MethodWriterDelegate delegate) {
        super(flags, delegate);
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        if (hierarchy.isDangerousClass(type1) || hierarchy.isDangerousClass(type2))
            return getCommonSuperClass0(type1, type2);
        try {
            ClassLoader classLoader = getClass().getClassLoader();
            Class<?> c = Class.forName(type1.replace('/', '.'), false, classLoader);
            Class<?> d = Class.forName(type2.replace('/', '.'), false, classLoader);
            if (c.isAssignableFrom(d)) {
                return type1;
            }
            if (d.isAssignableFrom(c)) {
                return type2;
            }
            if (c.isInterface() || d.isInterface()) {
                return "java/lang/Object";
            } else {
                do {
                    c = c.getSuperclass();
                } while (!c.isAssignableFrom(d));
                return c.getName().replace('.', '/');
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return getCommonSuperClass0(type1, type2);
        }
    }

    private String getCommonSuperClass0(String type1, String type2) {
        Type c = Type.getObjectType(type1);
        Type d = Type.getObjectType(type2);
        if (hierarchy.isAssignableFrom(c, d)) {
            return type1;
        }
        if (hierarchy.isAssignableFrom(d, c)) {
            return type2;
        }
        if (hierarchy.isInterface(c) || hierarchy.isInterface(d)) {
            return "java/lang/Object";
        } else {
            do {
                c = hierarchy.getSuperClass(c);
            } while (!hierarchy.isAssignableFrom(c, d));
            return c.getInternalName();
        }
    }
}
