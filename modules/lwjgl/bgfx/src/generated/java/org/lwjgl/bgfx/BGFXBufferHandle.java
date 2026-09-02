/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 * MACHINE GENERATED FILE, DO NOT EDIT
 */
package org.lwjgl.bgfx;

import org.jspecify.annotations.*;

import java.nio.*;

import org.lwjgl.*;
import org.lwjgl.system.*;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.MemoryStack.*;

/**
 * <pre>{@code
 * struct bgfx_buffer_handle_t {
 *     uint16_t idx;
 *     uint16_t type;
 * }}</pre>
 */
@NativeType("struct bgfx_buffer_handle_t")
public class BGFXBufferHandle extends Struct<BGFXBufferHandle> implements NativeResource {

    /** The struct size in bytes. */
    public static final int SIZEOF;

    /** The struct alignment in bytes. */
    public static final int ALIGNOF;

    /** The struct member offsets. */
    public static final int
        IDX,
        TYPE;

    static {
        Layout layout = __struct(
            __member(2),
            __member(2)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        IDX = layout.offsetof(0);
        TYPE = layout.offsetof(1);
    }

    protected BGFXBufferHandle(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected BGFXBufferHandle create(long address, @Nullable ByteBuffer container) {
        return new BGFXBufferHandle(address, container);
    }

    /**
     * Creates a {@code BGFXBufferHandle} instance at the current position of the specified {@link ByteBuffer} container. Changes to the buffer's content will be
     * visible to the struct instance and vice versa.
     *
     * <p>The created instance holds a strong reference to the container object.</p>
     */
    public BGFXBufferHandle(ByteBuffer container) {
        super(memAddress(container), __checkContainer(container, SIZEOF));
    }

    @Override
    public int sizeof() { return SIZEOF; }

    /** @return the value of the {@code idx} field. */
    @NativeType("uint16_t")
    public short idx() { return nidx(address()); }
    /** @return the value of the {@code type} field. */
    @NativeType("uint16_t")
    public short type() { return ntype(address()); }

    /** Sets the specified value to the {@code idx} field. */
    public BGFXBufferHandle idx(@NativeType("uint16_t") short value) { nidx(address(), value); return this; }
    /** Sets the specified value to the {@code type} field. */
    public BGFXBufferHandle type(@NativeType("uint16_t") short value) { ntype(address(), value); return this; }

    /** Initializes this struct with the specified values. */
    public BGFXBufferHandle set(
        short idx,
        short type
    ) {
        idx(idx);
        type(type);

        return this;
    }

    /**
     * Copies the specified struct data to this struct.
     *
     * @param src the source struct
     *
     * @return this struct
     */
    public BGFXBufferHandle set(BGFXBufferHandle src) {
        memCopy(src.address(), address(), SIZEOF);
        return this;
    }

    // -----------------------------------

    /** Returns a new {@code BGFXBufferHandle} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed. */
    public static BGFXBufferHandle malloc() {
        return new BGFXBufferHandle(nmemAllocChecked(SIZEOF), null);
    }

    /** Returns a new {@code BGFXBufferHandle} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed. */
    public static BGFXBufferHandle calloc() {
        return new BGFXBufferHandle(nmemCallocChecked(1, SIZEOF), null);
    }

    /** Returns a new {@code BGFXBufferHandle} instance allocated with {@link BufferUtils}. */
    public static BGFXBufferHandle create() {
        ByteBuffer container = BufferUtils.createByteBuffer(SIZEOF);
        return new BGFXBufferHandle(memAddress(container), container);
    }

    /** Returns a new {@code BGFXBufferHandle} instance for the specified memory address. */
    public static BGFXBufferHandle create(long address) {
        return new BGFXBufferHandle(address, null);
    }

    /** Like {@link #create(long) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static @Nullable BGFXBufferHandle createSafe(long address) {
        return address == NULL ? null : new BGFXBufferHandle(address, null);
    }

    /**
     * Returns a new {@link BGFXBufferHandle.Buffer} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer malloc(int capacity) {
        return new Buffer(nmemAllocChecked(__checkMalloc(capacity, SIZEOF)), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferHandle.Buffer} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer calloc(int capacity) {
        return new Buffer(nmemCallocChecked(capacity, SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferHandle.Buffer} instance allocated with {@link BufferUtils}.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer create(int capacity) {
        ByteBuffer container = __create(capacity, SIZEOF);
        return new Buffer(memAddress(container), container, -1, 0, capacity, capacity);
    }

    /**
     * Create a {@link BGFXBufferHandle.Buffer} instance at the specified memory.
     *
     * @param address  the memory address
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer create(long address, int capacity) {
        return new Buffer(address, capacity);
    }

    /** Like {@link #create(long, int) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static BGFXBufferHandle.@Nullable Buffer createSafe(long address, int capacity) {
        return address == NULL ? null : new Buffer(address, capacity);
    }

    /**
     * Returns a new {@code BGFXBufferHandle} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXBufferHandle malloc(MemoryStack stack) {
        return new BGFXBufferHandle(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    /**
     * Returns a new {@code BGFXBufferHandle} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXBufferHandle calloc(MemoryStack stack) {
        return new BGFXBufferHandle(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    /**
     * Returns a new {@link BGFXBufferHandle.Buffer} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer malloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.nmalloc(ALIGNOF, capacity * SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferHandle.Buffer} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXBufferHandle.Buffer calloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.ncalloc(ALIGNOF, capacity, SIZEOF), capacity);
    }

    // -----------------------------------

    /** Unsafe version of {@link #idx}. */
    public static short nidx(long struct) { return memGetShort(struct + BGFXBufferHandle.IDX); }
    /** Unsafe version of {@link #type}. */
    public static short ntype(long struct) { return memGetShort(struct + BGFXBufferHandle.TYPE); }

    /** Unsafe version of {@link #idx(short) idx}. */
    public static void nidx(long struct, short value) { memPutShort(struct + BGFXBufferHandle.IDX, value); }
    /** Unsafe version of {@link #type(short) type}. */
    public static void ntype(long struct, short value) { memPutShort(struct + BGFXBufferHandle.TYPE, value); }

    // -----------------------------------

    /** An array of {@link BGFXBufferHandle} structs. */
    public static class Buffer extends StructBuffer<BGFXBufferHandle, Buffer> implements NativeResource {

        private static final BGFXBufferHandle ELEMENT_FACTORY = BGFXBufferHandle.create(-1L);

        /**
         * Creates a new {@code BGFXBufferHandle.Buffer} instance backed by the specified container.
         *
         * <p>Changes to the container's content will be visible to the struct buffer instance and vice versa. The two buffers' position, limit, and mark values
         * will be independent. The new buffer's position will be zero, its capacity and its limit will be the number of bytes remaining in this buffer divided
         * by {@link BGFXBufferHandle#SIZEOF}, and its mark will be undefined.</p>
         *
         * <p>The created buffer instance holds a strong reference to the container object.</p>
         */
        public Buffer(ByteBuffer container) {
            super(container, container.remaining() / SIZEOF);
        }

        public Buffer(long address, int cap) {
            super(address, null, -1, 0, cap, cap);
        }

        Buffer(long address, @Nullable ByteBuffer container, int mark, int pos, int lim, int cap) {
            super(address, container, mark, pos, lim, cap);
        }

        @Override
        protected Buffer self() {
            return this;
        }

        @Override
        protected Buffer create(long address, @Nullable ByteBuffer container, int mark, int position, int limit, int capacity) {
            return new Buffer(address, container, mark, position, limit, capacity);
        }

        @Override
        protected BGFXBufferHandle getElementFactory() {
            return ELEMENT_FACTORY;
        }

        /** @return the value of the {@code idx} field. */
        @NativeType("uint16_t")
        public short idx() { return BGFXBufferHandle.nidx(address()); }
        /** @return the value of the {@code type} field. */
        @NativeType("uint16_t")
        public short type() { return BGFXBufferHandle.ntype(address()); }

        /** Sets the specified value to the {@code idx} field. */
        public BGFXBufferHandle.Buffer idx(@NativeType("uint16_t") short value) { BGFXBufferHandle.nidx(address(), value); return this; }
        /** Sets the specified value to the {@code type} field. */
        public BGFXBufferHandle.Buffer type(@NativeType("uint16_t") short value) { BGFXBufferHandle.ntype(address(), value); return this; }

    }

}