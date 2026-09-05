/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system.ffm.mapping;

import org.jspecify.annotations.*;
import org.lwjgl.system.*;
import org.lwjgl.system.libffi.*;

import java.lang.foreign.*;
import java.lang.invoke.*;
import java.util.*;
import jdk.internal.vm.annotation.Stable;

import static java.lang.foreign.MemoryLayout.*;
import static java.lang.foreign.ValueLayout.*;
import static java.lang.invoke.MethodType.*;

public abstract class FFIMapping extends Struct<FFIMapping> {
    public static final ValueLayout.OfBoolean boolean__uint8 = JAVA_BOOLEAN.withName("ffi_type_uint8").withByteAlignment(LibFFI.ffi_type_uint8.alignment());

    public static final ValueLayout.OfByte byte__uint8 = JAVA_BYTE.withName("ffi_type_uint8").withByteAlignment(LibFFI.ffi_type_uint8.alignment());
    public static final ValueLayout.OfByte byte__sint8 = JAVA_BYTE.withName("ffi_type_sint8").withByteAlignment(LibFFI.ffi_type_sint8.alignment());

    public static final ValueLayout.OfShort short__uint16 = JAVA_SHORT.withName("ffi_type_uint16").withByteAlignment(LibFFI.ffi_type_uint16.alignment());
    public static final ValueLayout.OfShort short__sint16 = JAVA_SHORT.withName("ffi_type_sint16").withByteAlignment(LibFFI.ffi_type_sint16.alignment());


    public static final ValueLayout.OfInt int__uint32 = JAVA_INT.withName("ffi_type_uint32").withByteAlignment(LibFFI.ffi_type_uint32.alignment());
    public static final ValueLayout.OfInt int__sint32 = JAVA_INT.withName("ffi_type_sint32").withByteAlignment(LibFFI.ffi_type_sint32.alignment());


    public static final ValueLayout.OfLong long__uint64 = JAVA_LONG.withName("ffi_type_uint64").withByteAlignment(LibFFI.ffi_type_uint64.alignment());
    public static final ValueLayout.OfLong long__sint64 = JAVA_LONG.withName("ffi_type_sint64").withByteAlignment(LibFFI.ffi_type_sint64.alignment());
    public static final ValueLayout.OfLong long__ulong = JAVA_LONG.withName("ffi_type_ulong").withByteAlignment(LibFFI.ffi_type_ulong.alignment());
    public static final ValueLayout.OfLong long__slong = JAVA_LONG.withName("ffi_type_slong").withByteAlignment(LibFFI.ffi_type_slong.alignment());

    public static final AddressLayout address__pointer = ADDRESS.withName("ffi_type_pointer").withByteAlignment(LibFFI.ffi_type_pointer.alignment());


    public static final MemoryLayout float__float = JAVA_FLOAT.withName("ffi_type_float").withByteAlignment(LibFFI.ffi_type_float.alignment());
    public static final MemoryLayout double__double = JAVA_DOUBLE.withName("ffi_type_double").withByteAlignment(LibFFI.ffi_type_double.alignment());



    private FFIMapping() {
        super(0, null);
    }

    public static final class Wrapper {
        @Nullable
        @Stable
        private MethodHandle create;
        public final StructLayout layout;
        public final Class<? extends Struct<?>> type;
        public Wrapper(Class<? extends Struct<?>> type, Object... members) throws ReflectiveOperationException {
            this.type = type;
            int packAlignment = DEFAULT_PACK_ALIGNMENT;
            int alignas = DEFAULT_ALIGN_AS;
            ArrayList<MemoryLayout> layoutList = new ArrayList<>(members.length * 2);
            MemoryLayout member = getLayout(members[0]);
            layoutList.add(member);
            long offset = member.byteSize();
            long maxAlignment = member.byteAlignment();
            for (int i = 1; i < members.length; ++i) {
                member = getLayout(members[i]);
                long memberSize = member.byteSize();
                long memberAlign = member.byteAlignment();

                long alignedOffset = ((offset + memberAlign - 1) / memberAlign) * memberAlign;

                if (alignedOffset > offset) {
                    long padBytes = alignedOffset - offset;
                    layoutList.add(paddingLayout(padBytes));
                }
                layoutList.add(member);
                offset = alignedOffset + memberSize;
                maxAlignment = Math.max(maxAlignment, memberAlign);
            }

            long trail = (maxAlignment - (offset % maxAlignment)) % maxAlignment;
            if (trail > 0) {
                layoutList.add(paddingLayout(trail * 8));
                offset += trail;
            }
            this.layout = MemoryLayout.structLayout(layoutList.toArray(new MemoryLayout[0])).withByteAlignment(maxAlignment);
        }
        private static MemoryLayout getLayout(Object object) {
            return object instanceof MemoryLayout ? (MemoryLayout) object : ((Wrapper) object).layout;
        }
        public MethodHandle handleCreate(MethodHandles.Lookup lookup) {
            if (this.create == null) {
                try {
                    this.create = lookup.findStatic(type, "create", methodType(type, long.class));
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            return this.create;
        }
    }
}
