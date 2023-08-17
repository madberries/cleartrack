package pac.agent;

import java.util.List;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.tree.analysis.AnalyzerException;
import pac.org.objectweb.asm.tree.analysis.BasicValue;
import pac.org.objectweb.asm.tree.analysis.BasicVerifier;

/**
 * This is an extension of ASM's simple verifier (See {@code 
 * org.objectweb.asm.tree.analysis.SimpleVerifier}). The problem with ASM's verifier is that it
 * relies on all of the classes to be loaded into the relevant {@code ClassLoader}. We circumvent
 * that by handling all class relations via the {@code ClassHierarchy} that is constructed prior to
 * instrumentation.
 * 
 * @author jeikenberry
 */
public class CleartrackVerifier extends BasicVerifier {
  public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

  /**
   * The class that is verified.
   */
  private final Type currentClass;

  /**
   * The super class of the class that is verified.
   */
  private final Type currentSuperClass;

  /**
   * The interfaces implemented by the class that is verified.
   */
  private final List<Type> currentClassInterfaces;

  /**
   * If the class that is verified is an interface.
   */
  private final boolean isInterface;

  /**
   * The loader to use for referenced classes.
   */
  private ClassLoader loader = getClass().getClassLoader();

  /**
   * Constructs a new {@link SimpleVerifier}.
   */
  public CleartrackVerifier() {
    this(null, null, false);
  }

  /**
   * Constructs a new {@link SimpleVerifier} to verify a specific class. This class will not be
   * loaded into the JVM since it may be incorrect.
   * 
   * @param currentClass the class that is verified.
   * @param currentSuperClass the super class of the class that is verified.
   * @param isInterface if the class that is verified is an interface.
   */
  public CleartrackVerifier(final Type currentClass, final Type currentSuperClass,
      final boolean isInterface) {
    this(currentClass, currentSuperClass, null, isInterface);
  }

  /**
   * Constructs a new {@link SimpleVerifier} to verify a specific class. This class will not be
   * loaded into the JVM since it may be incorrect.
   * 
   * @param currentClass the class that is verified.
   * @param currentSuperClass the super class of the class that is verified.
   * @param currentClassInterfaces the interfaces implemented by the class that is verified.
   * @param isInterface if the class that is verified is an interface.
   */
  public CleartrackVerifier(final Type currentClass, final Type currentSuperClass,
      final List<Type> currentClassInterfaces, final boolean isInterface) {
    this(ASM5, currentClass, currentSuperClass, currentClassInterfaces, isInterface);
  }

  protected CleartrackVerifier(final int api, final Type currentClass, final Type currentSuperClass,
      final List<Type> currentClassInterfaces, final boolean isInterface) {
    super(api);
    this.currentClass = currentClass;
    this.currentSuperClass = currentSuperClass;
    this.currentClassInterfaces = currentClassInterfaces;
    this.isInterface = isInterface;
  }

  /**
   * Set the <code>ClassLoader</code> which will be used to load referenced classes. This is useful
   * if you are verifying multiple interdependent classes.
   * 
   * @param loader a <code>ClassLoader</code> to use.
   */
  public void setClassLoader(final ClassLoader loader) {
    this.loader = loader;
  }

  @Override
  public BasicValue newValue(final Type type) {
    if (type == null) {
      return BasicValue.UNINITIALIZED_VALUE;
    }

    boolean isArray = type.getSort() == Type.ARRAY;
    if (isArray) {
      switch (type.getElementType().getSort()) {
        case Type.BOOLEAN:
        case Type.CHAR:
        case Type.BYTE:
        case Type.SHORT:
          return new BasicValue(type);
      }
    }

    BasicValue v = super.newValue(type);
    if (BasicValue.REFERENCE_VALUE.equals(v)) {
      if (isArray) {
        v = newValue(type.getElementType());
        String desc = v.getType().getDescriptor();
        for (int i = 0; i < type.getDimensions(); ++i) {
          desc = '[' + desc;
        }
        v = new BasicValue(Type.getType(desc));
      } else {
        v = new BasicValue(type);
      }
    }
    return v;
  }

  @Override
  protected boolean isArrayValue(final BasicValue value) {
    Type t = value.getType();
    return t != null && ("Lnull;".equals(t.getDescriptor()) || t.getSort() == Type.ARRAY);
  }

  @Override
  protected BasicValue getElementValue(final BasicValue objectArrayValue) throws AnalyzerException {
    Type arrayType = objectArrayValue.getType();
    if (arrayType != null) {
      if (arrayType.getSort() == Type.ARRAY) {
        return newValue(Type.getType(arrayType.getDescriptor().substring(1)));
      } else if ("Lnull;".equals(arrayType.getDescriptor())) {
        return objectArrayValue;
      }
    }
    throw new Error("Internal error");
  }

  @Override
  protected boolean isSubTypeOf(final BasicValue value, final BasicValue expected) {
    Type expectedType = expected.getType();
    Type type = value.getType();
    switch (expectedType.getSort()) {
      case Type.INT:
      case Type.FLOAT:
      case Type.LONG:
      case Type.DOUBLE:
        return type.equals(expectedType);
      case Type.ARRAY:
      case Type.OBJECT:
        if ("Lnull;".equals(type.getDescriptor())) {
          return true;
        } else if (type.getSort() == Type.OBJECT) {
          return true;
        } else if (type.getSort() == Type.ARRAY) {
          switch (type.getElementType().getSort()) {
            case Type.OBJECT:
              return true;
            default:
              return isAssignableFrom(expectedType, type);
          }
        } else {
          return false;
        }
      default:
        throw new Error("Internal error");
    }
  }

  @Override
  public BasicValue merge(final BasicValue v, final BasicValue w) {
    if (!v.equals(w)) {
      Type t = v.getType();
      Type u = w.getType();
      if (t == null || u == null)
        return BasicValue.UNINITIALIZED_VALUE;

      int tSort = t.getSort();
      int uSort = u.getSort();
      if (tSort == Type.OBJECT || tSort == Type.ARRAY) {
        if (uSort == Type.OBJECT || uSort == Type.ARRAY) {
          if ("Lnull;".equals(t.getDescriptor())) {
            return w;
          }
          if ("Lnull;".equals(u.getDescriptor())) {
            return v;
          }
          if (isAssignableFrom(t, u)) {
            return v;
          }
          if (isAssignableFrom(u, t)) {
            return w;
          }

          // If both types are arrays, then the merged type will be some sort of array type. Unless
          // both types are primitive array types, in which case the merged type will be Object.
          if (tSort == Type.ARRAY && uSort == Type.ARRAY) {
            Type tEleType = t.getElementType();
            Type uEleType = u.getElementType();
            int tDims = t.getDimensions();
            int uDims = u.getDimensions();
            BasicValue objValue = new BasicValue(Type.getObjectType("java/lang/Object"));

            if (tEleType.getSort() == Type.OBJECT) {
              if (uEleType.getSort() == Type.OBJECT) {
                BasicValue eleMerge = merge(new BasicValue(tEleType), new BasicValue(uEleType));
                if (!eleMerge.equals(objValue)) {
                  // If the merged type of the array element types is not an object, then we can
                  // only return an array type of this merged type of the array dimensions of both
                  // types are the same.
                  if (tDims == uDims)
                    return toArrayValue(eleMerge, tDims);
                }
              } else {
                // Primitive arrays need an extra dimension to be compatible with Object array
                // types.
                uDims--;
              }
            } else {
              // Primitive arrays need an extra dimension to be compatible with Object array types.
              tDims--;
              // Check if both types are primitive arrays, and if so return Object as the merged type.
              if (uEleType.getSort() != Type.OBJECT)
                return objValue;
            }
            // Return an Object array type with tight dimension bounds.
            return toArrayValue(objValue, Math.min(tDims, uDims));
          }

          /*
           * TODO: should we look also for a common super interface? problem: there may be several
           * possible common super interfaces
           */
          do {
            if (t == null || isInterface(t)) {
              return BasicValue.REFERENCE_VALUE;
            }
            t = getSuperClass(t);
            if (t != null && isAssignableFrom(t, u)) {
              return newValue(t);
            }
          } while (true);
        }
      }
      return BasicValue.UNINITIALIZED_VALUE;
    }
    return v;
  }

  private BasicValue toArrayValue(BasicValue value, int dims) {
    Type t = value.getType();
    StringBuilder buf = new StringBuilder();
    for (int i = 0; i < dims; i++) {
      buf.append('[');
    }
    buf.append(t.getDescriptor());
    return new BasicValue(Type.getType(buf.toString()));
  }

  protected Type getSuperClass(Type t) {
    if (currentClass != null && t.equals(currentClass)) {
      return currentSuperClass;
    }
    try {
      Class<?> c = getClass(t).getSuperclass();
      return c == null ? null : Type.getType(c);
    } catch (ClassNotFoundException e) {
      return hierarchy.getSuperClass(t);
    }
  }

  protected boolean isInterface(Type t) {
    if (currentClass != null && t.equals(currentClass)) {
      return isInterface;
    }
    try {
      return getClass(t).isInterface();
    } catch (ClassNotFoundException e) {
      return hierarchy.isInterface(t);
    }
  }

  protected boolean isAssignableFrom(final Type t, final Type u) {
    if (t.equals(u)) {
      return true;
    }
    if (currentClass != null && t.equals(currentClass)) {
      if (getSuperClass(u) == null) {
        return false;
      } else {
        if (isInterface) {
          return u.getSort() == Type.OBJECT || u.getSort() == Type.ARRAY;
        }
        return isAssignableFrom(t, getSuperClass(u));
      }
    }
    if (currentClass != null && u.equals(currentClass)) {
      if (isAssignableFrom(t, currentSuperClass)) {
        return true;
      }
      if (currentClassInterfaces != null) {
        for (int i = 0; i < currentClassInterfaces.size(); ++i) {
          Type v = currentClassInterfaces.get(i);
          if (isAssignableFrom(t, v)) {
            return true;
          }
        }
      }
      return false;
    }

    try {
      Class<?> tc = getClass(t);
      if (tc.isInterface()) {
        tc = Object.class;
      }
      return tc.isAssignableFrom(getClass(u));
    } catch (ClassNotFoundException | NoClassDefFoundError e) {
      if (hierarchy.isInterface(t)) {
        return hierarchy.isAssignableFrom(Type.getType(Object.class), u);
      }
      return hierarchy.isAssignableFrom(t, u);
    }
  }

  protected Class<?> getClass(final Type t) throws ClassNotFoundException, NoClassDefFoundError {
    if (t.getSort() == Type.ARRAY) {
      return Class.forName(t.getDescriptor().replace('/', '.'), false, loader);
    }
    return Class.forName(t.getClassName(), false, loader);
  }
}
