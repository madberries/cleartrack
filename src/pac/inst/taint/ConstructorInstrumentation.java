package pac.inst.taint;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass("java/lang/reflect/Constructor")
public final class ConstructorInstrumentation extends MemberInstrumentation {

  @InstrumentationMethod(inline = true,
      skippedDescriptor = "([Ljava/lang/Object;)Ljava/lang/Object;")
  public static final Object newInstance(Constructor<?> constructor, Object[] initargs, Ret ret)
      throws IllegalArgumentException, InstantiationException, IllegalAccessException,
      InvocationTargetException {
    try {
      Class<?>[] instTypes =
          MethodInstrumentation.getInstrumentedTypes(constructor.getParameterTypes());
      Constructor<?> instConstructor =
          constructor.getDeclaringClass().getDeclaredConstructor(instTypes);
      if (instConstructor != null) {
        // The application could explicitly call setAccesible(true) on the original constructor even
        // though it may not be accessible according to the method modifier. Therefore we need to
        // propagate this value.
        if (constructor.isAccessible())
          instConstructor.setAccessible(true);
        Object[] instArgs = MethodInstrumentation.getInstrumentedArgs(instTypes, initargs);
        return instConstructor.newInstance(instArgs);
      }
    } catch (SecurityException | IllegalArgumentException | NoSuchMethodException e) {
      // Do not intervene, but execute plan "b" (i.e. invoke using the original constructor object).
    }
    return constructor.newInstance(MethodInstrumentation.getUninstrumentedArgs(initargs));
  }

}
