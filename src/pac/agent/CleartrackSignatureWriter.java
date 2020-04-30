package pac.agent;

import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.signature.SignatureReader;
import pac.org.objectweb.asm.signature.SignatureVisitor;
import pac.util.AsmUtils;

/**
 * Instruments java generic signatures by replacing primitive types with their
 * taint wrapped type.  Or by adding taint arguments after the type, in the case
 * of primitive types.
 * 
 * @author jeikenberry
 */
public class CleartrackSignatureWriter extends SignatureVisitor {

    /**
     * Buffer used to construct the signature.
     */
    private final StringBuffer buf = new StringBuffer();

    /**
     * Indicates if the signature contains formal type parameters.
     */
    private boolean hasFormals;

    /**
     * Indicates if the signature contains method parameter types.
     */
    private boolean hasParameters;

    /**
     * Stack used to keep track of class types that have arguments. Each element
     * of this stack is a boolean encoded in one bit. The top of the stack is
     * the lowest order bit. Pushing false = *2, pushing true = *2+1, popping =
     * /2.
     */
    private int argumentStack;

    /**
     * Array dimension of next type.
     */
    private int arrDim = 0;

    private boolean onReturnType;

    /**
     * Constructs a new {@link SignatureWriter} object.
     */
    public CleartrackSignatureWriter() {
        super(Opcodes.ASM5);
    }

    // ------------------------------------------------------------------------
    // Implementation of the SignatureVisitor interface
    // ------------------------------------------------------------------------

    public void visitFormalTypeParameter(String name) {
        if (!hasFormals) {
            hasFormals = true;
            buf.append('<');
        }
        buf.append(name);
        buf.append(':');
    }

    public SignatureVisitor visitClassBound() {
        return CleartrackSignatureWriter.this;
    }

    public SignatureVisitor visitInterfaceBound() {
        buf.append(':');
        return this;
    }

    public SignatureVisitor visitSuperclass() {
        endFormals();
        return this;
    }

    public SignatureVisitor visitInterface() {
        return this;
    }

    public SignatureVisitor visitParameterType() {
        endFormals();
        if (!hasParameters) {
            hasParameters = true;
            buf.append('(');
        }
        return this;
    }

    public SignatureVisitor visitReturnType() {
        endFormals();
        if (!hasParameters) {
            buf.append('(');
        }
        visitClassType("pac/util/Ret;");
        buf.append(')');
        onReturnType = true;
        return this;
    }

    public SignatureVisitor visitExceptionType() {
        buf.append('^');
        return this;
    }

    public void visitBaseType(char descriptor) {
        String instType = arrDim > 0 ? AsmUtils.toWrappedArrayType("" + descriptor).getInternalName() : null;
        if (arrDim == 0) {
            buf.append(descriptor);
            if (descriptor != 'V' && !onReturnType)
                buf.append('I');
        } else {
            if (instType != null)
                buf.deleteCharAt(buf.length() - 1);
            buf.append('L');
            buf.append(instType);
            buf.append(';');
        }
        arrDim = 0;
    }

    public void visitTypeVariable(String name) {
        buf.append('T');
        buf.append(name);
        buf.append(';');
        arrDim = 0;
    }

    public SignatureVisitor visitArrayType() {
        buf.append('[');
        arrDim++;
        return this;
    }

    public void visitClassType(String name) {
        buf.append('L');
        buf.append(name);
        argumentStack *= 2;
        arrDim = 0;
    }

    public void visitInnerClassType(String name) {
        endArguments();
        buf.append('.');
        buf.append(name);
        argumentStack *= 2;
    }

    public void visitTypeArgument() {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            buf.append('<');
        }
        buf.append('*');
    }

    public SignatureVisitor visitTypeArgument(char wildcard) {
        if (argumentStack % 2 == 0) {
            ++argumentStack;
            buf.append('<');
        }
        if (wildcard != '=') {
            buf.append(wildcard);
        }
        return this;
    }

    public void visitEnd() {
        endArguments();
        buf.append(';');
        onReturnType = false;
    }

    /**
     * Returns the signature that was built by this signature writer.
     * 
     * @return the signature that was built by this signature writer.
     */
    public String toString() {
        return buf.toString();
    }

    // ------------------------------------------------------------------------
    // Utility methods
    // ------------------------------------------------------------------------

    /**
     * Ends the formal type parameters section of the signature.
     */
    private void endFormals() {
        if (hasFormals) {
            hasFormals = false;
            buf.append('>');
        }
    }

    /**
     * Ends the type arguments of a class or inner class type.
     */
    private void endArguments() {
        if (argumentStack % 2 == 1) {
            buf.append('>');
        }
        argumentStack /= 2;
    }

    /**
     * Creates an instrumented generic signature from one that is uninstrumented.
     * 
     * @param signature
     * @param isConstructor
     * @return
     */
    public static String instrumentSignature(String signature) {
        if (signature == null)
            return null;
        SignatureReader sigReader = new SignatureReader(signature);
        CleartrackSignatureWriter sigWriter = new CleartrackSignatureWriter();
        sigReader.accept(sigWriter);
        String newSig = sigWriter.toString();
        return newSig;
    }
    
}
