/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 *
 * Modified from LWJGL source code.
 * Original copyright notice below.
 */
/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import jdk.internal.misc.*;
import jdk.internal.vm.annotation.*;
import org.jspecify.annotations.*;
import org.lwjgl.*;

import java.nio.*;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryUtil.*;

/**
 * Pointer interface.
 *
 * <p>LWJGL can run on both 32bit and 64bit architectures. Since LWJGL applications deal with native memory directly, this interface provides necessary
 * information about the underlying architecture of the running JVM process.</p>
 *
 * <p>When interacting with native functions, pointer values are mapped to Java {@code long}. LWJGL automatically converts long values to the correct pointer
 * addresses when used in native code. Native functions sometimes require arrays of pointer values; the {@link PointerBuffer} class may be used for that
 * purpose. It has an API similar to a {@link java.nio.LongBuffer} but handles pointer casts automatically.</p>
 */
public interface Pointer {

    /** The pointer size in bytes. Will be 4 on a 32bit JVM and 8 on a 64bit one. */
    int POINTER_SIZE = Unsafe.getUnsafe().addressSize();

    /** The pointer size power-of-two. Will be 2 on a 32bit JVM and 3 on a 64bit one. */
    int POINTER_SHIFT = POINTER_SIZE == 8 ? 3 : 2;

    /** The value of {@code sizeof(long)} for the current platform. */
    int CLONG_SIZE = POINTER_SIZE == 8 && Platform.get() == Platform.WINDOWS ? 4 : POINTER_SIZE;

    /** The value of {@code sizeof(long)} as a power-of-two. */
    int CLONG_SHIFT = CLONG_SIZE == 8 ? 3 : 2;

    /** Will be true on a 32bit JVM. */
    boolean BITS32 = POINTER_SIZE * 8 == 32;

    /** Will be true on a 64bit JVM. */
    boolean BITS64 = POINTER_SIZE * 8 == 64;

    /**
     * Returns the raw pointer address as a {@code long} value.
     *
     * @return the pointer address
     */
    long address();

    /** Default {@link Pointer} implementation. */
    abstract class Default implements Pointer {

        protected final long address;

        protected Default(long address) {
            if (CHECKS && address == NULL) {
                throw new NullPointerException();
            }
            this.address = address;
        }

        @Override
        public long address() {
            return address;
        }

        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Pointer)) {
                return false;
            }

            Pointer other = (Pointer)o;

            return this.address == other.address();
        }

        public int hashCode() {
            return (int)(address ^ (address >>> 32));
        }

        @Override
        public String toString() {
            return String.format("%s pointer [0x%X]", getClass().getSimpleName(), address);
        }

        /**
         * Creates a new {@code Pointer.Default} instance of the specified class at the specified memory address.
         *
         * <p>This method does not run a constructor. The instance is allocated with {@code Unsafe.allocateInstance}, which
         * initializes {@code type} if necessary but never invokes its constructor, and only the inherited {@code address}
         * field is set. Instance fields declared by {@code type} are therefore left at their default values and field
         * initializers do not run.</p>
         *
         * @param type   the pointer class; must not be {@code null}
         * @param address the pointer memory address; must not be {@code NULL} when {@link Checks#CHECKS checks} are enabled
         * @param <T>     the pointer type
         *
         * @return a new {@code Pointer.Default} instance at the specified address
         *
         * @throws NullPointerException      if {@code type} is {@code null}, or if {@code address} is {@code NULL} and
         *                                   {@link Checks#CHECKS checks} are enabled
         * @throws InstanceAllocateException if {@code type} cannot be instantiated
         */
        @ForceInline
        protected static <T extends Pointer.Default> T createPointer(Class<T> type, long address) {
            //noinspection ConstantValue
            if (type == null) { // must check here to avoid crash jvm!
                throw new NullPointerException("type is null!");
            }
            if (CHECKS) {
                if (address == NULL) {
                    throw new NullPointerException("address is null");
                }
            }
            try {
                Object instance = UNSAFE.allocateInstance(type);
                UNSAFE.putLong(instance, POINTER_DEF_ADDRESS, address);
                return (T) instance;
            } catch (InstantiationException e) {
                throw new InstanceAllocateException(e);
            }
        }
    }

}