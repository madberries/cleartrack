package pac.config;

@SuppressWarnings("serial")
public class MsgException extends Exception {
    private String msg;
    private StackTraceElement[] stackTrace;

    // constructor
    public MsgException(String msg) {
        this.msg = msg;
    }

    @Override
    public String getMessage() {
        String retval;

        if (stackTrace == null) {
            retval = msg;
        } else {
            final StringBuilder buf = new StringBuilder();

            for (int i = 0; i < stackTrace.length; i++) {
                final StackTraceElement element = stackTrace[i];
                buf.append("  ");
                buf.append(element.toString());
                buf.append("\n");
            }

            buf.append(msg);
            retval = buf.toString();
        }

        return retval;
    }
}
