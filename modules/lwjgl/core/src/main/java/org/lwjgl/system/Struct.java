/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import jdk.internal.vm.annotation.*;
import org.jspecify.annotations.*;

import java.nio.*;
import java.util.*;

import static java.lang.Math.*;
import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Base class of all struct implementations. */
public abstract class Struct<SELF extends Struct<SELF>> extends Pointer.Default {

    protected static final int DEFAULT_PACK_ALIGNMENT = Platform.get() == Platform.WINDOWS ? 8 : 0x4000_0000;
    protected static final int DEFAULT_ALIGN_AS       = 0;

    static {
        Library.initialize();
    }

    @SuppressWarnings({"unused", "FieldCanBeLocal"})
    protected @Nullable ByteBuffer container;

    /**
     * Creates a struct instance at the specified address.
     *
     * @param address   the struct memory address
     * @param container an optional container buffer, to be referenced strongly by the struct instance.
     */
    protected Struct(long address, @Nullable ByteBuffer container) {
        super(address);
        this.container = container;
    }

    /**
     * Similar to {@link #Struct(long, ByteBuffer) the constructor}, but returns the exact type of the {@code Struct} subclass.
     *
     * <p>Effectively, every {@code Struct} instance is a factory for more instances of the same type. This is used by {@link StructBuffer}.</p>
     *
     * @param address   the struct memory address
     * @param container an optional container buffer, to be referenced strongly by the struct instance.
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    protected SELF create(long address, @Nullable ByteBuffer container) {
        return (SELF) create(this.getClass(), address, container);
    }

    /**
     * @see #create(Class, long)
     */
    @ForceInline
    public static <T extends Struct<?>> T create(Class<T> type, long address, @Nullable ByteBuffer container) {
        var v = createPointer(type, address);
        UNSAFE.putReference(v, STRUCT_CONTAINER, container);
        return v;
    }
    /**
     * Creates a new {@code Struct} instance of the specified class at the specified memory address.
     *
     * <p>This method does not run a constructor. The instance is allocated with {@code Unsafe.allocateInstance}, which
     * initializes {@code type} if necessary but never invokes its constructor, and only the inherited {@code address}
     * field is set. Instance fields declared by {@code type} are therefore left at their default values and field
     * initializers do not run, which is why {@code Struct} subclasses must not declare instance fields; all state must be
     * derived from the memory address.</p>
     *
     * @param type   the struct class; must not be {@code null} and must not declare instance fields
     * @param address the struct memory address; must not be {@code NULL} when {@link Checks#CHECKS checks} are enabled
     * @param <T>     the struct type
     *
     * @return a new {@code Struct} instance at the specified address
     *
     * @throws NullPointerException      if {@code type} is {@code null}, or if {@code address} is {@code NULL} and
     *                                   {@link Checks#CHECKS checks} are enabled
     * @throws InstanceAllocateException if {@code type} cannot be instantiated
     *
     * @see #create(Class, long, ByteBuffer)
     * @see #createSafe(Class, long)
     */
    @ForceInline
    @SuppressWarnings("unchecked")
    public static <T extends Struct<?>> T create(Class<T> type, long address) {
        return createPointer(type, address);
    }

    /**
     * Like {@link #create(Class, long) create}, but returns {@code null} if {@code address} is {@code NULL}.
     *
     * <p>When {@code address} is {@code NULL} no instance is created and {@code clazz} is not validated, so a {@code null}
     * {@code clazz} is <b>not</b> rejected in that case.</p>
     *
     * @param clazz   the struct class; must not be {@code null} unless {@code address} is {@code NULL}
     * @param address the struct memory address, or {@code NULL}
     * @param <T>     the struct type
     *
     * @return a new {@code Struct} instance at the specified address, or {@code null} if {@code address} is {@code NULL}
     *
     * @throws NullPointerException      if {@code clazz} is {@code null} and {@code address} is not {@code NULL}
     * @throws InstanceAllocateException if {@code clazz} cannot be instantiated
     *
     * @see #create(Class, long)
     */
    @Nullable
    @ForceInline
    public static <T extends Struct<?>> T createSafe(Class<T> clazz, long address) {
        return address != NULL ? createPointer(clazz, address) : null;
    }

    /** Returns {@code sizeof(struct)}. */
    public abstract int sizeof();

    /** Zeroes-out the struct data. */
    public void clear() {
        memSet(address(), 0, sizeof());
    }

    /**
     * Frees the struct allocation.
     *
     * <p>This method should not be used if the memory backing this struct is not owned by the struct.</p>
     */
    public void free() {
        nmemFree(address());
    }

    /**
     * Returns true if the pointer member that corresponds to the specified {@code memberOffset} is {@code NULL}.
     *
     * <p>This is useful to verify that not nullable members of an untrusted struct instance are indeed not {@code NULL}.</p>
     *
     * @param memberOffset the byte offset of the member to query
     *
     * @return true if the member is {@code NULL}
     */
    public boolean isNull(int memberOffset) {
        if (DEBUG) {
            checkMemberOffset(memberOffset);
        }
        return memGetAddress(address() + memberOffset) == NULL;
    }

    // ---------------- Implementation utilities ----------------

    private void checkMemberOffset(int memberOffset) {
        if (memberOffset < 0 || sizeof() - memberOffset < POINTER_SIZE) {
            throw new IllegalArgumentException("Invalid member offset.");
        }
    }

    protected static ByteBuffer __checkContainer(ByteBuffer container, int sizeof) {
        if (CHECKS) {
            check(container, sizeof);
        }
        return container;
    }

    private static long getBytes(int elements, int elementSize) {
        return (elements & 0xFFFF_FFFFL) * elementSize;
    }

    protected static long __checkMalloc(int elements, int elementSize) {
        long bytes = (elements & 0xFFFF_FFFFL) * elementSize;
        if (DEBUG) {
            if (elements < 0) {
                throw new IllegalArgumentException("Invalid number of elements");
            }
            if (BITS32 && 0xFFFF_FFFFL < bytes) {
                throw new IllegalArgumentException("The request allocation is too large");
            }
        }
        return bytes;
    }

    protected static ByteBuffer __create(int elements, int elementSize) {
        apiCheckAllocation(elements, getBytes(elements, elementSize), 0x7FFF_FFFFL);
        return ByteBuffer.allocateDirect(elements * elementSize).order(ByteOrder.nativeOrder());
    }

    protected static <T extends Struct<T>> @Nullable ByteBuffer __getContainer(T struct) {
        return struct.container;
    }

    protected static @Nullable ByteBuffer __getContainer(StructBuffer<?, ?> struct) {
        return struct.container;
    }


    /** A functional interface that enables lambda expressions to be passed to the {@link #validate} method. [INTERNAL USE ONLY] */
    @FunctionalInterface
    public interface StructValidation {
        void validate(long struct);
    }

    /**
     * Validates each struct contained in the specified struct array. [INTERNAL USE ONLY]
     *
     * @param array  the struct array to validate
     * @param count  the number of structs in {@code array}
     * @param SIZEOF the size of each struct, in bytes
     */
    public static void validate(long array, int count, int SIZEOF, StructValidation validation) {
        for (int i = 0; i < count; i++) {
            validation.validate(array + Integer.toUnsignedLong(i) * SIZEOF);
        }
    }
    public static void validate(long array, long count, int SIZEOF, StructValidation validation) { validate(array, (int)count, SIZEOF, validation); }

    // ---------------- Struct Member Layout ----------------

    protected static class Member {
        final int     size;
        final int     alignment;
        final boolean forcedAlignment;

        int offset;

        Member(int size, int alignment, boolean forcedAlignment) {
            this.size = size;
            this.alignment = alignment;
            this.forcedAlignment = forcedAlignment;
        }

        public int getSize() {
            return size;
        }

        public int getAlignment() {
            return alignment;
        }

        public int getAlignment(int packAlignment) {
            return forcedAlignment ? alignment : min(alignment, packAlignment);
        }
    }

    protected static class Layout extends Member {
        final Member[] members;

        Layout(int size, int alignment, boolean forceAlignment, Member[] members) {
            super(size, alignment, forceAlignment);
            this.members = members;
        }

        public int offsetof(int member) {
            return members[member].offset;
        }
    }

    protected static Member __padding(int num, boolean condition) {
        return __padding(num, 1, condition);
    }

    protected static Member __padding(int num, int size, boolean condition) {
        return __member(condition ? num * size : 0, size);
    }

    protected static Member __member(int size) {
        return __member(size, size);
    }

    protected static Member __member(int size, int alignment) {
        return __member(size, alignment, false);
    }

    protected static Member __member(int size, int alignment, boolean forceAlignment) {
        return new Member(size, alignment, forceAlignment);
    }

    protected static Member __array(int size, int length) {
        return __array(size, size, length);
    }
    protected static Member __array(int size, int alignment, int length) {
        return new Member(size * length, alignment, false);
    }
    protected static Member __array(int size, int alignment, boolean forceAlignment, int length) {
        return new Member(size * length, alignment, forceAlignment);
    }

    protected static Layout __union(Member... members) { return __union(DEFAULT_PACK_ALIGNMENT, DEFAULT_ALIGN_AS, members); }
    protected static Layout __union(int packAlignment, int alignas, Member... members) {
        List<Member> union = new ArrayList<>(members.length);

        int size      = 0;
        int alignment = alignas;
        for (Member m : members) {
            size = max(size, m.size);
            alignment = max(alignment, m.getAlignment(packAlignment));

            m.offset = 0;
            union.add(m);
            if (m instanceof Layout) {
                addNestedMembers(m, union, 0);
            }
        }

        return new Layout(size, alignment, alignas != 0, union.toArray(new Member[0]));
    }

    protected static Layout __struct(Member... members) { return __struct(DEFAULT_PACK_ALIGNMENT, DEFAULT_ALIGN_AS, members); }
    protected static Layout __struct(int packAlignment, int alignas, Member... members) {
        List<Member> struct = new ArrayList<>(members.length);

        int size      = 0;
        int alignment = alignas;
        for (Member m : members) {
            int memberAlignment = m.getAlignment(packAlignment);

            m.offset = align(size, memberAlignment);

            size = m.offset + m.size;
            alignment = max(alignment, memberAlignment);

            struct.add(m);
            if (m instanceof Layout) {
                addNestedMembers(m, struct, m.offset);
            }
        }

        // tail padding
        size = align(size, alignment);

        return new Layout(size, alignment, alignas != 0, struct.toArray(new Member[0]));
    }

    private static void addNestedMembers(Member nested, List<Member> members, int offset) {
        Layout layout = (Layout)nested;

        for (Member m : layout.members) {
            m.offset += offset;
            members.add(m);
        }
    }

    private static int align(int offset, int alignment) {
        return ((offset - 1) | (alignment - 1)) + 1;
    }

}