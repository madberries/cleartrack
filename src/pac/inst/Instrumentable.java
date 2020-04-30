package pac.inst;

import pac.util.Ret;

/**
 * To ensure complete compatibility, the Object class must not be altered.  However, 
 * all toString() methods need to be instrumented, to ensure that we are correctly
 * propagating taint for all subclasses that override this method.  To compensate for
 * this, all such classes that override toString() must implement this interface.
 * 
 * @author jeikenberry
 */
public interface Instrumentable {

    public String toString(Ret ret);

}
