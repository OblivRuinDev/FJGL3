/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

import org.jspecify.annotations.*;
import org.lwjgl.system.libffi.*;

import java.lang.classfile.*;
import java.lang.foreign.*;
import java.lang.invoke.*;

import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.ForeignType.*;
import static org.lwjgl.system.Pointer.*;

class ForeignSupport {
    static MemoryLayout toLayout(int type) {
        switch (type) {
            case 'P':
                return P;
            case 'I':
                return I;
            case 'J':
                return J;
            case 'B':
                return B;
            case 'S':
                return S;
            case 'D':
                return D;
            case 'F':
                return F;
            case 'Z':
                return Z;
            default:
                throw new IllegalArgumentException("Unknown layout type: " + type);
        }
    }
    static Class<?> jType(int cType) {
        switch (cType) {
            case 'P', 'J':
                return long.class;
            case 'I':
                return int.class;
            case 'N':
                return CLONG_SIZE == 8 ? long.class : int.class;
            case 'Z':
                return boolean.class;
            case 'S':
                return short.class;
            case 'B':
                return byte.class;
            case 'F':
                return float.class;
            case 'D':
                return double.class;
            default:
                throw new IllegalArgumentException("Unknown layout type: " + cType);
        }
    }
    static MethodType jType(String cType) {
        int len = cType.length();
        if (len < 1) {
            throw new IllegalArgumentException("Illegal cType: " + cType);
        }
        char c = cType.charAt(0);
        Class<?> rtype = c == 'V' ? void.class : jType(c);
        if (len != 1) {
            Class<?>[] ptypes = new Class<?>[len];
            ptypes[0] = long.class;
            for (int index = 1; index < len; ++index) {
                ptypes[index] = jType(cType.charAt(index));
            }
            return MethodType.methodType(rtype, ptypes);
        } else {
            return MethodType.methodType(rtype, long.class);
        }
    }

    public static final class FFM implements ForeignType.LazyFFI {
        @Nullable
        volatile FFIType   ffiType;
        final MemoryLayout layout;

        FFM(MemoryLayout layout) {
            this.layout = layout;
        }

        @Override
        public FFIType ffiType() {
            FFIType ffiType = this.ffiType;
            if (ffiType == null) {
                throw new UnsupportedOperationException();//todo
            }
            return ffiType;
        }

        @Override
        public MemoryLayout layout() {
            return layout;
        }
    }
    static final class FFI implements ForeignType.LazyFFM {
        final FFIType ffiType;
        @Nullable
        volatile MemoryLayout layout;

        FFI(FFIType ffiType, @Nullable String ffiName, @Nullable MemoryLayout layout) {
            this.ffiType = ffiType;
            if (layout == null) {
                this.layout = null;
            } else {
                if (layout.byteSize() != ffiType.size()) {
                    throw new IllegalStateException("FFI type: " + ffiName + "(size: " + ffiType.size() + ") do not match FFM: " + layout);
                }
                layout = layout.withByteAlignment(ffiType.alignment());
                this.layout = ffiName != null ? layout.withName(ffiName) : layout;
            }
        }
        FFI(FFIType ffiType) {
            this.ffiType = ffiType;
        }

        @Override
        public FFIType ffiType() {
            return ffiType;
        }

        @Override
        public MemoryLayout layout() {
            throw new UnsupportedOperationException();
        }
    }
    public static final class TempMembers implements ForeignType.Temp {
        public final int type;
        public final @Nullable String name;
        public final ForeignType[] members;
        TempMembers(int type, @Nullable String name, ForeignType[] members) {
            this.type = type;
            this.name = name;
            this.members = members;
        }
        public FFIType[] toFFI() {
            FFIType[] members = new FFIType[this.members.length];
            for (int i = 0; i < members.length; ++i) {
                members[i] = this.members[i].ffiType();
            }
            return members;
        }
        public MemoryLayout[] toFFM() {
            MemoryLayout[] members = new MemoryLayout[this.members.length];
            for (int i = 0; i < members.length; ++i) {
                members[i] = this.members[i].layout();
            }
            return members;
        }
        @Override
        public FFIType ffiType() {
            switch (type) {
                case 0:
                    return apiCreateStruct(toFFI());
                case 1:
                    return apiCreateUnion(toFFI());
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }
        @Override public MemoryLayout layout() {
            switch (type) {
                case 0:
                    return apiCreateStruct(toFFM());
                case 1:
                    return MemoryLayout.unionLayout(toFFM());
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }
        }
        @Override public ForeignType resolveToFFI() {
            return new FFI(ffiType(), name, null);
        }
    }
    public static final class TempArray implements ForeignType.Temp {
        public final int         length;
        public final ForeignType element;

        public TempArray(ForeignType element, int length) {
            this.element = element;
            this.length = length;
        }

        @Override
        public FFIType ffiType() {
            return apiCreateArray(element.ffiType(), length);
        }
        @Override
        public MemoryLayout layout() {
            return MemoryLayout.sequenceLayout(length, element.layout());
        }
        @Override public ForeignType resolveToFFI() {
            return new FFI(ffiType());
        }
    }
}
