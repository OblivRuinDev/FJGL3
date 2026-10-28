/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import java.nio.*;

import static org.lwjgl.system.MemoryStack.*;

/** A provider of native function addresses. */
@FunctionalInterface
public interface FunctionProvider {

    /** {@link CharSequence} version of {@link #getFunctionAddress(ByteBuffer)}. */
    default long getFunctionAddress(CharSequence functionName) {
        try (MemoryStack stack = stackPush()) {
            return getFunctionAddress(stack.ASCII(functionName));
        }
    }

    /**
     * Returns the function address of the specified function. If the function is not supported, returns 0L.
     *
     * @param functionName the encoded name of the function to query
     *
     * @return the function address or 0L if the function is not supported
     */
    long getFunctionAddress(ByteBuffer functionName);

    default long[] getFunctionAddresses(CharSequence... functionNames) {
        MemoryStack stack = stackGet();
        long[] caps = new long[functionNames.length];
        long pointer = stack.getPointerAddress();
        for (int i = 0; i < functionNames.length; i++) {
            try {
                caps[i] = getFunctionAddress(stack.ASCII(functionNames[i]));
            } finally {
                stack.setPointerAddress(pointer);
            }
        }
        return caps;
    }

}