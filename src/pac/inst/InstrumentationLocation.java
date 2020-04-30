package pac.inst;

public enum InstrumentationLocation {
    /** Apply to only instrumented JDK methods */
    JDK,

    /** Apply to only instrumented application methods */
    APP,

    /** Apply to all instrumented JDK and application methods */
    ALL,

    /**
     * Apply to all methods for compatibility (instrumented and
     * uninstrumented) 
     */
    COMPAT,

    /** 
     * Apply to all instrumented and uninstrumented application 
     * methods (for transparency)
     */
    TRANS
}
