package pac.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import pac.agent.hierarchy.ClassHierarchy;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.Type;
import pac.org.objectweb.asm.commons.CodeSizeEvaluator;
import pac.org.objectweb.asm.tree.AbstractInsnNode;
import pac.org.objectweb.asm.tree.ClassNode;
import pac.org.objectweb.asm.tree.InsnNode;
import pac.org.objectweb.asm.tree.IntInsnNode;
import pac.org.objectweb.asm.tree.LdcInsnNode;
import pac.org.objectweb.asm.tree.MethodNode;
import pac.org.objectweb.asm.util.Textifier;
import pac.org.objectweb.asm.util.TraceClassVisitor;
import pac.org.objectweb.asm.util.TraceMethodVisitor;

// CodeSizeEvaluator;
public class AsmUtils {
  public static final ClassHierarchy hierarchy = ClassHierarchy.getInstance();

  public static final String FIELD_SUFFIX = "_t";

  public static final int UNKNOWN_OPCODE = Opcodes.ICONST_1;
  public static final int TRUSTED_OPCODE = Opcodes.ICONST_0;
  public static final int TAINTED_OPCODE = Opcodes.ICONST_3;

  /**
   * It is possible that Java a class can contain fields all with the same name but each with a
   * different type. This is apparently legal with respect to the JavaVM, though I can't see how
   * anything like this would ever be compiled. To counteract this problem we must make any
   * duplicate primitive field it's own unique taint field (by appending the descriptor onto the
   * field name).
   * 
   * @param owner String of the owner classname (internal form).
   * @param origFieldName String of the original field name that we should instrument.
   * @param desc String of the descriptor of the original field.
   * @return String of the name for the instrumented field.
   */
  public static String getFieldInstName(String owner, String origFieldName, String desc) {
    if (hierarchy.hasDuplicatePrimField(owner)) {
      Type fieldType = Type.getType(desc);
      switch (fieldType.getSort()) {
        case Type.OBJECT:
        case Type.ARRAY:
          break;
        default:
          return origFieldName + "_" + desc + FIELD_SUFFIX;
      }
    }
    return origFieldName + FIELD_SUFFIX;
  }

  /**
   * @param access int of the access modifier.
   * @param flags int of the flags to check for in modifier.
   * @return true if and only if the bits on in flags is also on in access.
   */
  public final static boolean hasModifiers(int access, int flags) {
    return (access & flags) == flags;
  }

  /**
   * @param type Type of the object in question.
   * @return true if and only if type is a primitive array.
   */
  public static boolean isPrimitiveType(Type type) {
    switch (type.getSort()) {
      case Type.OBJECT:
      case Type.ARRAY:
      case Type.VOID:
      case Type.METHOD:
        return false;
      default:
        return true;
    }
  }

  /**
   * @param type Type of the object in question.
   * @return true if and only if type is a primitive.
   */
  public static boolean isPrimitiveArrayType(Type type) {
    switch (type.getSort()) {
      case Type.ARRAY:
        return isPrimitiveType(type.getElementType());
      default:
        return false;
    }
  }

  /**
   * @param type Type of the object in question.
   * @return true if and only if the type is exactly one of Object, Object[], Object[][],
   *         Object[][][], etc...
   */
  public static boolean canBePrimitiveArrayType(Type type) {
    if (isPrimitiveArrayType(type))
      return true;
    switch (type.getSort()) {
      case Type.OBJECT:
        return type.getInternalName().equals("java/lang/Object");
      case Type.ARRAY:
        return type.getElementType().getInternalName().equals("java/lang/Object");
    }
    return false;
  }

  /**
   * Converts the supplied Type object to a new Type object that represents the primitive array
   * taint type.
   * 
   * @param type Type of the original object.
   * @return Type of the primitive array taint type.
   */
  public static Type toPrimitiveArrayType(Type type) {
    switch (type.getSort()) {
      case Type.ARRAY:
        Type eleType = type.getElementType();

        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < type.getDimensions() - 1; i++) {
          buf.append('[');
        }

        switch (eleType.getSort()) {
          case Type.BOOLEAN:
            buf.append("Lpac/wrap/BooleanArrayTaint;");
            break;
          case Type.BYTE:
            buf.append("Lpac/wrap/ByteArrayTaint;");
            break;
          case Type.SHORT:
            buf.append("Lpac/wrap/ShortArrayTaint;");
            break;
          case Type.CHAR:
            buf.append("Lpac/wrap/CharArrayTaint;");
            break;
          case Type.INT:
            buf.append("Lpac/wrap/IntArrayTaint;");
            break;
          case Type.FLOAT:
            buf.append("Lpac/wrap/FloatArrayTaint;");
            break;
          case Type.LONG:
            buf.append("Lpac/wrap/LongArrayTaint;");
            break;
          case Type.DOUBLE:
            buf.append("Lpac/wrap/DoubleArrayTaint;");
            break;
          default:
            return null;
        }

        return Type.getType(buf.toString());
      default:
        return null;
    }
  }

  /**
   * Converts the supplied Type object to a new Type object that represents the primitive taint type
   * (or primitive array taint type in the event that the supplied type is a primitive array).
   * 
   * @param type Type of the original object.
   * @return Type.INT_TYPE (in the case of a primitive type), the primitive array taint wrapper type
   *         (for primitive arrays), otherwise null.
   */
  public static Type toPrimitiveType(Type type) {
    if (isPrimitiveType(type))
      return Type.INT_TYPE;
    else if (isPrimitiveArrayType(type))
      return toPrimitiveArrayType(type);
    return null;
  }

  public static Type toWrappedType(String desc) {
    return toWrappedType(Type.getType(desc));
  }

  public static Type toWrappedType(Type type) {
    switch (type.getSort()) {
      case Type.BOOLEAN:
        return Type.getType(Boolean.class);
      case Type.BYTE:
        return Type.getType(Byte.class);
      case Type.SHORT:
        return Type.getType(Short.class);
      case Type.CHAR:
        return Type.getType(Character.class);
      case Type.INT:
        return Type.getType(Integer.class);
      case Type.FLOAT:
        return Type.getType(Float.class);
      case Type.LONG:
        return Type.getType(Long.class);
      case Type.DOUBLE:
        return Type.getType(Double.class);
      default:
        return null;
    }
  }

  public static Type toWrappedArrayType(String desc) {
    return toWrappedArrayType(Type.getType(desc));
  }

  public static Type toWrappedArrayType(Type type) {
    switch (type.getSort()) {
      case Type.BOOLEAN:
        return Type.getType("Lpac/wrap/BooleanArrayTaint;");
      case Type.BYTE:
        return Type.getType("Lpac/wrap/ByteArrayTaint;");
      case Type.SHORT:
        return Type.getType("Lpac/wrap/ShortArrayTaint;");
      case Type.CHAR:
        return Type.getType("Lpac/wrap/CharArrayTaint;");
      case Type.INT:
        return Type.getType("Lpac/wrap/IntArrayTaint;");
      case Type.FLOAT:
        return Type.getType("Lpac/wrap/FloatArrayTaint;");
      case Type.LONG:
        return Type.getType("Lpac/wrap/LongArrayTaint;");
      case Type.DOUBLE:
        return Type.getType("Lpac/wrap/DoubleArrayTaint;");
      default:
        return null;
    }
  }

  /**
   * Returns the disassembled text of the specified instruction.
   * 
   * @param inst
   * @return string describing the instruction
   */
  public static String toString(AbstractInsnNode inst) {
    Textifier tf = new Textifier();
    TraceMethodVisitor tmv = new TraceMethodVisitor(tf);
    inst.accept(tmv);
    List<Object> l = tf.getText();
    String out = "";
    for (int ii = 0; ii < l.size(); ii++) {
      Object o = l.get(ii);
      out += o.toString().trim();
    }
    return out;
  }

  /**
   * Returns the disassembled text of the specified method.
   * 
   * @param method
   * @return String describing the method.
   */
  public static String toString(MethodNode method) {
    Textifier tf = new Textifier();
    TraceMethodVisitor tmv = new TraceMethodVisitor(tf);
    method.accept(tmv);
    List<Object> l = tf.getText();
    String out = "";
    for (int ii = 0; ii < l.size(); ii++) {
      Object o = l.get(ii);
      out += o.toString().trim() + "\n";
    }
    return out;
  }

  /**
   * Returns the disassembled text of the specified class.
   * 
   * @param method
   * @return String describing the method.
   */
  public static String toString(ClassNode cls) {
    StringWriter sw = new StringWriter();
    TraceClassVisitor tmv = new TraceClassVisitor(new PrintWriter(sw));
    cls.accept(tmv);
    // List<Object> l = tf.getText();
    // String out = "";
    // for (int ii = 0; ii < l.size(); ii++) {
    //   Object o = l.get(ii);
    //   out += o.toString().trim() + "\n";
    // }
    // return out;
    return sw.toString();
  }

  /** Returns the size of the instruction (in bytes). **/
  public static int getInstructionSize(AbstractInsnNode inst) {

    if (inst.getOpcode() == -1)
      return 0;

    CodeSizeEvaluator cse = new CodeSizeEvaluator(null);
    inst.accept(cse);
    assert cse.getMaxSize() == cse.getMinSize();
    return cse.getMaxSize();
  }

  public final static AbstractInsnNode numToInstruction(int n) {
    switch (n) {
      case -1:
        return new InsnNode(Opcodes.ICONST_M1);
      case 0:
        return new InsnNode(Opcodes.ICONST_0);
      case 1:
        return new InsnNode(Opcodes.ICONST_1);
      case 2:
        return new InsnNode(Opcodes.ICONST_2);
      case 3:
        return new InsnNode(Opcodes.ICONST_3);
      case 4:
        return new InsnNode(Opcodes.ICONST_4);
      case 5:
        return new InsnNode(Opcodes.ICONST_5);
      default:
        if (n <= Byte.MAX_VALUE && n >= Byte.MIN_VALUE)
          return new IntInsnNode(Opcodes.BIPUSH, n);
        if (n <= Short.MAX_VALUE && n >= Short.MIN_VALUE)
          return new IntInsnNode(Opcodes.SIPUSH, n);
    }
    return new LdcInsnNode(n);
  }

  public final static AbstractInsnNode numToInstruction(Number num) {
    if (num instanceof Double) {
      double d = num.doubleValue();
      if (d == 0)
        return new InsnNode(Opcodes.DCONST_0);
      if (d == 1)
        return new InsnNode(Opcodes.DCONST_1);
      return new LdcInsnNode(num);
    } else if (num instanceof Float) {
      float f = num.floatValue();
      if (f == 0)
        return new InsnNode(Opcodes.FCONST_0);
      if (f == 1)
        return new InsnNode(Opcodes.FCONST_1);
      if (f == 2)
        return new InsnNode(Opcodes.FCONST_2);
      return new LdcInsnNode(num);
    } else if (num instanceof Long) {
      long l = num.longValue();
      if (l == 0)
        return new InsnNode(Opcodes.LCONST_0);
      if (l == 1)
        return new InsnNode(Opcodes.LCONST_1);
      return new LdcInsnNode(num);
    }
    return numToInstruction(num.intValue());
  }
}
