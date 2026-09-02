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
 * <pre><code>
 * struct bgfx_buffer_region_t {
 *     {@link BGFXBufferHandle bgfx_buffer_handle_t} handle;
 *     uint32_t offset;
 *     uint32_t size;
 *     uint32_t rowPitch;
 *     uint32_t slicePitch;
 * }</code></pre>
 */
@NativeType("struct bgfx_buffer_region_t")
public class BGFXBufferRegion extends Struct<BGFXBufferRegion> implements NativeResource {

    /** The struct size in bytes. */
    public static final int SIZEOF;

    /** The struct alignment in bytes. */
    public static final int ALIGNOF;

    /** The struct member offsets. */
    public static final int
        HANDLE,
        OFFSET,
        SIZE,
        ROWPITCH,
        SLICEPITCH;

    static {
        BGFXBufferHandle.createSafe(NULL);

        Layout layout = __struct(
            __member(BGFXBufferHandle.SIZEOF, BGFXBufferHandle.ALIGNOF),
            __member(4),
            __member(4),
            __member(4),
            __member(4)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        HANDLE = layout.offsetof(0);
        OFFSET = layout.offsetof(1);
        SIZE = layout.offsetof(2);
        ROWPITCH = layout.offsetof(3);
        SLICEPITCH = layout.offsetof(4);
    }

    protected BGFXBufferRegion(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected BGFXBufferRegion create(long address, @Nullable ByteBuffer container) {
        return new BGFXBufferRegion(address, container);
    }

    /**
     * Creates a {@code BGFXBufferRegion} instance at the current position of the specified {@link ByteBuffer} container. Changes to the buffer's content will be
     * visible to the struct instance and vice versa.
     *
     * <p>The created instance holds a strong reference to the container object.</p>
     */
    public BGFXBufferRegion(ByteBuffer container) {
        super(memAddress(container), __checkContainer(container, SIZEOF));
    }

    @Override
    public int sizeof() { return SIZEOF; }

    /** @return a {@link BGFXBufferHandle} view of the {@code handle} field. */
    @NativeType("bgfx_buffer_handle_t")
    public BGFXBufferHandle handle() { return nhandle(address()); }
    /** @return the value of the {@code offset} field. */
    @NativeType("uint32_t")
    public int offset() { return noffset(address()); }
    /** @return the value of the {@code size} field. */
    @NativeType("uint32_t")
    public int size() { return nsize(address()); }
    /** @return the value of the {@code rowPitch} field. */
    @NativeType("uint32_t")
    public int rowPitch() { return nrowPitch(address()); }
    /** @return the value of the {@code slicePitch} field. */
    @NativeType("uint32_t")
    public int slicePitch() { return nslicePitch(address()); }

    /** Copies the specified {@link BGFXBufferHandle} to the {@code handle} field. */
    public BGFXBufferRegion handle(@NativeType("bgfx_buffer_handle_t") BGFXBufferHandle value) { nhandle(address(), value); return this; }
    /** Passes the {@code handle} field to the specified {@link java.util.function.Consumer Consumer}. */
    public BGFXBufferRegion handle(java.util.function.Consumer<BGFXBufferHandle> consumer) { consumer.accept(handle()); return this; }
    /** Sets the specified value to the {@code offset} field. */
    public BGFXBufferRegion offset(@NativeType("uint32_t") int value) { noffset(address(), value); return this; }
    /** Sets the specified value to the {@code size} field. */
    public BGFXBufferRegion size(@NativeType("uint32_t") int value) { nsize(address(), value); return this; }
    /** Sets the specified value to the {@code rowPitch} field. */
    public BGFXBufferRegion rowPitch(@NativeType("uint32_t") int value) { nrowPitch(address(), value); return this; }
    /** Sets the specified value to the {@code slicePitch} field. */
    public BGFXBufferRegion slicePitch(@NativeType("uint32_t") int value) { nslicePitch(address(), value); return this; }

    /** Initializes this struct with the specified values. */
    public BGFXBufferRegion set(
        BGFXBufferHandle handle,
        int offset,
        int size,
        int rowPitch,
        int slicePitch
    ) {
        handle(handle);
        offset(offset);
        size(size);
        rowPitch(rowPitch);
        slicePitch(slicePitch);

        return this;
    }

    /**
     * Copies the specified struct data to this struct.
     *
     * @param src the source struct
     *
     * @return this struct
     */
    public BGFXBufferRegion set(BGFXBufferRegion src) {
        memCopy(src.address(), address(), SIZEOF);
        return this;
    }

    // -----------------------------------

    /** Returns a new {@code BGFXBufferRegion} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed. */
    public static BGFXBufferRegion malloc() {
        return new BGFXBufferRegion(nmemAllocChecked(SIZEOF), null);
    }

    /** Returns a new {@code BGFXBufferRegion} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed. */
    public static BGFXBufferRegion calloc() {
        return new BGFXBufferRegion(nmemCallocChecked(1, SIZEOF), null);
    }

    /** Returns a new {@code BGFXBufferRegion} instance allocated with {@link BufferUtils}. */
    public static BGFXBufferRegion create() {
        ByteBuffer container = BufferUtils.createByteBuffer(SIZEOF);
        return new BGFXBufferRegion(memAddress(container), container);
    }

    /** Returns a new {@code BGFXBufferRegion} instance for the specified memory address. */
    public static BGFXBufferRegion create(long address) {
        return new BGFXBufferRegion(address, null);
    }

    /** Like {@link #create(long) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static @Nullable BGFXBufferRegion createSafe(long address) {
        return address == NULL ? null : new BGFXBufferRegion(address, null);
    }

    /**
     * Returns a new {@link BGFXBufferRegion.Buffer} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer malloc(int capacity) {
        return new Buffer(nmemAllocChecked(__checkMalloc(capacity, SIZEOF)), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferRegion.Buffer} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer calloc(int capacity) {
        return new Buffer(nmemCallocChecked(capacity, SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferRegion.Buffer} instance allocated with {@link BufferUtils}.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer create(int capacity) {
        ByteBuffer container = __create(capacity, SIZEOF);
        return new Buffer(memAddress(container), container, -1, 0, capacity, capacity);
    }

    /**
     * Create a {@link BGFXBufferRegion.Buffer} instance at the specified memory.
     *
     * @param address  the memory address
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer create(long address, int capacity) {
        return new Buffer(address, capacity);
    }

    /** Like {@link #create(long, int) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static BGFXBufferRegion.@Nullable Buffer createSafe(long address, int capacity) {
        return address == NULL ? null : new Buffer(address, capacity);
    }

    /**
     * Returns a new {@code BGFXBufferRegion} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXBufferRegion malloc(MemoryStack stack) {
        return new BGFXBufferRegion(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    /**
     * Returns a new {@code BGFXBufferRegion} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXBufferRegion calloc(MemoryStack stack) {
        return new BGFXBufferRegion(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    /**
     * Returns a new {@link BGFXBufferRegion.Buffer} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer malloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.nmalloc(ALIGNOF, capacity * SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXBufferRegion.Buffer} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXBufferRegion.Buffer calloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.ncalloc(ALIGNOF, capacity, SIZEOF), capacity);
    }

    // -----------------------------------

    /** Unsafe version of {@link #handle}. */
    public static BGFXBufferHandle nhandle(long struct) { return BGFXBufferHandle.create(struct + BGFXBufferRegion.HANDLE); }
    /** Unsafe version of {@link #offset}. */
    public static int noffset(long struct) { return memGetInt(struct + BGFXBufferRegion.OFFSET); }
    /** Unsafe version of {@link #size}. */
    public static int nsize(long struct) { return memGetInt(struct + BGFXBufferRegion.SIZE); }
    /** Unsafe version of {@link #rowPitch}. */
    public static int nrowPitch(long struct) { return memGetInt(struct + BGFXBufferRegion.ROWPITCH); }
    /** Unsafe version of {@link #slicePitch}. */
    public static int nslicePitch(long struct) { return memGetInt(struct + BGFXBufferRegion.SLICEPITCH); }

    /** Unsafe version of {@link #handle(BGFXBufferHandle) handle}. */
    public static void nhandle(long struct, BGFXBufferHandle value) { memCopy(value.address(), struct + BGFXBufferRegion.HANDLE, BGFXBufferHandle.SIZEOF); }
    /** Unsafe version of {@link #offset(int) offset}. */
    public static void noffset(long struct, int value) { memPutInt(struct + BGFXBufferRegion.OFFSET, value); }
    /** Unsafe version of {@link #size(int) size}. */
    public static void nsize(long struct, int value) { memPutInt(struct + BGFXBufferRegion.SIZE, value); }
    /** Unsafe version of {@link #rowPitch(int) rowPitch}. */
    public static void nrowPitch(long struct, int value) { memPutInt(struct + BGFXBufferRegion.ROWPITCH, value); }
    /** Unsafe version of {@link #slicePitch(int) slicePitch}. */
    public static void nslicePitch(long struct, int value) { memPutInt(struct + BGFXBufferRegion.SLICEPITCH, value); }

    // -----------------------------------

    /** An array of {@link BGFXBufferRegion} structs. */
    public static class Buffer extends StructBuffer<BGFXBufferRegion, Buffer> implements NativeResource {

        private static final BGFXBufferRegion ELEMENT_FACTORY = BGFXBufferRegion.create(-1L);

        /**
         * Creates a new {@code BGFXBufferRegion.Buffer} instance backed by the specified container.
         *
         * <p>Changes to the container's content will be visible to the struct buffer instance and vice versa. The two buffers' position, limit, and mark values
         * will be independent. The new buffer's position will be zero, its capacity and its limit will be the number of bytes remaining in this buffer divided
         * by {@link BGFXBufferRegion#SIZEOF}, and its mark will be undefined.</p>
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
        protected BGFXBufferRegion getElementFactory() {
            return ELEMENT_FACTORY;
        }

        /** @return a {@link BGFXBufferHandle} view of the {@code handle} field. */
        @NativeType("bgfx_buffer_handle_t")
        public BGFXBufferHandle handle() { return BGFXBufferRegion.nhandle(address()); }
        /** @return the value of the {@code offset} field. */
        @NativeType("uint32_t")
        public int offset() { return BGFXBufferRegion.noffset(address()); }
        /** @return the value of the {@code size} field. */
        @NativeType("uint32_t")
        public int size() { return BGFXBufferRegion.nsize(address()); }
        /** @return the value of the {@code rowPitch} field. */
        @NativeType("uint32_t")
        public int rowPitch() { return BGFXBufferRegion.nrowPitch(address()); }
        /** @return the value of the {@code slicePitch} field. */
        @NativeType("uint32_t")
        public int slicePitch() { return BGFXBufferRegion.nslicePitch(address()); }

        /** Copies the specified {@link BGFXBufferHandle} to the {@code handle} field. */
        public BGFXBufferRegion.Buffer handle(@NativeType("bgfx_buffer_handle_t") BGFXBufferHandle value) { BGFXBufferRegion.nhandle(address(), value); return this; }
        /** Passes the {@code handle} field to the specified {@link java.util.function.Consumer Consumer}. */
        public BGFXBufferRegion.Buffer handle(java.util.function.Consumer<BGFXBufferHandle> consumer) { consumer.accept(handle()); return this; }
        /** Sets the specified value to the {@code offset} field. */
        public BGFXBufferRegion.Buffer offset(@NativeType("uint32_t") int value) { BGFXBufferRegion.noffset(address(), value); return this; }
        /** Sets the specified value to the {@code size} field. */
        public BGFXBufferRegion.Buffer size(@NativeType("uint32_t") int value) { BGFXBufferRegion.nsize(address(), value); return this; }
        /** Sets the specified value to the {@code rowPitch} field. */
        public BGFXBufferRegion.Buffer rowPitch(@NativeType("uint32_t") int value) { BGFXBufferRegion.nrowPitch(address(), value); return this; }
        /** Sets the specified value to the {@code slicePitch} field. */
        public BGFXBufferRegion.Buffer slicePitch(@NativeType("uint32_t") int value) { BGFXBufferRegion.nslicePitch(address(), value); return this; }

    }

}