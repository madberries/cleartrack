package pac.inst.taint;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.wrap.BooleanArrayTaint;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.DoubleArrayTaint;
import pac.wrap.FloatArrayTaint;
import pac.wrap.IntArrayTaint;
import pac.wrap.LongArrayTaint;
import pac.wrap.ShortArrayTaint;

@InstrumentationClass("java/lang/reflect/Method")
public final class MethodInstrumentation extends MemberInstrumentation {

    public static final Object[] getUninstrumentedArgs(Object[] args) {
        if (args == null || args.length == 0)
            return args;
        boolean hasTaintArgs = args[args.length - 1] != null && args[args.length - 1].getClass() == Ret.class;
        int len = hasTaintArgs ? args.length - 1 : args.length;
        // WHY MUST WE UNBOX THE INSTRUMENTED PRIMITIVE AND REBOX IT???
        // Since primitive wrapper classes are final, the native call 
        // must make this assumption about these classes.  Instead of
        // checking for instanceof it must check for class equality,
        // for example...
        LinkedList<Object> newArgs = new LinkedList<Object>();
        boolean removeNext = false;
        for (int i = 0; i < len; i++) {
            if (removeNext) {
                removeNext = false;
                continue;
            }
            if (args[i] == null) {
                newArgs.add(null);
                continue;
            }
            Class<?> c = args[i].getClass();
            if (c == CleartrackInteger.class) {
                newArgs.add(Integer.valueOf(((CleartrackInteger) args[i]).intValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackBoolean.class) {
                newArgs.add(Boolean.valueOf(((CleartrackBoolean) args[i]).booleanValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackCharacter.class) {
                newArgs.add(Character.valueOf(((CleartrackCharacter) args[i]).charValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackByte.class) {
                newArgs.add(Byte.valueOf(((CleartrackByte) args[i]).byteValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackShort.class) {
                newArgs.add(Short.valueOf(((CleartrackShort) args[i]).shortValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackLong.class) {
                newArgs.add(Long.valueOf(((CleartrackLong) args[i]).longValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackFloat.class) {
                newArgs.add(Float.valueOf(((CleartrackFloat) args[i]).floatValue()));
                removeNext = hasTaintArgs;
            } else if (c == CleartrackDouble.class) {
                newArgs.add(Double.valueOf(((CleartrackDouble) args[i]).doubleValue()));
                removeNext = hasTaintArgs;
            } else if (c == IntArrayTaint.class) {
                newArgs.add(IntArrayTaint.toValueArray((IntArrayTaint) args[i]));
            } else if (c == BooleanArrayTaint.class) {
                newArgs.add(BooleanArrayTaint.toValueArray((BooleanArrayTaint) args[i]));
            } else if (c == CharArrayTaint.class) {
                newArgs.add(CharArrayTaint.toValueArray((CharArrayTaint) args[i]));
            } else if (c == ShortArrayTaint.class) {
                newArgs.add(ShortArrayTaint.toValueArray((ShortArrayTaint) args[i]));
            } else if (c == LongArrayTaint.class) {
                newArgs.add(LongArrayTaint.toValueArray((LongArrayTaint) args[i]));
            } else if (c == FloatArrayTaint.class) {
                newArgs.add(FloatArrayTaint.toValueArray((FloatArrayTaint) args[i]));
            } else if (c == DoubleArrayTaint.class) {
                newArgs.add(DoubleArrayTaint.toValueArray((DoubleArrayTaint) args[i]));
            } else if (c == ByteArrayTaint.class) {
                newArgs.add(ByteArrayTaint.toValueArray((ByteArrayTaint) args[i]));
            } else if (c == IntArrayTaint[].class) {
                newArgs.add(IntArrayTaint.toValueArray((IntArrayTaint[]) args[i]));
            } else if (c == BooleanArrayTaint[].class) {
                newArgs.add(BooleanArrayTaint.toValueArray((BooleanArrayTaint[]) args[i]));
            } else if (c == CharArrayTaint[].class) {
                newArgs.add(CharArrayTaint.toValueArray((CharArrayTaint[]) args[i]));
            } else if (c == ShortArrayTaint[].class) {
                newArgs.add(ShortArrayTaint.toValueArray((ShortArrayTaint[]) args[i]));
            } else if (c == LongArrayTaint[].class) {
                newArgs.add(LongArrayTaint.toValueArray((LongArrayTaint[]) args[i]));
            } else if (c == FloatArrayTaint[].class) {
                newArgs.add(FloatArrayTaint.toValueArray((FloatArrayTaint[]) args[i]));
            } else if (c == DoubleArrayTaint[].class) {
                newArgs.add(DoubleArrayTaint.toValueArray((DoubleArrayTaint[]) args[i]));
            } else if (c == ByteArrayTaint[].class) {
                newArgs.add(ByteArrayTaint.toValueArray((ByteArrayTaint[]) args[i]));
            } else if (c == IntArrayTaint[][].class) {
                newArgs.add(IntArrayTaint.toValueArray((IntArrayTaint[][]) args[i]));
            } else if (c == BooleanArrayTaint[][].class) {
                newArgs.add(BooleanArrayTaint.toValueArray((BooleanArrayTaint[][]) args[i]));
            } else if (c == CharArrayTaint[][].class) {
                newArgs.add(CharArrayTaint.toValueArray((CharArrayTaint[][]) args[i]));
            } else if (c == ShortArrayTaint[][].class) {
                newArgs.add(ShortArrayTaint.toValueArray((ShortArrayTaint[][]) args[i]));
            } else if (c == LongArrayTaint[][].class) {
                newArgs.add(LongArrayTaint.toValueArray((LongArrayTaint[][]) args[i]));
            } else if (c == FloatArrayTaint[][].class) {
                newArgs.add(FloatArrayTaint.toValueArray((FloatArrayTaint[][]) args[i]));
            } else if (c == DoubleArrayTaint[][].class) {
                newArgs.add(DoubleArrayTaint.toValueArray((DoubleArrayTaint[][]) args[i]));
            } else if (c == ByteArrayTaint[][].class) {
                newArgs.add(ByteArrayTaint.toValueArray((ByteArrayTaint[][]) args[i]));
            } else {
                newArgs.add(args[i]);
                if (c == Boolean.class || c == Byte.class || c == Short.class || c == Character.class
                        || c == Integer.class || c == Float.class || c == Long.class || c == Double.class) {
                    removeNext = hasTaintArgs;
                }
            }
        }

        return newArgs.toArray();
    }

    public static final Object[] getInstrumentedArgs(Class<?>[] types, Object[] args) {
        Ret ret = new Ret();
        if (args == null || args.length == 0)
            return new Object[] { ret };
        boolean hasRet = args[args.length - 1] != null && args[args.length - 1].getClass() == Ret.class;
        int len = hasRet ? args.length - 1 : args.length;
        List<Object> newArgs = new LinkedList<Object>();
        int j = 0;
        for (int i = 0; i < len; i++) {
            Object obj = args[i];
            if (obj == null) {
                newArgs.add(null);
                j++;
                continue;
            }
            Class<?> c = types[j++];
            if (!c.isPrimitive()) {
                newArgs.add(obj);
            } else {
                boolean isPrimitive = false;
                if (c == boolean.class) {
                    newArgs.add(((Boolean) obj).booleanValue(ret));
                    isPrimitive = true;
                } else if (c == byte.class) {
                    newArgs.add(((Byte) obj).byteValue(ret));
                    isPrimitive = true;
                } else if (c == short.class) {
                    newArgs.add(((Short) obj).shortValue(ret));
                    isPrimitive = true;
                } else if (c == char.class) {
                    newArgs.add(((Character) obj).charValue(ret));
                    isPrimitive = true;
                } else if (c == int.class) {
                    newArgs.add(((Integer) obj).intValue(ret));
                    isPrimitive = true;
                } else if (c == float.class) {
                    newArgs.add(((Float) obj).floatValue(ret));
                    isPrimitive = true;
                } else if (c == long.class) {
                    newArgs.add(((Long) obj).longValue(ret));
                    isPrimitive = true;
                } else if (c == double.class) {
                    newArgs.add(((Double) obj).doubleValue(ret));
                    isPrimitive = true;
                } else {
                    newArgs.add(obj);
                }

                if (isPrimitive) {
                    newArgs.add(ret.taint);
                    j++;
                }
            }
        }
        if (!hasRet)
            newArgs.add(ret);
        return newArgs.toArray();
    }

    protected static final boolean isPrimitiveClass(Class<?> klass) {
        return klass == boolean.class || klass == byte.class || klass == short.class || klass == char.class
                || klass == int.class || klass == float.class || klass == long.class || klass == double.class;
    }

    public static final Class<?>[] getUninstrumentedTypes(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0)
            return parameterTypes;
        int length = parameterTypes.length;
        List<Class<?>> newParamTypes = new LinkedList<Class<?>>();
        for (int i = 0; i < length; i++) { //8776382817
            Class<?> oldType = parameterTypes[i];
            if (oldType == Ret.class)
                continue;
            Class<?> newType = ClassInstrumentation.instTypeMap.get(oldType);
            if (newType == null) { // either a primitive, or some object that 
                                   // is not a taint wrapped primitive array
                if (isPrimitiveClass(oldType)) {
                    newParamTypes.add(oldType);
                    i++; // skip the taint param
                } else {
                    newParamTypes.add(oldType);
                }
            } else {
                newParamTypes.add(newType);
            }
        }
        return newParamTypes.toArray(new Class<?>[0]);
    }

    public static final Class<?>[] getInstrumentedTypes(Class<?>[] parameterTypes) {
        if (parameterTypes == null || parameterTypes.length == 0)
            return new Class<?>[] { Ret.class };
        boolean hasRet = parameterTypes[parameterTypes.length - 1] == Ret.class;
        List<Class<?>> newParamTypes = new LinkedList<Class<?>>();
        int length = parameterTypes.length;
        for (int i = 0; i < length; i++) { //8776382817
            Class<?> oldType = parameterTypes[i];
            Class<?> newType = ClassInstrumentation.primTypeMap.get(oldType);
            if (newType == null) {
                newParamTypes.add(oldType);
            } else {
                if (newType == int.class) {
                    newParamTypes.add(oldType);
                }
                newParamTypes.add(newType);
            }
        }
        if (!hasRet)
            newParamTypes.add(Ret.class);
        return newParamTypes.toArray(new Class<?>[0]);
    }

    @InstrumentationMethod(inline = true, skippedDescriptor = "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;")
    public static final Object invoke(Method method, Object obj, Object[] args, Ret ret)
            throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // Note that the method object must be one of the original methods not added by the
        // instrumentation since they are filter out by our Class wrappers.
        try {
            Class<?>[] instTypes = getInstrumentedTypes(method.getParameterTypes());
            Method instMethod = method.getDeclaringClass().getDeclaredMethod(method.getName(), instTypes);
            if (instMethod != null) {
                // The application could explicitly call setAccesible(true) on the
                // original constructor even though it may not be accessible according
                // to the method modifier.  Therefore we need to propagate this value.
                if (method.isAccessible())
                    instMethod.setAccessible(true);
                Object[] instArgs = MethodInstrumentation.getInstrumentedArgs(instTypes, args);
                Object result = instMethod.invoke(obj, instArgs);
                if (result == null)
                    return null;
                Class<?> returnType = instMethod.getReturnType();
                if (returnType == boolean.class)
                    return CleartrackBoolean.valueOf((boolean) result, ret.taint, ret);
                else if (returnType == byte.class)
                    return CleartrackByte.valueOf((byte) result, ret.taint, ret);
                else if (returnType == short.class)
                    return CleartrackShort.valueOf((short) result, ret.taint, ret);
                else if (returnType == char.class)
                    return CleartrackCharacter.valueOf((char) result, ret.taint, ret);
                else if (returnType == int.class)
                    return CleartrackInteger.valueOf((int) result, ret.taint, ret);
                else if (returnType == float.class)
                    return CleartrackFloat.valueOf((float) result, ret.taint, ret);
                else if (returnType == long.class)
                    return CleartrackLong.valueOf((long) result, ret.taint, ret);
                else if (returnType == double.class)
                    return CleartrackDouble.valueOf((double) result, ret.taint, ret);
                return result;
            }
        } catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
            // Do not intervene, but execute plan "b" (i.e. invoke using the original method object).
        }
        return method.invoke(obj, MethodInstrumentation.getUninstrumentedArgs(args));
    }

}
