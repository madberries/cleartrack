package pac.inst.taint;

import java.util.Collection;

import pac.inst.InstrumentationClass;
import pac.inst.InstrumentationLocation;
import pac.inst.InstrumentationMethod;
import pac.util.Ret;

@InstrumentationClass(value = "java/util/Collection", isInterface = true)
public class CollectionInstrumentation {

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "(Ljava/lang/Object;)Z")
    public static final <T> boolean add(Collection<T> list, T obj, Ret ret) {
        return list.add(obj);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "(Ljava/lang/Object;)Z")
    public static final <T> boolean remove(Collection<T> list, Object o, Ret ret) {
        return list.remove(o);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "(Ljava/lang/Object;)Z")
    public static final <T> boolean equals(Collection<T> list, Object o, Ret ret) {
        return list.equals(o);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "(Ljava/lang/Object;)Z")
    public static final <T> boolean contains(Collection<T> list, Object o, Ret ret) {
        return list.contains(o);
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "()[Ljava/lang/Object;")
    public static final <T> Object[] toArray(Collection<T> list, Ret ret) {
        return list.toArray();
    }

    @InstrumentationMethod(instrumentationLocation = InstrumentationLocation.APP, canExtend = true, skippedDescriptor = "([Ljava/lang/Object;)[Ljava/lang/Object;")
    public static final <T> T[] toArray(Collection<T> collection, T[] a, Ret ret) {
        return collection.toArray(a);
    }

}
