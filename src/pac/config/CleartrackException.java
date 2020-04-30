package pac.config;

/**
 * Convenience class for check exceptions.  When an exception is thrown we'd like
 * to be able to tell if it was an exception related to the check or if it was some
 * other unrelated Exception.  Therefore we should use this class for exceptions on
 * checks that we define in the config file.
 * 
 * @author jeikenberry
 */
public class CleartrackException extends RuntimeException {
    private static final long serialVersionUID = -5931323355653644841L;

    public CleartrackException(String message) {
        super(message);
    }
}
