/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

import org.jspecify.annotations.*;
import org.lwjgl.system.libffi.*;

import java.lang.foreign.*;

import static java.lang.foreign.ValueLayout.*;
import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.Pointer.*;
import static org.lwjgl.system.libffi.LibFFI.*;

/**
 * <h1>Internal API</h1>
 * This is an internal API and it may be changed without prior notice.
 * <p>
 * A mapping between a native (C) type and the two representations used by LWJGL callbacks:
 * <ul>
 *     <li>{@link #ffiType()}, the libffi type used by the legacy callback backend, and</li>
 *     <li>{@link #layout()}, the FFM layout used by the {@code java.lang.foreign} backend.</li>
 * </ul>
 *
 * <p>{@code FFIMapping} instances are attached to {@link org.lwjgl.system.Callback.Descriptor} instances, which derive the libffi {@code ffi_cif} from the former
 * and the {@link FunctionDescriptor} from the latter. The {@link FunctionDescriptor} determines the exact signature of the {@code ffmCall} method that is
 * generated in every {@link org.lwjgl.system.CallbackI} interface.</p>
 */
public sealed interface ForeignType {
    FFIType ffiType();
    MemoryLayout layout();


    ValueLayout CLONG = CLONG_SIZE == 8 ? JAVA_LONG : JAVA_INT;

    ForeignType ft_uint8  = new Common(ffi_type_uint8 , "ffi_type_uint8" , JAVA_BYTE);
    ForeignType ft_sint8  = new Common(ffi_type_sint8 , "ffi_type_sint8" , JAVA_BYTE);
    ForeignType ft_uint16 = new Common(ffi_type_uint16, "ffi_type_uint16", JAVA_SHORT);
    ForeignType ft_sint16 = new Common(ffi_type_sint16, "ffi_type_sint16", JAVA_SHORT);
    ForeignType ft_uint32 = new Common(ffi_type_uint32, "ffi_type_uint32", JAVA_INT);
    ForeignType ft_sint32 = new Common(ffi_type_sint32, "ffi_type_sint32", JAVA_INT);
    ForeignType ft_uint64 = new Common(ffi_type_uint64, "ffi_type_uint64", JAVA_LONG);
    ForeignType ft_sint64 = new Common(ffi_type_sint64, "ffi_type_sint64", JAVA_LONG);

    ForeignType ft_uchar  = new Common(ffi_type_uchar , "ffi_type_uchar" , JAVA_BYTE);
    ForeignType ft_schar  = new Common(ffi_type_schar , "ffi_type_schar" , JAVA_BYTE);
    ForeignType ft_ushort = new Common(ffi_type_ushort, "ffi_type_ushort", JAVA_SHORT);
    ForeignType ft_sshort = new Common(ffi_type_sshort, "ffi_type_sshort", JAVA_SHORT);
    ForeignType ft_uint   = new Common(ffi_type_uint  , "ffi_type_uint"  , JAVA_INT);
    ForeignType ft_sint   = new Common(ffi_type_sint  , "ffi_type_sint"  , JAVA_INT);
    ForeignType ft_ulong  = new Common(ffi_type_ulong , "ffi_type_ulong" , CLONG);
    ForeignType ft_slong  = new Common(ffi_type_slong , "ffi_type_slong" , CLONG);

    ForeignType ft_float  = new Common(ffi_type_float , "ffi_type_float" , JAVA_FLOAT);
    ForeignType ft_double = new Common(ffi_type_double, "ffi_type_double", JAVA_DOUBLE);

    ForeignType ft_pointer = new Common(ffi_type_pointer, "ffi_type_pointer", ADDRESS);
    ForeignType ft_boolean = new Common(ffi_type_uint8  , "ffi_type_uint8@java_boolean", JAVA_BOOLEAN);

    MemoryLayout Z = ft_boolean.layout();
    MemoryLayout B = ft_sint8.layout();
    MemoryLayout S = ft_sint16.layout();
    MemoryLayout I = ft_sint32.layout();
    MemoryLayout J = ft_sint64.layout();
    MemoryLayout P = ft_pointer.layout();
    MemoryLayout F = ft_float.layout();
    MemoryLayout D = ft_double.layout();

    /**
     * <h1>Internal API</h1>
     * This is an internal API and it may be changed without prior notice.
     */
    public final class Common implements ForeignType {
        final FFIType ffiType;
        final MemoryLayout layout;

        Common(FFIType ffiType, @Nullable String ffiName, MemoryLayout layout) {
            this.ffiType = ffiType;
            if (layout.byteSize() != ffiType.size()) {
                throw new IllegalStateException("FFI type: " + ffiName + "(size: " + ffiType.size() + ") do not match FFM: " + layout);
            }
            //todo: the below code is necessary?
            //layout = layout.withByteAlignment(ffiType.alignment());
            this.layout = ffiName != null ? layout.withName(ffiName) : layout;
        }
        Common(FFIType ffiType, MemoryLayout layout) {
            this.ffiType = ffiType;
            this.layout = layout;
        }

        @Override
        public FFIType ffiType() {
            return ffiType;
        }

        @Override
        public MemoryLayout layout() {
            return layout;
        }
    }

    /**
     * <h1>Internal API</h1>
     * This is an internal API and it may be changed without prior notice.
     */
    public sealed interface LazyFFM extends ForeignType
        permits ForeignSupport.FFI, Temp { }
    /**
     * <h1>Internal API</h1>
     * This is an internal API and it may be changed without prior notice.
     */
    public sealed interface LazyFFI extends ForeignType
        permits ForeignSupport.FFM, Temp { }
    /**
     * <h1>Internal API</h1>
     * This is an internal API and it may be changed without prior notice.
     * <p>
     * A mapping that only stores the data required to create an aggregate ({@code struct} / {@code union} / array) mapping.
     *
     */
    public sealed interface Temp extends LazyFFI, LazyFFM
        permits ForeignSupport.TempArray, ForeignSupport.TempMembers {

        ForeignType resolveToFFI();
        default ForeignType resolveToFFM() {
            return new ForeignSupport.FFM(layout());
        }
        default ForeignType resolveAll() {
            return new Common(ffiType(), layout());
        }
    }
    static Temp tempStruct(@Nullable String name, ForeignType... members) {
        return new ForeignSupport.TempMembers(0, name, members);
    }
    static Temp tempUnion(@Nullable String name, ForeignType... members) {
        return new ForeignSupport.TempMembers(1, name, members);
    }
    static Temp tempStruct(ForeignType... members) {
        return new ForeignSupport.TempMembers(0, null, members);
    }
    static Temp tempUnion(ForeignType... members) {
        return new ForeignSupport.TempMembers(1, null, members);
    }
    static Temp tempArray(ForeignType type, int length) {
        return new ForeignSupport.TempArray(type, length);
    }
}
