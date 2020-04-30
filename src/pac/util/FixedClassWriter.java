/*
 * Copyright (C) 2010-2012 The Project Lombok Authors.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package pac.util;

import pac.org.objectweb.asm.ClassReader;
import pac.org.objectweb.asm.ClassVisitor;
import pac.org.objectweb.asm.ClassWriter;
import pac.org.objectweb.asm.MethodVisitor;
import pac.org.objectweb.asm.Opcodes;
import pac.org.objectweb.asm.commons.JSRInlinerAdapter;

/**
 * Utility class for essentially inlining all JSR/RET instructions into
 * subroutines.
 * 
 * @author jeikenberry
 */
public class FixedClassWriter extends ClassWriter {

    FixedClassWriter(ClassReader classReader, int flags) {
        super(classReader, flags);
    }

    public static byte[] fixJSRInlining(ClassReader reader) {
        ClassWriter writer = new FixedClassWriter(reader, 0);
        ClassVisitor visitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature,
                                             String[] exceptions) {
                return new JSRInlinerAdapter(super.visitMethod(access, name, desc, signature, exceptions), access, name,
                        desc, signature, exceptions);
            }
        };
        reader.accept(visitor, 0);
        return writer.toByteArray();
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        // By default, ASM will attempt to live-load the class types, which 
        // will fail if meddling with classes in an environment with custom
        // classloaders, such as Equinox. It's just an optimization; returning
        // Object is always legal.
        try {
            return super.getCommonSuperClass(type1, type2);
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Throwable t) {
            return "java/lang/Object";
        }
    }

}