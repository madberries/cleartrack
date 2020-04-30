
package pac.config;

import java.lang.reflect.Constructor;

import pac.util.TaintUtils;
import pac.wrap.CharArrayTaint;

// append
// prepareForExceptionOrTerminate
// setExceptionConstructor
// getExceptionConstructor
// setAction
// getAction
// getMessage
// setDescription
// getDescription
// setChk
// getChk
// isMessage
// setAlteredShell
// getAlteredShell
// getCweNumber
// setCweNumber

public class NotifyMsg {
    private int writeCount = 0;
    private String description;
    private Chk chk;
    final private String command;
    private int cweNumber;

    private int action;
    private Constructor<?> exceptionConstructor;
    private boolean alteredShell; // Is set when ex: bash is changed to /bin/bash

    final private StringBuffer msg, taintOutput;

    /**
     * @param description The Api method description WITHOUT return type and WITHOUT parameter identifiers
     *        ex. "Runtime.exec(String, String[], File)"
     */
    public NotifyMsg(final String description, final String command, final int cweNumber) {
        this.description = description;
        this.command = command;
        this.cweNumber = cweNumber;
        msg = new StringBuffer();
        taintOutput = new StringBuffer();
        alteredShell = false;
    }

    /**
    * @param description The Api method description WITHOUT return type and WITHOUT parameter identifiers
    *        ex. "Runtime.exec(String, String[], File)"
    */
    public NotifyMsg(final String description, final String command) {
        this.description = description;
        this.command = command;
        this.cweNumber = 0;
        msg = new StringBuffer();
        taintOutput = new StringBuffer();
        alteredShell = false;
    }

    public void enterWriteCall() {
        writeCount++;
    }

    public void exitWriteCall() {
        writeCount--;
    }

    public boolean inWriteCall() {
        return writeCount > 1;
    }

    /**
     * Call this method: After altering untrusted data
     *                   When action is exception of terminate, just before calling Notify.notifyAndRespond
     * @param msg
     */
    public void append(final String msg) {
        this.msg.append(msg);
    }

    public void prepend(final String msg) {
        this.msg.insert(0, msg);
    }

    /**
     * Call this method after addTaintOutput() to mark the offending line
     * in the output that was previously added.
     * 
     * @param start of the offending region (inclusive)
     * @param end of the offending region (inclusive)
     */
    public void addTaintMarker(int start, int end) {
        for (int i = 0; i < start; i++)
            taintOutput.append(' ');
        for (int i = start; i <= end; i++)
            taintOutput.append('^');
        taintOutput.append('\n');
    }

    public void addTaintOutput(CharArrayTaint cmd) {
        if (cmd != null) {
            // add two lines: A line that indicate taint value of each char. The line of text.
            taintOutput.append(CharArrayTaint.createTaintDisplayLines(cmd));
        }
    }

    public void addTaintOutput(String cmd) {
        if (cmd != null) {
            // add two lines: A line that indicate taint value of each char. The line of text.
            taintOutput.append(TaintUtils.createTaintDisplayLines(cmd));
        }
    }

    public void addTaintOutput(String[] cmd) {
        if (cmd != null && cmd.length > 0) {
            for (int i = 0; i < cmd.length; i++) {
                // add two lines: A line that indicate taint value of each char. The line of text.
                taintOutput.append(TaintUtils.createTaintDisplayLines(cmd[i]));
            }
        }
    }

    public String getTaintOutput() {
        return taintOutput.toString();
    }

    /**
    * Call this method immediately before calling Notify.notifyAndRespond
    * when action is either exception or terminate
    *
    * @param constructor
    * @param action
    */
    public void prepareForExceptionOrTerminate(final Constructor<?> constructor, final int action) {
        setExceptionConstructor(constructor);
        setAction(action);
    }

    public void setExceptionConstructor(final Constructor<?> constructor) {
        exceptionConstructor = constructor;
    }

    Constructor<?> getExceptionConstructor() {
        return exceptionConstructor;
    }

    /**
    * Any method that calls this.append() above, should call setAction:
    *    o before returning
    *    o before calling Notify.notifyAndRespond
    * @param action
    */
    public void setAction(final int action) {
        this.action = action;
    }

    int getAction() {
        return action;
    }

    String getCommand() {
        return command;
    }

    String getMessage() {
        return msg.toString();
    }

    void setDescription(final String description) {
        this.description = description;
    }

    String getDescription() {
        return description;
    }

    void setChk(final Chk chk) {
        this.chk = chk;
    }

    Chk getChk() {
        return chk;
    }

    public boolean isMessage() {
        return (msg.length() > 0);
    }

    public void setAlteredShell() {
        alteredShell = true;
    }

    public boolean getAlteredShell() {
        return alteredShell;
    }

    int getCweNumber() {
        return cweNumber;
    }

    public void setCweNumber(final int cweNumber) {
        this.cweNumber = cweNumber;
    }
} // class NotifyMsg
