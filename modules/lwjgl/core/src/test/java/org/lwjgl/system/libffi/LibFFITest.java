/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system.libffi;

import org.lwjgl.*;
import org.lwjgl.system.*;
import org.testng.annotations.*;

import java.lang.foreign.*;
import java.nio.*;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.Pointer.*;
import static org.lwjgl.system.libffi.LibFFI.*;
import static org.testng.Assert.*;

@Test
public class LibFFITest {

    public void testConstants() {
        assertEquals(ffi_get_default_abi(), FFI_DEFAULT_ABI);
        assertEquals(ffi_get_closure_size(), FFIClosure.SIZEOF);
    }

    /** Calls {@link MemoryAccessJNI#nputInt} using libffi. */
    public void testDowncall() {
        // Get the function address. Ignore this particular implementation, normally you'd create
        // a SharedLibrary instance here and call getFunctionAddress("<function name>").
        downcall(getMemSetAddress());
    }

    private static void downcall(long functionAddress) {
        // Prepare the call interface
        FFICIF cif = FFICIF.malloc();

        PointerBuffer argumentTypes = BufferUtils.createPointerBuffer(3) // 3 arguments
            .put(0, ffi_type_pointer) // void*
            .put(1, ffi_type_sint) // int
            .put(2, ffi_type_pointer); // size_t

        int status = ffi_prep_cif(cif, FFI_DEFAULT_ABI, ffi_type_pointer, argumentTypes);
        if (status != FFI_OK) {
            throw new IllegalStateException("ffi_prep_cif failed: " + status);
        }

        // An array of pointers that point to the actual argument values.
        PointerBuffer arguments = BufferUtils.createPointerBuffer(3);

        // Storage for the actual argument values.
        ByteBuffer values = BufferUtils.createByteBuffer(
            POINTER_SIZE +
            Integer.SIZE +
            POINTER_SIZE
        );

        // The memory we'll modify using libffi
        ByteBuffer target = BufferUtils.createByteBuffer(16);
        long targetAddress = memAddress0(target);

        // Setup the argument buffers
        {
            // void*
            arguments.put(memAddress(values));
            PointerBuffer.put(values, targetAddress);

            // int
            arguments.put(memAddress(values));
            values.putInt(0x5A);

            // size_t
            arguments.put(memAddress(values));
            PointerBuffer.put(values, 16);
        }
        arguments.flip();
        values.flip();

        ByteBuffer ret = ByteBuffer.allocateDirect(POINTER_SIZE);

        // Invoke the function and validate
        for (int i = 0; i < 16; ++i) {
            assertEquals(target.get(i), 0);
        }
        ffi_call(cif, functionAddress, ret, arguments);
        for (int i = 0; i < 16; ++i) {
            assertEquals(target.get(i), 0x5A);
        }
        assertEquals(PointerBuffer.get(ret, 0), targetAddress);

        cif.free();
    }

    private static long getMemSetAddress() {
        return Linker.nativeLinker().defaultLookup().findOrThrow("memset").address();
    }

}