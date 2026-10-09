/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

import jdk.internal.foreign.*;
import org.jspecify.annotations.*;

import java.lang.foreign.*;
import java.lang.invoke.*;

import static java.lang.invoke.MethodType.*;
import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.Configuration.*;
import static org.lwjgl.system.ForeignType.*;

/**
 * <h1>Internal API</h1>
 * This is an internal API and it may be changed without prior notice.
 */
public class Foreign {
    public static final MethodHandle STACK_GET;

    /** {@code MemorySegment.ofAddress(long)}, used to box a Java {@code long} into the function address / pointer arguments of a downcall handle. */
    static final MethodHandle OF_ADDRESS;
    /** {@code MemorySegment.address()}, used to unbox a pointer return value of a downcall handle into a Java {@code long}. */
    static final MethodHandle SEGMENT_ADDRESS;
    static final boolean USE_JAVA_FOREIGN_LINKER;
    static final boolean FOREIGN_DOWNCALL;
    static final boolean FOREIGN_UPCALL;

    static {
        boolean force = FORCE_USE_JAVA_FOREIGN_LINKER.get(false);
        Boolean down = FFM_DOWNCALL.get();
        Boolean up = FFM_UPCALL.get();
        int status = 0;
        if (force) {
            try {
                Linker.nativeLinker();
                status = 1;
                down = Boolean.TRUE;
                up = Boolean.TRUE;
            } catch (Exception e) {
                throw new IllegalStateException("Force use java foreign linker but failed to get it!", e);
            }
        }

        try {
            if (status != 1) {
                if (CABI.current() != CABI.FALLBACK && CABI.current() != CABI.UNSUPPORTED) {
                    status = 2;
                }
            }
        } catch (LinkageError ex) {
            if (Checks.DEBUG) {
                apiLog("");
                apiLogException(ex);
            }
            try {
                String name = Linker.nativeLinker().getClass().getName();
                if (name.contains("Fallback")) {
                    status = -1;
                } else {
                    status = 3;
                }
            } catch (UnsupportedOperationException e2) {
                status = -1;
            }
        }

        USE_JAVA_FOREIGN_LINKER = status != -1;
        FOREIGN_DOWNCALL = USE_JAVA_FOREIGN_LINKER && Boolean.TRUE.equals(down);
        FOREIGN_UPCALL = USE_JAVA_FOREIGN_LINKER && Boolean.TRUE.equals(up);

        var lookup = MethodHandles.lookup();
        var mtd_SegAlloc = methodType(SegmentAllocator.class);
        try {
            var back = lookup.findStatic(MemoryStack.class, "stackGet", methodType(MemoryStack.class)).asType(mtd_SegAlloc);
            Object cfg = FFM_DOWNCALL_SEGMENT_ALLOCATOR.get();
            var getter = back;
            if (cfg != null) {
                if (cfg instanceof String path) {
                    int dot = path.lastIndexOf('.');
                    if (dot != -1) {
                        try {
                            Class<?> clazz = Class.forName(path.substring(0, dot));
                            getter = lookup.unreflect(clazz.getMethod(path.substring(dot + 1))).asType(mtd_SegAlloc);
                        } catch (ReflectiveOperationException | WrongMethodTypeException e) {
                            apiLogException(e);
                        }
                    } else {
                        apiLog("[FJGL] Bad \"org.lwjgl.system.downcall.SegmentAllocator\" value ".concat(path));
                    }
                } else if (cfg instanceof MethodHandle) {
                    try {
                        getter = ((MethodHandle) cfg).asType(mtd_SegAlloc);
                    } catch (WrongMethodTypeException e) {
                        apiLogException(e);
                    }
                } else if (cfg instanceof SegmentAllocator) {
                    getter = MethodHandles.constant(SegmentAllocator.class, (SegmentAllocator) cfg);
                }
            }

            STACK_GET = getter;
            OF_ADDRESS = lookup.findStatic(MemorySegment.class, "ofAddress", methodType(MemorySegment.class, long.class));
            SEGMENT_ADDRESS = lookup.findVirtual(MemorySegment.class, "address", methodType(long.class));
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Nullable//todo
    /*public*/ static MethodHandle lookup(String cType) throws IllegalAccessException {
        try {
            return MethodHandles.lookup().findStatic(Downcall.class, cType, ForeignSupport.jType(cType));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     *
     * @param cType each char must be one of the {@code P, I, J, B, S, D, F, Z}, if is 1st char, it can also be {@code V}
     * @return a downcall handle which follow {@code cType}
     */
    static MethodHandle create(String cType, Linker.Option... options) {
        // The cType is the sequence of native type letters, the return type first and the parameters after it. C long ("N") is never passed here: the
        // generator resolves it to "I" or "J" depending on the platform.
        MemoryLayout[] paras = new MemoryLayout[cType.length() - 1];
        MethodHandle[] adapter = new MethodHandle[paras.length + 1];
        adapter[0] = OF_ADDRESS;// __functionAddress
        for (int i = 1; i < cType.length(); ++i) {
            //noinspection DataFlowIssue
            if ((paras[i - 1] = ForeignSupport.toLayout(cType.charAt(i))) == P) {
                adapter[i] = OF_ADDRESS;
            }
        }

        var desc = cType.charAt(0) == 'V' ? FunctionDescriptor.ofVoid(paras) : FunctionDescriptor.of(ForeignSupport.toLayout(cType.charAt(0)), paras);
        MethodHandle handle = Linker.nativeLinker().downcallHandle(desc, options);

        // The linker represents the function address and every pointer layout as a MemorySegment. The generated adapters work exclusively with primitive
        // values, so the address is boxed and every pointer argument/return value is unboxed here. The resulting handle type is exactly the adapter
        // descriptor, which lets the adapters share the descriptor entry with their own method descriptor.
        MethodType jType = handle.type();
        if (jType.parameterCount() >= 2 && jType.parameterType(1) == SegmentAllocator.class) {
            handle = MethodHandles.foldArguments(handle, 1, STACK_GET);
        }
        if (jType.returnType() == MemorySegment.class) {
            handle = MethodHandles.filterReturnValue(handle, SEGMENT_ADDRESS);
        }
        handle = MethodHandles.filterArguments(handle, 0, adapter);

        return handle;
    }
    /**
     * The bootstrap method of every downcall handle dynamic constant.
     *
     * <p>The name of the dynamic constant is the {@code cType} string, so the bootstrap derives the signature from it instead of receiving it (and its
     * {@code MethodType}) as static arguments.</p>
     */
    public static MethodHandle bootstrap(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return create(name);
    }
    public static MethodHandle bootstrapCritical(MethodHandles.Lookup lookup, String name, Class<?> type) {
        return create(name, Linker.Option.critical(false));
    }
}
