package pac.inst.taint;

import java.util.List;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;
import pac.util.Overflows;
import pac.util.TaintValues;

@InstrumentationClass(value = "java/util/List", isInterface = true)
public final class ListInstrumentation extends CollectionInstrumentation {

  /*
   * These methods refer to instrumented methods that do not exist in List, because list is not
   * instrumented. However, they will still be used, since we set the skippedDescriptor field in the
   * InstrumentationMethod annotation.
   */
  
  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true,
      skippedDescriptor = "(I)Ljava/lang/Object;")
  public static final <T> T get(List<T> list, int idx, int idx_t, Ret ret) {
    try {
      Overflows.checkOverflow(idx_t, "List.get()", "object", null, idx);
      if ((idx_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
          && (idx < 0 || idx >= list.size())) { // Index out of bounds.
        Overflows.outOfBounds("List.get()", "object", null, idx);
        return null;
      }
      return list.get(idx);
      // return (T) ArrayTaint.toTaintArray(list.get(idx));
    } catch (IndexOutOfBoundsException e) {
      throw e;
    }
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true,
      skippedDescriptor = "(ILjava/lang/Object;)Ljava/lang/Object;")
  public static final <T> T set(List<T> list, int idx, int idx_t, T obj, Ret ret) {
    Overflows.checkOverflow(idx_t, "List.set()", "object", obj, idx);
    if ((idx_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
        && (idx < 0 || idx >= list.size())) { // Index out of bounds.
      Overflows.outOfBounds("List.set()", "object", null, idx);
      return null;
    }
    return list.set(idx, obj);
    // return list.set(idx, (T) ArrayTaint.toValueArray(obj));
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true,
      skippedDescriptor = "(I)Ljava/lang/Object;")
  public static final <T> T remove(List<T> list, int idx, int idx_t, Ret ret) {
    try {
      Overflows.checkOverflow(idx_t, "List.get()", "object", null, idx);
      if ((idx_t & TaintValues.TRUST_MASK) != TaintValues.TRUSTED
          && (idx < 0 || idx >= list.size())) { // Index out of bounds.
        Overflows.outOfBounds("List.get()", "object", null, idx);
        return null;
      }
      return list.remove(idx);
    } catch (IndexOutOfBoundsException e) {
      throw e;
    }
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true,
      skippedDescriptor = "(Ljava/lang/Object;)I")
  public static final <T> int indexOf(List<T> list, Object o, Ret ret) {
    return list.indexOf(o);
  }

  @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true,
      skippedDescriptor = "(Ljava/lang/Object;)I")
  public static final <T> int lastIndexOf(List<T> list, Object o, Ret ret) {
    return list.lastIndexOf(o);
  }

}
