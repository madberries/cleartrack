package pac.util;

/**
 * All instrumented methods take an object of this type as the final
 * argument for the purpose of:
 * 
 * <ol>
 * <li>Distinguishing instrumented methods from uninstrumented ones.</li>
 * <li>Transferring taint across method calls.</li>
 * </ol>
 * 
 * @author jeikenberry
 */
public final class Ret {

    /** holds the taint of the return value */
    public int taint;

    /** indicates that we are breaking from a deeply recursive method */
    public boolean breakFromRecursion;

}
