package pac.inst.taint;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.AsmUtils;
import pac.util.Ret;
import pac.util.TaintUtils;
import pac.util.TaintValues;
import pac.wrap.BooleanArrayTaint;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.DoubleArrayTaint;
import pac.wrap.FloatArrayTaint;
import pac.wrap.IntArrayTaint;
import pac.wrap.LongArrayTaint;
import pac.wrap.ShortArrayTaint;

@InstrumentationClass("java/lang/Class")
public final class ClassInstrumentation extends MemberInstrumentation {
    public static final HashMap<Class<?>, Class<?>> primTypeMap = new HashMap<Class<?>, Class<?>>();
    public static final HashMap<Class<?>, Class<?>> dangerousTypeMap = new HashMap<Class<?>, Class<?>>();
    protected static final HashMap<Class<?>, Class<?>> instTypeMap = new HashMap<Class<?>, Class<?>>();

    static {
        primTypeMap.put(boolean.class, int.class);
        primTypeMap.put(byte.class, int.class);
        primTypeMap.put(short.class, int.class);
        primTypeMap.put(char.class, int.class);
        primTypeMap.put(int.class, int.class);
        primTypeMap.put(long.class, int.class);
        primTypeMap.put(float.class, int.class);
        primTypeMap.put(double.class, int.class);
        primTypeMap.put(boolean[].class, BooleanArrayTaint.class);
        primTypeMap.put(byte[].class, ByteArrayTaint.class);
        primTypeMap.put(short[].class, ShortArrayTaint.class);
        primTypeMap.put(char[].class, CharArrayTaint.class);
        primTypeMap.put(int[].class, IntArrayTaint.class);
        primTypeMap.put(long[].class, LongArrayTaint.class);
        primTypeMap.put(float[].class, FloatArrayTaint.class);
        primTypeMap.put(double[].class, DoubleArrayTaint.class);
        primTypeMap.put(boolean[][].class, BooleanArrayTaint[].class);
        primTypeMap.put(byte[][].class, ByteArrayTaint[].class);
        primTypeMap.put(short[][].class, ShortArrayTaint[].class);
        primTypeMap.put(char[][].class, CharArrayTaint[].class);
        primTypeMap.put(int[][].class, IntArrayTaint[].class);
        primTypeMap.put(long[][].class, LongArrayTaint[].class);
        primTypeMap.put(float[][].class, FloatArrayTaint[].class);
        primTypeMap.put(double[][].class, DoubleArrayTaint[].class);
        primTypeMap.put(boolean[][][].class, BooleanArrayTaint[][].class);
        primTypeMap.put(byte[][][].class, ByteArrayTaint[][].class);
        primTypeMap.put(short[][][].class, ShortArrayTaint[][].class);
        primTypeMap.put(char[][][].class, CharArrayTaint[][].class);
        primTypeMap.put(int[][][].class, IntArrayTaint[][].class);
        primTypeMap.put(long[][][].class, LongArrayTaint[][].class);
        primTypeMap.put(float[][][].class, FloatArrayTaint[][].class);
        primTypeMap.put(double[][][].class, DoubleArrayTaint[][].class);

        instTypeMap.put(BooleanArrayTaint.class, boolean[].class);
        instTypeMap.put(ByteArrayTaint.class, byte[].class);
        instTypeMap.put(ShortArrayTaint.class, short[].class);
        instTypeMap.put(CharArrayTaint.class, char[].class);
        instTypeMap.put(IntArrayTaint.class, int[].class);
        instTypeMap.put(LongArrayTaint.class, long[].class);
        instTypeMap.put(FloatArrayTaint.class, float[].class);
        instTypeMap.put(DoubleArrayTaint.class, double[].class);
        instTypeMap.put(BooleanArrayTaint[].class, boolean[][].class);
        instTypeMap.put(ByteArrayTaint[].class, byte[][].class);
        instTypeMap.put(ShortArrayTaint[].class, short[][].class);
        instTypeMap.put(CharArrayTaint[].class, char[][].class);
        instTypeMap.put(IntArrayTaint[].class, int[][].class);
        instTypeMap.put(LongArrayTaint[].class, long[][].class);
        instTypeMap.put(FloatArrayTaint[].class, float[][].class);
        instTypeMap.put(DoubleArrayTaint[].class, double[][].class);
        instTypeMap.put(BooleanArrayTaint[][].class, boolean[][][].class);
        instTypeMap.put(ByteArrayTaint[][].class, byte[][][].class);
        instTypeMap.put(ShortArrayTaint[][].class, short[][][].class);
        instTypeMap.put(CharArrayTaint[][].class, char[][][].class);
        instTypeMap.put(IntArrayTaint[][].class, int[][][].class);
        instTypeMap.put(LongArrayTaint[][].class, long[][][].class);
        instTypeMap.put(FloatArrayTaint[][].class, float[][][].class);
        instTypeMap.put(DoubleArrayTaint[][].class, double[][][].class);

        dangerousTypeMap.put(Boolean.class, CleartrackBoolean.class);
        dangerousTypeMap.put(Byte.class, CleartrackByte.class);
        dangerousTypeMap.put(Character.class, CleartrackCharacter.class);
        dangerousTypeMap.put(Short.class, CleartrackShort.class);
        dangerousTypeMap.put(Integer.class, CleartrackInteger.class);
        dangerousTypeMap.put(Long.class, CleartrackLong.class);
        dangerousTypeMap.put(Float.class, CleartrackFloat.class);
        dangerousTypeMap.put(Double.class, CleartrackDouble.class);
    }

    @InstrumentationMethod
    public static final String getName(Class<?> c) {
        // The class name is obtained from a native call,
        // but we need to trust it in case a forName() is
        // called on this String as in the case of H2.
        Class<?> origClass = instTypeMap.get(c);
        String name = origClass == null ? c.getName() : origClass.getName();
        TaintUtils.trust(name);
        return name;
    }

    @InstrumentationMethod
    public static final String getCanonicalName(Class<?> c) {
        Class<?> origClass = instTypeMap.get(c);
        String name = origClass == null ? c.getCanonicalName() : origClass.getCanonicalName();
        TaintUtils.trust(name);
        return name;
    }

    //	@InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT)
    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Field[] getDeclaredFields(Class<?> c) {
        Field[] declaredFields = c.getDeclaredFields();
        List<Field> originalFields = new ArrayList<Field>(declaredFields.length);
        for (int i = 0; i < declaredFields.length; i++) {
            Field field = declaredFields[i];
            if (field.getName().endsWith(AsmUtils.FIELD_SUFFIX))
                continue;
            originalFields.add(field);
        }
        return originalFields.toArray(new Field[0]);
    }

    //	@InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT)
    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Field[] getFields(Class<?> c) {
        Field[] fields = c.getFields();
        List<Field> originalFields = new ArrayList<Field>(fields.length);
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (field.getName().endsWith(AsmUtils.FIELD_SUFFIX))
                continue;
            originalFields.add(field);
        }
        return originalFields.toArray(new Field[0]);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Method[] getDeclaredMethods(Class<?> c) {
        Method[] declaredMethods = c.getDeclaredMethods();
        List<Method> originalMethods = new ArrayList<Method>(declaredMethods.length);
        for (int i = 0; i < declaredMethods.length; i++) {
            Method method = declaredMethods[i];
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == Ret.class)
                continue;
            // Also filter out instrumented methods that are recursive.
            if (paramTypes.length > 1 && paramTypes[paramTypes.length - 2] == Ret.class)
                continue;
            // Skip over wrapped methods we've inlined.
            if (method.getAnnotation(InstrumentationMethod.class) != null)
                continue;
            originalMethods.add(method);
        }
        return originalMethods.toArray(new Method[0]);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Method[] getMethods(Class<?> c) {
        Method[] methods = c.getMethods();
        List<Method> originalMethods = new ArrayList<Method>(methods.length);
        for (int i = 0; i < methods.length; i++) {
            Method method = methods[i];
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == Ret.class)
                continue;
            // Also filter out instrumented methods that are recursive.
            if (paramTypes.length > 1 && paramTypes[paramTypes.length - 2] == Ret.class)
                continue;
            // Skip over wrapped methods we've inlined.
            if (method.getAnnotation(InstrumentationMethod.class) != null)
                continue;
            originalMethods.add(method);
        }
        return originalMethods.toArray(new Method[0]);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Constructor<?>[] getDeclaredConstructors(Class<?> c) {
        Constructor<?>[] constructors = c.getDeclaredConstructors();
        List<Constructor<?>> originalConstructors = new ArrayList<Constructor<?>>(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == Ret.class)
                continue;
            // Also filter out instrumented methods that are recursive.
            if (paramTypes.length > 1 && paramTypes[paramTypes.length - 2] == Ret.class)
                continue;
            // Skip over wrapped methods we've inlined.
            if (constructor.getAnnotation(InstrumentationMethod.class) != null)
                continue;
            originalConstructors.add(constructor);
        }
        return originalConstructors.toArray(new Constructor<?>[0]);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Constructor<?>[] getConstructors(Class<?> c) {
        Constructor<?>[] constructors = c.getConstructors();
        List<Constructor<?>> originalConstructors = new ArrayList<Constructor<?>>(constructors.length);
        for (int i = 0; i < constructors.length; i++) {
            Constructor<?> constructor = constructors[i];
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == Ret.class)
                continue;
            // Also filter out instrumented methods that are recursive.
            if (paramTypes.length > 1 && paramTypes[paramTypes.length - 2] == Ret.class)
                continue;
            // Skip over wrapped methods we've inlined.
            if (constructor.getAnnotation(InstrumentationMethod.class) != null)
                continue;
            originalConstructors.add(constructor);
        }
        return originalConstructors.toArray(new Constructor<?>[0]);
    }

    // We need to inline Class.newInstance() since the call relies on
    // finding the protections of the original calling class.
    @InstrumentationMethod(inline = true)
    public static final Object newInstance(Class<?> c) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Class<?> subClass = dangerousTypeMap.get(c);
        try {
            if (subClass == null)
                subClass = c;
            Constructor<?> instConstructor = subClass.getDeclaredConstructor(Ret.class);
            if (instConstructor != null)
                return instConstructor.newInstance(new Ret());
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e) {
        }
        return c.newInstance();
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final boolean isArray(Class<?> c) {
        if (c == IntArrayTaint.class || c == ByteArrayTaint.class || c == CharArrayTaint.class
                || c == BooleanArrayTaint.class || c == DoubleArrayTaint.class || c == FloatArrayTaint.class
                || c == LongArrayTaint.class || c == ShortArrayTaint.class)
            return true;
        return c.isArray();
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.TRANS)
    public static final Class<?> getComponentType(Class<?> c) {
        Class<?> instType = instTypeMap.get(c);
        if (instType != null)
            return instType.getComponentType();
        return c.getComponentType();
    }

    @InstrumentationMethod
    public static final InputStream getResourceAsStream(Class<?> c, String name) {
        InputStream inStream = c.getResourceAsStream(name);
        if (inStream == null)
            return null;
        inStream.ss_hasUniformTaint = true;
        inStream.ss_taint = TaintValues.JAR | TaintValues.TRUSTED;
        if (inStream instanceof FilterInputStream) {
            InputStream in2 = ((FilterInputStream) inStream).in;
            in2.ss_hasUniformTaint = true;
            in2.ss_taint = inStream.ss_taint;
        }
        return inStream;
    }

    @InstrumentationMethod
    public static final URL getResource(Class<?> c, String name) {
        URL url = c.getResource(name);
        if (url == null)
            return null;
        boolean isTrusted = TaintUtils.isTrusted(name) || !TaintUtils.isTracked(name);
        if (isTrusted) {
            URLInstrumentation.trustURL(url, TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK));
        } else {
            URLInstrumentation.taintURL(url, TaintUtils.getTaintOr(name, TaintValues.INPUTTYPE_MASK));
        }
        return url;
    }
}
