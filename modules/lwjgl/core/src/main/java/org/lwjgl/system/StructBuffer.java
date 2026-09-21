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

import jdk.internal.vm.annotation.*;
import org.jspecify.annotations.*;

import java.nio.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.MemoryUtil.*;

/** Base class of struct custom buffers. */
@SuppressWarnings("unchecked")
public abstract class StructBuffer<T extends Struct<T>, SELF extends StructBuffer<T, SELF>> extends CustomBuffer<SELF> implements Iterable<T> {

    protected StructBuffer(ByteBuffer container, int remaining) {
        super(memAddress(container), container, -1, 0, remaining, remaining);
    }

    protected StructBuffer(long address, @Nullable ByteBuffer container, int mark, int position, int limit, int capacity) {
        super(address, container, mark, position, limit, capacity);
    }

    @Override
    public abstract int sizeof();
    @ForceInline
    protected abstract Class<?> getElementClass();

    /**
     * Relative <i>get</i> method. Reads the struct at this buffer's current position, and then increments the position.
     *
     * <p>The struct instance returned is a view of the buffer at the current position. Changes to this buffer's content will be visible in the struct instance
     * and vice versa.</p>
     *
     * @return the struct at the buffer's current position
     *
     * @throws java.nio.BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    @ForceInline
    public T get() {
        return Struct.create((Class<T>)getElementClass(), address + Integer.toUnsignedLong(nextGetIndex()) * sizeof(), container);
    }

    /**
     * Relative <i>get</i> method. Reads the struct data at this buffer's current position into the specified struct, and then increments the position.
     *
     * @return the struct at the buffer's current position
     *
     * @throws java.nio.BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    public SELF get(T value) {
        int sizeof = sizeof();
        memCopy(address + Integer.toUnsignedLong(nextGetIndex()) * sizeof, value.address(), sizeof);
        return (SELF) this;
    }

    /**
     * Relative <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p>Writes the specified struct into this buffer at the current position, and then increments the position.</p>
     *
     * @param value the struct to be written
     *
     * @return This buffer
     *
     * @throws java.nio.BufferOverflowException If this buffer's current position is not smaller than its limit
     * @throws java.nio.ReadOnlyBufferException If this buffer is read-only
     */
    public SELF put(T value) {
        int sizeof = sizeof();
        memCopy(value.address(), address + Integer.toUnsignedLong(nextPutIndex()) * sizeof, sizeof);
        return (SELF) this;
    }

    /**
     * Absolute <i>get</i> method. Reads the struct at the specified index.
     *
     * <p>The struct instance returned is a view of the buffer at the specified position. Changes to this buffer's content will be visible in the struct
     * instance and vice versa.</p>
     *
     * @param index the index from which the struct will be read
     *
     * @return the struct at the specified index
     *
     * @throws IndexOutOfBoundsException If {@code index} is negative or not smaller than the buffer's limit
     */
    @ForceInline
    public T get(int index) {
        return Struct.create((Class<T>) getElementClass(), address + Integer.toUnsignedLong(check(index, limit)) * sizeof(), container);
    }

    /**
     * Absolute <i>get</i> method. Reads the struct data at the specified index into the specified struct.
     *
     * @param index the index from which the struct will be read
     *
     * @return the struct at the specified index
     *
     * @throws IndexOutOfBoundsException If {@code index} is negative or not smaller than the buffer's limit
     */
    public SELF get(int index, T value) {
        int sizeof = sizeof();
        memCopy(address + Checks.check(index, limit) * sizeof, value.address(), sizeof);
        return (SELF) this;
    }

    /**
     * Absolute <i>put</i> method&nbsp;&nbsp;<i>(optional operation)</i>.
     *
     * <p>Writes the specified struct into this buffer at the specified index.</p>
     *
     * @param index the index at which the struct will be written
     * @param value the struct value to be written
     *
     * @return This buffer
     *
     * @throws IndexOutOfBoundsException        If {@code index} is negative or not smaller than the buffer's limit
     * @throws java.nio.ReadOnlyBufferException If this buffer is read-only
     */
    public SELF put(int index, T value) {
        int sizeof = sizeof();
        memCopy(value.address(), address + Checks.check(index, limit) * sizeof, sizeof);
        return (SELF) this;
    }

    /**
     * Relative <i>apply</i> method.
     *
     * <p>Passes the struct at the buffer's current position to the specified {@link Consumer}, and then increments the position.</p>
     *
     * @return This buffer
     *
     * @throws java.nio.BufferUnderflowException If the buffer's current position is not smaller than its limit
     */
    public SELF apply(Consumer<T> consumer) {
        consumer.accept(get());
        return (SELF) this;
    }

    /**
     * Absolute <i>apply</i> method.
     *
     * <p>Passes the struct at the specified position to the specified {@link Consumer}.</p>
     *
     * @param index the index where the {@code consumer} will be applied.
     *
     * @return This buffer
     *
     * @throws IndexOutOfBoundsException If {@code index} is negative or not smaller than the buffer's limit
     */
    public SELF apply(int index, Consumer<T> consumer) {
        consumer.accept(get(index));
        return (SELF) this;
    }

    // --------------------------------------

    @Override
    public Iterator<T> iterator() {
        return new StructIterator<>(address, container, (Class<T>) getElementClass(), sizeof(), position, limit);
    }

    static class BaseIter<T extends Struct<T>> {
        protected final long       address;
        @Nullable
        protected final ByteBuffer container;
        protected final Class<T>   type;
        protected final int        sizeof;
        protected       int        index;
        protected final int        fence;
        BaseIter(long address, @Nullable ByteBuffer container, Class<T> type, int sizeof, int position, int limit) {
            this.address = address;
            this.container = container;
            this.type = type;
            this.sizeof = sizeof;
            this.index = position;
            this.fence = limit;
        }

        final T getAt(int i) {
            return Struct.create(type, address + Integer.toUnsignedLong(i) * sizeof, container);
        }
    }
    // This class is static to avoid capturing the StructBuffer instance. Hotspot trivially marks the instance
    // as escaping when this happens, even if the iterator instance is not escaping and scalar replaced. This
    // is not a problem on Graal. Also, see JDK-8166840.
    static class StructIterator<T extends Struct<T>> extends BaseIter<T> implements Iterator<T> {

        StructIterator(long address, @Nullable ByteBuffer container, Class<T> type, int sizeof, int position, int limit) {
            super(address, container, type, sizeof, position, limit);
        }

        @Override public boolean hasNext() {
            return index < fence;
        }

        @Override public T next() {
            if (CHECKS && fence <= index) {
                throw new NoSuchElementException();
            }
            return getAt(index++);
        }

        @Override public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            int i = index;
            try {
                for (; i < fence; i++) {
                    action.accept(getAt(i));
                }
            } finally {
                index = i;
            }
        }
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Objects.requireNonNull(action);
        Class<T> type = (Class<T>)getElementClass();
        int sizeof = sizeof();
        for (int i = position, fence = limit; i < fence; i++) {
            action.accept(Struct.create(type, address + Integer.toUnsignedLong(i) * sizeof, container));
        }
    }

    @Override
    public Spliterator<T> spliterator() {
        return new StructSpliterator<>(address, container, (Class<T>) getElementClass(), sizeof(), position, limit);
    }

    static class StructSpliterator<T extends Struct<T>> extends BaseIter<T> implements Spliterator<T> {
        StructSpliterator(long address, @Nullable ByteBuffer container, Class<T> type, int sizeof, int position, int limit) {
            super(address, container, type, sizeof, position, limit);
        }

        @Override
        public boolean tryAdvance(Consumer<? super T> action) {
            Objects.requireNonNull(action);

            if (index < fence) {
                action.accept(getAt(index++));
                return true;
            }

            return false;
        }

        @Override
        public @Nullable Spliterator<T> trySplit() {
            int lo = index,
                mid = (lo + fence) >>> 1;

            return lo < mid
                ? new StructSpliterator<>(address, container, type, sizeof, lo, index = mid)
                : null;
        }

        @Override
        public long estimateSize() {
            return fence - index;
        }

        @Override
        public int characteristics() {
            return Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.SIZED | Spliterator.SUBSIZED;
        }

        @Override
        public void forEachRemaining(Consumer<? super T> action) {
            Objects.requireNonNull(action);
            int i = index;
            try {
                for (; i < fence; i++) {
                    action.accept(getAt(i));
                }
            } finally {
                index = i;
            }
        }

        @Override
        public Comparator<? super T> getComparator() {
            throw new IllegalStateException();
        }
    }

    /** Returns a sequential {@code Stream} with this struct buffer as its source. */
    public Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    /** Returns a parallel {@code Stream} with this struct buffer as its source. */
    public Stream<T> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    // --------------------------------------
    private static int check(int index, int length) {
        if (CHECKS && (index < 0 || length <= index)) {
            throw new IndexOutOfBoundsException();
        }
        return index;
    }

}