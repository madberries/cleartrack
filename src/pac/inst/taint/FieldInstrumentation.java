package pac.inst.taint;

import java.lang.reflect.Field;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.AsmUtils;
import pac.util.Ret;
import pac.wrap.ArrayTaint;
import pac.wrap.BooleanArrayTaint;
import pac.wrap.ByteArrayTaint;
import pac.wrap.CharArrayTaint;
import pac.wrap.DoubleArrayTaint;
import pac.wrap.FloatArrayTaint;
import pac.wrap.IntArrayTaint;
import pac.wrap.LongArrayTaint;
import pac.wrap.ShortArrayTaint;

@InstrumentationClass("java/lang/reflect/Field")
public final class FieldInstrumentation extends MemberInstrumentation {

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT)
    public static final Class<?> getType(Field field, Ret ret) {
        Class<?> c = field.getType();
        if (c == CleartrackBoolean.class)
            return Boolean.class;
        if (c == CleartrackByte.class)
            return Byte.class;
        if (c == CleartrackCharacter.class)
            return Character.class;
        if (c == CleartrackShort.class)
            return Short.class;
        if (c == CleartrackInteger.class)
            return Integer.class;
        if (c == CleartrackLong.class)
            return Long.class;
        if (c == CleartrackFloat.class)
            return Float.class;
        if (c == CleartrackDouble.class)
            return Double.class;
        return c;
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void set(Field field, Object obj, Object value, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        Class<?> type = field.getType();
        if (ClassInstrumentation.primTypeMap.containsKey(type)) {
            if (type == boolean.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackBoolean) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setBoolean(obj, ((CleartrackBoolean) value).value);
            } else if (type == byte.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackByte) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setByte(obj, ((CleartrackByte) value).value);
            } else if (type == short.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackShort) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setShort(obj, ((CleartrackShort) value).value);
            } else if (type == char.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackCharacter) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setChar(obj, ((CleartrackCharacter) value).value);
            } else if (type == int.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackInteger) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setInt(obj, ((CleartrackInteger) value).value);
            } else if (type == long.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackLong) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setLong(obj, ((CleartrackLong) value).value);
            } else if (type == float.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackFloat) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setFloat(obj, ((CleartrackFloat) value).value);
            } else if (type == double.class) {
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX)
                            .setInt(obj, ((CleartrackDouble) value).value_t);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.setDouble(obj, ((CleartrackDouble) value).value);
            } else {
                // it must be some sort of primitive array...
                Object origValue = null;
                if (type == boolean[].class)
                    origValue = BooleanArrayTaint.toValueArray((BooleanArrayTaint) value);
                else if (type == short[].class)
                    origValue = ShortArrayTaint.toValueArray((ShortArrayTaint) value);
                else if (type == byte[].class)
                    origValue = ByteArrayTaint.toValueArray((ByteArrayTaint) value);
                else if (type == char[].class)
                    origValue = CharArrayTaint.toValueArray((CharArrayTaint) value);
                else if (type == int[].class)
                    origValue = IntArrayTaint.toValueArray((IntArrayTaint) value);
                else if (type == long[].class)
                    origValue = LongArrayTaint.toValueArray((LongArrayTaint) value);
                else if (type == float[].class)
                    origValue = FloatArrayTaint.toValueArray((FloatArrayTaint) value);
                else if (type == double[].class)
                    origValue = DoubleArrayTaint.toValueArray((DoubleArrayTaint) value);
                else if (type == boolean[][].class)
                    origValue = BooleanArrayTaint.toValueArray((BooleanArrayTaint[]) value);
                else if (type == short[][].class)
                    origValue = ShortArrayTaint.toValueArray((ShortArrayTaint[]) value);
                else if (type == byte[][].class)
                    origValue = ByteArrayTaint.toValueArray((ByteArrayTaint[]) value);
                else if (type == char[][].class)
                    origValue = CharArrayTaint.toValueArray((CharArrayTaint[]) value);
                else if (type == int[][].class)
                    origValue = IntArrayTaint.toValueArray((IntArrayTaint[]) value);
                else if (type == long[][].class)
                    origValue = LongArrayTaint.toValueArray((LongArrayTaint[]) value);
                else if (type == float[][].class)
                    origValue = FloatArrayTaint.toValueArray((FloatArrayTaint[]) value);
                else if (type == double[][].class)
                    origValue = DoubleArrayTaint.toValueArray((DoubleArrayTaint[]) value);
                else if (type == boolean[][][].class)
                    origValue = BooleanArrayTaint.toValueArray((BooleanArrayTaint[][]) value);
                else if (type == short[][][].class)
                    origValue = ShortArrayTaint.toValueArray((ShortArrayTaint[][]) value);
                else if (type == byte[][][].class)
                    origValue = ByteArrayTaint.toValueArray((ByteArrayTaint[][]) value);
                else if (type == char[][][].class)
                    origValue = CharArrayTaint.toValueArray((CharArrayTaint[][]) value);
                else if (type == int[][][].class)
                    origValue = IntArrayTaint.toValueArray((IntArrayTaint[][]) value);
                else if (type == long[][][].class)
                    origValue = LongArrayTaint.toValueArray((LongArrayTaint[][]) value);
                else if (type == float[][][].class)
                    origValue = FloatArrayTaint.toValueArray((FloatArrayTaint[][]) value);
                else if (type == double[][][].class)
                    origValue = DoubleArrayTaint.toValueArray((DoubleArrayTaint[][]) value);
                else {
                    // I give up...
                    try {
                        field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).set(obj,
                                                                                                                value);
                    } catch (NoSuchFieldException e) {
                    } catch (SecurityException e) {
                    }
                    return;
                }
                try {
                    field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).set(obj, value);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                field.set(obj, origValue);
            }
        } else {
            field.set(obj, value);
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setBoolean(Field field, Object obj, boolean value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setBoolean(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setByte(Field field, Object obj, byte value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setByte(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setShort(Field field, Object obj, short value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setShort(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setChar(Field field, Object obj, char value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setChar(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setInt(Field field, Object obj, int value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setInt(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setLong(Field field, Object obj, long value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setLong(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setFloat(Field field, Object obj, float value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setFloat(obj, value);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final void setDouble(Field field, Object obj, double value, int value_t, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            field.getDeclaringClass().getDeclaredField(field.getName() + AsmUtils.FIELD_SUFFIX).setInt(obj, value_t);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        field.setDouble(obj, value);
    }

    // TODO we should check all gets against original field as we do at the bytecode level...

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final Object get(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        Class<?> type = field.getType();
        if (ClassInstrumentation.primTypeMap.containsKey(type)) {
            if (type == boolean.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return CleartrackBoolean.valueOf(field.getBoolean(obj), ret.taint, ret);
            } else if (type == byte.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return ByteInstrumentation.valueOf(field.getByte(obj), ret.taint, ret);
            } else if (type == short.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return ShortInstrumentation.valueOf(field.getShort(obj), ret.taint, ret);
            } else if (type == char.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return CharacterInstrumentation.valueOf(field.getChar(obj), ret.taint, ret);
            } else if (type == int.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return IntegerInstrumentation.valueOf(field.getInt(obj), ret.taint, ret);
            } else if (type == long.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return LongInstrumentation.valueOf(field.getLong(obj), ret.taint, ret);
            } else if (type == float.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return FloatInstrumentation.valueOf(field.getFloat(obj), ret.taint, ret);
            } else if (type == double.class) {
                try {
                    ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }
                return DoubleInstrumentation.valueOf(field.getDouble(obj), ret.taint, ret);
            } else {
                // it must be some sort of primitive array...
                try {
                    return field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).get(obj);
                } catch (NoSuchFieldException e) {
                } catch (SecurityException e) {
                }

                return ArrayTaint.toTaintArray(field.get(obj));
            }
        } else {
            return field.get(obj);
        }
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final boolean getBoolean(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getBoolean(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final byte getByte(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getByte(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final short getShort(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getShort(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final char getChar(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getChar(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final int getInt(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getInt(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final long getLong(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getLong(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final float getFloat(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getFloat(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.COMPAT, inline = true)
    public static final double getDouble(Field field, Object obj, Ret ret)
            throws IllegalArgumentException, IllegalAccessException {
        try {
            ret.taint = field.getDeclaringClass().getField(field.getName() + AsmUtils.FIELD_SUFFIX).getInt(obj);
        } catch (NoSuchFieldException e) {
        } catch (SecurityException e) {
        }
        return field.getDouble(obj);
    }

}
