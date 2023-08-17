package pac.inst.taint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass(value = "java/lang/reflect/InvocationHandler", isInterface = true)
public final class InvocationHandlerInstrumentation {

  @InstrumentationMethod(inline = true, canExtend = true,
      instrumentationLocation = InstrumentationLocation.TRANS)
  public static final Object invoke(InvocationHandler invHandler, Object proxy, Method method,
      Object[] args, Ret ret) throws Throwable {
    String methodName = method.getName();
    Class<?>[] paramTypes = method.getParameterTypes();
    if (paramTypes.length > 0 && paramTypes[paramTypes.length - 1] == Ret.class) {
      // Attempt to get the original method since reflection is expecting all methods to be
      // uninstrumented.
      try {
        Method uninstMethod = method.getDeclaringClass().getDeclaredMethod(methodName,
            MethodInstrumentation.getUninstrumentedTypes(paramTypes));
        if (uninstMethod != null) {
          if (method.isAccessible())
            uninstMethod.setAccessible(true);
          method = uninstMethod;
        }
      } catch (NoSuchMethodException e) {
      } catch (SecurityException e) {
      }
    }

    Object result = invHandler.invoke(proxy, method, args, ret);
    if (result == null)
      return null;
    Class<?> resultType = result.getClass();
    Class<?> expectingType = method.getReturnType();
    if (resultType == expectingType)
      return result;

    if (resultType == CleartrackInteger.class && expectingType == int.class) {
      return ((CleartrackInteger) result).intValue(ret);
    } else if (resultType == CleartrackBoolean.class && expectingType == boolean.class) {
      return ((CleartrackBoolean) result).booleanValue(ret);
    } else if (resultType == CleartrackCharacter.class && expectingType == char.class) {
      return ((CleartrackCharacter) result).charValue(ret);
    } else if (resultType == CleartrackByte.class && expectingType == byte.class) {
      return ((CleartrackByte) result).byteValue(ret);
    } else if (resultType == CleartrackShort.class && expectingType == short.class) {
      return ((CleartrackShort) result).shortValue(ret);
    } else if (resultType == CleartrackLong.class && expectingType == long.class) {
      return ((CleartrackLong) result).longValue(ret);
    } else if (resultType == CleartrackFloat.class && expectingType == float.class) {
      return ((CleartrackFloat) result).floatValue(ret);
    } else if (resultType == CleartrackDouble.class && expectingType == double.class) {
      return ((CleartrackDouble) result).doubleValue(ret);
    }

    // TODO: Ensure that we don't get here.
    return result;
  }

}
