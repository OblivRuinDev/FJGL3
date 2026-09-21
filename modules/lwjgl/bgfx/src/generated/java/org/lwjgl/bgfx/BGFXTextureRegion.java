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
 * struct bgfx_texture_region_t {
 *     bgfx_texture_handle_t handle;
 *     uint8_t mip;
 *     uint16_t x;
 *     uint16_t y;
 *     uint16_t z;
 *     uint16_t width;
 *     uint16_t height;
 *     uint16_t depth;
 * }}</pre>
 */
@NativeType("struct bgfx_texture_region_t")
public class BGFXTextureRegion extends Struct<BGFXTextureRegion> implements NativeResource {

    /** The struct size in bytes. */
    public static final int SIZEOF;

    /** The struct alignment in bytes. */
    public static final int ALIGNOF;

    /** The struct member offsets. */
    public static final int
        HANDLE,
        MIP,
        X,
        Y,
        Z,
        WIDTH,
        HEIGHT,
        DEPTH;

    static {
        Layout layout = __struct(
            __member(2),
            __member(1),
            __member(2),
            __member(2),
            __member(2),
            __member(2),
            __member(2),
            __member(2)
        );

        SIZEOF = layout.getSize();
        ALIGNOF = layout.getAlignment();

        HANDLE = layout.offsetof(0);
        MIP = layout.offsetof(1);
        X = layout.offsetof(2);
        Y = layout.offsetof(3);
        Z = layout.offsetof(4);
        WIDTH = layout.offsetof(5);
        HEIGHT = layout.offsetof(6);
        DEPTH = layout.offsetof(7);
    }

    protected BGFXTextureRegion(long address, @Nullable ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected BGFXTextureRegion create(long address, @Nullable ByteBuffer container) {
        return new BGFXTextureRegion(address, container);
    }

    /**
     * Creates a {@code BGFXTextureRegion} instance at the current position of the specified {@link ByteBuffer} container. Changes to the buffer's content will be
     * visible to the struct instance and vice versa.
     *
     * <p>The created instance holds a strong reference to the container object.</p>
     */
    public BGFXTextureRegion(ByteBuffer container) {
        super(memAddress(container), __checkContainer(container, SIZEOF));
    }

    @Override
    public int sizeof() { return SIZEOF; }

    /** @return the value of the {@code handle} field. */
    @NativeType("bgfx_texture_handle_t")
    public short handle() { return nhandle(address()); }
    /** @return the value of the {@code mip} field. */
    @NativeType("uint8_t")
    public byte mip() { return nmip(address()); }
    /** @return the value of the {@code x} field. */
    @NativeType("uint16_t")
    public short x() { return nx(address()); }
    /** @return the value of the {@code y} field. */
    @NativeType("uint16_t")
    public short y() { return ny(address()); }
    /** @return the value of the {@code z} field. */
    @NativeType("uint16_t")
    public short z() { return nz(address()); }
    /** @return the value of the {@code width} field. */
    @NativeType("uint16_t")
    public short width() { return nwidth(address()); }
    /** @return the value of the {@code height} field. */
    @NativeType("uint16_t")
    public short height() { return nheight(address()); }
    /** @return the value of the {@code depth} field. */
    @NativeType("uint16_t")
    public short depth() { return ndepth(address()); }

    /** Sets the specified value to the {@code handle} field. */
    public BGFXTextureRegion handle(@NativeType("bgfx_texture_handle_t") short value) { nhandle(address(), value); return this; }
    /** Sets the specified value to the {@code mip} field. */
    public BGFXTextureRegion mip(@NativeType("uint8_t") byte value) { nmip(address(), value); return this; }
    /** Sets the specified value to the {@code x} field. */
    public BGFXTextureRegion x(@NativeType("uint16_t") short value) { nx(address(), value); return this; }
    /** Sets the specified value to the {@code y} field. */
    public BGFXTextureRegion y(@NativeType("uint16_t") short value) { ny(address(), value); return this; }
    /** Sets the specified value to the {@code z} field. */
    public BGFXTextureRegion z(@NativeType("uint16_t") short value) { nz(address(), value); return this; }
    /** Sets the specified value to the {@code width} field. */
    public BGFXTextureRegion width(@NativeType("uint16_t") short value) { nwidth(address(), value); return this; }
    /** Sets the specified value to the {@code height} field. */
    public BGFXTextureRegion height(@NativeType("uint16_t") short value) { nheight(address(), value); return this; }
    /** Sets the specified value to the {@code depth} field. */
    public BGFXTextureRegion depth(@NativeType("uint16_t") short value) { ndepth(address(), value); return this; }

    /** Initializes this struct with the specified values. */
    public BGFXTextureRegion set(
        short handle,
        byte mip,
        short x,
        short y,
        short z,
        short width,
        short height,
        short depth
    ) {
        handle(handle);
        mip(mip);
        x(x);
        y(y);
        z(z);
        width(width);
        height(height);
        depth(depth);

        return this;
    }

    /**
     * Copies the specified struct data to this struct.
     *
     * @param src the source struct
     *
     * @return this struct
     */
    public BGFXTextureRegion set(BGFXTextureRegion src) {
        memCopy(src.address(), address(), SIZEOF);
        return this;
    }

    // -----------------------------------

    /** Returns a new {@code BGFXTextureRegion} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed. */
    public static BGFXTextureRegion malloc() {
        return new BGFXTextureRegion(nmemAllocChecked(SIZEOF), null);
    }

    /** Returns a new {@code BGFXTextureRegion} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed. */
    public static BGFXTextureRegion calloc() {
        return new BGFXTextureRegion(nmemCallocChecked(1, SIZEOF), null);
    }

    /** Returns a new {@code BGFXTextureRegion} instance allocated with {@link BufferUtils}. */
    public static BGFXTextureRegion create() {
        ByteBuffer container = BufferUtils.createByteBuffer(SIZEOF);
        return new BGFXTextureRegion(memAddress(container), container);
    }

    /** Returns a new {@code BGFXTextureRegion} instance for the specified memory address. */
    public static BGFXTextureRegion create(long address) {
        return new BGFXTextureRegion(address, null);
    }

    /** Like {@link #create(long) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static @Nullable BGFXTextureRegion createSafe(long address) {
        return address == NULL ? null : new BGFXTextureRegion(address, null);
    }

    /**
     * Returns a new {@link BGFXTextureRegion.Buffer} instance allocated with {@link MemoryUtil#memAlloc memAlloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer malloc(int capacity) {
        return new Buffer(nmemAllocChecked(__checkMalloc(capacity, SIZEOF)), capacity);
    }

    /**
     * Returns a new {@link BGFXTextureRegion.Buffer} instance allocated with {@link MemoryUtil#memCalloc memCalloc}. The instance must be explicitly freed.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer calloc(int capacity) {
        return new Buffer(nmemCallocChecked(capacity, SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXTextureRegion.Buffer} instance allocated with {@link BufferUtils}.
     *
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer create(int capacity) {
        ByteBuffer container = __create(capacity, SIZEOF);
        return new Buffer(memAddress(container), container, -1, 0, capacity, capacity);
    }

    /**
     * Create a {@link BGFXTextureRegion.Buffer} instance at the specified memory.
     *
     * @param address  the memory address
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer create(long address, int capacity) {
        return new Buffer(address, capacity);
    }

    /** Like {@link #create(long, int) create}, but returns {@code null} if {@code address} is {@code NULL}. */
    public static BGFXTextureRegion.@Nullable Buffer createSafe(long address, int capacity) {
        return address == NULL ? null : new Buffer(address, capacity);
    }

    /**
     * Returns a new {@code BGFXTextureRegion} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXTextureRegion malloc(MemoryStack stack) {
        return new BGFXTextureRegion(stack.nmalloc(ALIGNOF, SIZEOF), null);
    }

    /**
     * Returns a new {@code BGFXTextureRegion} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack the stack from which to allocate
     */
    public static BGFXTextureRegion calloc(MemoryStack stack) {
        return new BGFXTextureRegion(stack.ncalloc(ALIGNOF, 1, SIZEOF), null);
    }

    /**
     * Returns a new {@link BGFXTextureRegion.Buffer} instance allocated on the specified {@link MemoryStack}.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer malloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.nmalloc(ALIGNOF, capacity * SIZEOF), capacity);
    }

    /**
     * Returns a new {@link BGFXTextureRegion.Buffer} instance allocated on the specified {@link MemoryStack} and initializes all its bits to zero.
     *
     * @param stack    the stack from which to allocate
     * @param capacity the buffer capacity
     */
    public static BGFXTextureRegion.Buffer calloc(int capacity, MemoryStack stack) {
        return new Buffer(stack.ncalloc(ALIGNOF, capacity, SIZEOF), capacity);
    }

    // -----------------------------------

    /** Unsafe version of {@link #handle}. */
    public static short nhandle(long struct) { return memGetShort(struct + BGFXTextureRegion.HANDLE); }
    /** Unsafe version of {@link #mip}. */
    public static byte nmip(long struct) { return memGetByte(struct + BGFXTextureRegion.MIP); }
    /** Unsafe version of {@link #x}. */
    public static short nx(long struct) { return memGetShort(struct + BGFXTextureRegion.X); }
    /** Unsafe version of {@link #y}. */
    public static short ny(long struct) { return memGetShort(struct + BGFXTextureRegion.Y); }
    /** Unsafe version of {@link #z}. */
    public static short nz(long struct) { return memGetShort(struct + BGFXTextureRegion.Z); }
    /** Unsafe version of {@link #width}. */
    public static short nwidth(long struct) { return memGetShort(struct + BGFXTextureRegion.WIDTH); }
    /** Unsafe version of {@link #height}. */
    public static short nheight(long struct) { return memGetShort(struct + BGFXTextureRegion.HEIGHT); }
    /** Unsafe version of {@link #depth}. */
    public static short ndepth(long struct) { return memGetShort(struct + BGFXTextureRegion.DEPTH); }

    /** Unsafe version of {@link #handle(short) handle}. */
    public static void nhandle(long struct, short value) { memPutShort(struct + BGFXTextureRegion.HANDLE, value); }
    /** Unsafe version of {@link #mip(byte) mip}. */
    public static void nmip(long struct, byte value) { memPutByte(struct + BGFXTextureRegion.MIP, value); }
    /** Unsafe version of {@link #x(short) x}. */
    public static void nx(long struct, short value) { memPutShort(struct + BGFXTextureRegion.X, value); }
    /** Unsafe version of {@link #y(short) y}. */
    public static void ny(long struct, short value) { memPutShort(struct + BGFXTextureRegion.Y, value); }
    /** Unsafe version of {@link #z(short) z}. */
    public static void nz(long struct, short value) { memPutShort(struct + BGFXTextureRegion.Z, value); }
    /** Unsafe version of {@link #width(short) width}. */
    public static void nwidth(long struct, short value) { memPutShort(struct + BGFXTextureRegion.WIDTH, value); }
    /** Unsafe version of {@link #height(short) height}. */
    public static void nheight(long struct, short value) { memPutShort(struct + BGFXTextureRegion.HEIGHT, value); }
    /** Unsafe version of {@link #depth(short) depth}. */
    public static void ndepth(long struct, short value) { memPutShort(struct + BGFXTextureRegion.DEPTH, value); }

    // -----------------------------------

    /** An array of {@link BGFXTextureRegion} structs. */
    public static class Buffer extends StructBuffer<BGFXTextureRegion, Buffer> implements NativeResource {

        private static final BGFXTextureRegion ELEMENT_FACTORY = BGFXTextureRegion.create(-1L);

        /**
         * Creates a new {@code BGFXTextureRegion.Buffer} instance backed by the specified container.
         *
         * <p>Changes to the container's content will be visible to the struct buffer instance and vice versa. The two buffers' position, limit, and mark values
         * will be independent. The new buffer's position will be zero, its capacity and its limit will be the number of bytes remaining in this buffer divided
         * by {@link BGFXTextureRegion#SIZEOF}, and its mark will be undefined.</p>
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
        protected BGFXTextureRegion getElementFactory() {
            return ELEMENT_FACTORY;
        }

        /** @return the value of the {@code handle} field. */
        @NativeType("bgfx_texture_handle_t")
        public short handle() { return BGFXTextureRegion.nhandle(address()); }
        /** @return the value of the {@code mip} field. */
        @NativeType("uint8_t")
        public byte mip() { return BGFXTextureRegion.nmip(address()); }
        /** @return the value of the {@code x} field. */
        @NativeType("uint16_t")
        public short x() { return BGFXTextureRegion.nx(address()); }
        /** @return the value of the {@code y} field. */
        @NativeType("uint16_t")
        public short y() { return BGFXTextureRegion.ny(address()); }
        /** @return the value of the {@code z} field. */
        @NativeType("uint16_t")
        public short z() { return BGFXTextureRegion.nz(address()); }
        /** @return the value of the {@code width} field. */
        @NativeType("uint16_t")
        public short width() { return BGFXTextureRegion.nwidth(address()); }
        /** @return the value of the {@code height} field. */
        @NativeType("uint16_t")
        public short height() { return BGFXTextureRegion.nheight(address()); }
        /** @return the value of the {@code depth} field. */
        @NativeType("uint16_t")
        public short depth() { return BGFXTextureRegion.ndepth(address()); }

        /** Sets the specified value to the {@code handle} field. */
        public BGFXTextureRegion.Buffer handle(@NativeType("bgfx_texture_handle_t") short value) { BGFXTextureRegion.nhandle(address(), value); return this; }
        /** Sets the specified value to the {@code mip} field. */
        public BGFXTextureRegion.Buffer mip(@NativeType("uint8_t") byte value) { BGFXTextureRegion.nmip(address(), value); return this; }
        /** Sets the specified value to the {@code x} field. */
        public BGFXTextureRegion.Buffer x(@NativeType("uint16_t") short value) { BGFXTextureRegion.nx(address(), value); return this; }
        /** Sets the specified value to the {@code y} field. */
        public BGFXTextureRegion.Buffer y(@NativeType("uint16_t") short value) { BGFXTextureRegion.ny(address(), value); return this; }
        /** Sets the specified value to the {@code z} field. */
        public BGFXTextureRegion.Buffer z(@NativeType("uint16_t") short value) { BGFXTextureRegion.nz(address(), value); return this; }
        /** Sets the specified value to the {@code width} field. */
        public BGFXTextureRegion.Buffer width(@NativeType("uint16_t") short value) { BGFXTextureRegion.nwidth(address(), value); return this; }
        /** Sets the specified value to the {@code height} field. */
        public BGFXTextureRegion.Buffer height(@NativeType("uint16_t") short value) { BGFXTextureRegion.nheight(address(), value); return this; }
        /** Sets the specified value to the {@code depth} field. */
        public BGFXTextureRegion.Buffer depth(@NativeType("uint16_t") short value) { BGFXTextureRegion.ndepth(address(), value); return this; }

    }

}