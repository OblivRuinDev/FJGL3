/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import org.jspecify.annotations.*;
import org.lwjgl.system.libffi.*;

import java.lang.foreign.*;
import java.lang.invoke.*;

import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Upcalls.*;
import static org.lwjgl.system.libffi.LibFFI.*;

/**
 * Base class for dynamically created native functions that call into Java code.
 *
 * <p>Callback instances use native resources and must be explicitly freed when no longer used by calling the {@link #free} method.</p>
 */
public abstract class Callback implements Pointer, NativeResource {

    private long address;

    /**
     * Creates a callback instance using the specified libffi CIF.
     *
     * @param descriptor the callback descriptor
     */
    @SuppressWarnings("this-escape")
    protected Callback(Descriptor descriptor) {
        this.address = upcallCreate(descriptor, this);
    }

    /**
     * Creates a callback instance using the specified function address
     *
     * @param address the function address
     */
    protected Callback(long address) {
        if (CHECKS) {
            check(address);
        }
        this.address = address;
    }

    @Override
    public long address() {
        return address;
    }

    @Override
    public void free() {
        free(address());
    }

    /**
     * Converts the specified function pointer to the {@code CallbackI} instance associated with it.
     *
     * @param functionPointer a function pointer
     * @param <T>             the {@code CallbackI} instance type
     *
     * @return the {@code CallbackI} instance
     */
    public static <T extends CallbackI> T get(long functionPointer) {
        return upcallGet(functionPointer);
    }

    /** Like {@link #get}, but returns {@code null} if {@code functionPointer} is {@code NULL}. */
    public static <T extends CallbackI> @Nullable T getSafe(long functionPointer) {
        return functionPointer == NULL ? null : get(functionPointer);
    }

    /**
     * Frees any resources held by the specified function pointer.
     *
     * @param functionPointer the function pointer
     */
    public static void free(long functionPointer) {
        upcallFree(functionPointer);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Callback)) {
            return false;
        }

        Callback that = (Callback)o;

        return address == that.address();
    }

    public int hashCode() {
        return (int)(address ^ (address >>> 32));
    }

    @Override
    public String toString() {
        return String.format("%s pointer [0x%X]", getClass().getSimpleName(), address);
    }

    /** An upcall descriptor. [INTERNAL API] */
    public static final class Descriptor {

        final Class<? extends CallbackI> type;

        final MethodHandles.Lookup lookup;

        FFICIF cif;
        FunctionDescriptor desc;

        public Descriptor(Class<? extends CallbackI> type, MethodHandles.Lookup lookup, FFICIF cif) {
            this.type = type;
            this.lookup = lookup;
            this.cif = cif;
        }

        public Descriptor(Class<? extends CallbackI> type, MethodHandles.Lookup lookup, int abi, FFIType rtype, FFIType... atypes) {
            this.type = type;
            this.lookup = lookup;
            if (abi == FFI_DEFAULT_ABI && apiUseJavaForeignLinker()) {
                initDesc(rtype, atypes);
            } else {
                initCif(abi, rtype, atypes);
            }
        }
        public Descriptor(Class<? extends CallbackI> type, MethodHandles.Lookup lookup, MemoryLayout rtype, MemoryLayout... atypes) {
            this.type = type;
            this.lookup = lookup;
            if (apiUseJavaForeignLinker()) {
                initDesc(rtype, atypes);
            } else {
                initCif(FFI_DEFAULT_ABI, rtype, atypes);
            }
        }
        private void initCif(int abi, FFIType rtype, FFIType... atypes) {
            this.cif = apiCreateCIF(abi, rtype, atypes);
        }
        private void initDesc(@Nullable MemoryLayout rtype, MemoryLayout... atypes) {
            if (rtype == null) {
                this.desc = FunctionDescriptor.ofVoid(atypes);
            } else {
                this.desc = FunctionDescriptor.of(rtype, atypes);
            }
        }
    }

}
