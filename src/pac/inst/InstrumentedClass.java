package pac.inst;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * This annotation only exists so that I can easily distinguish at runtime
 * the classes that have been instrumented statically.
 * 
 * @author jeikenberry
 */
@Target(ElementType.TYPE)
public @interface InstrumentedClass {

    /** 
     * Current revision number of cleartrack that the annotated class was
     * instrumented with.
     */
    String cleartrack_rev();

    /**
     * List of all of the instrumented options.
     */
    String[] cleartrack_opts();

}
