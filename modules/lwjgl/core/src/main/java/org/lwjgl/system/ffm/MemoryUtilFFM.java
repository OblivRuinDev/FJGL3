/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system.ffm;

import jdk.internal.misc.*;
import org.jspecify.annotations.*;
import org.lwjgl.system.*;

import java.lang.foreign.*;

import static java.lang.Double.*;
import static java.lang.Float.*;
import static org.lwjgl.system.APIUtil.*;
import static org.lwjgl.system.Checks.*;
import static org.lwjgl.system.Pointer.*;

/**
 * Additional memory utilities for FFM.
 *
 * <p>This API is meant to be used with static import.</p>
 */
@NullMarked
public final class MemoryUtilFFM {
    private MemoryUtilFFM() {
    }
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private static final String PROPERTY_PATH = "java.lang.foreign.native.threshold.power.";
    private static final long NATIVE_THRESHOLD_COPY = powerOfPropertyOr("copy", 6);
    private static long powerOfPropertyOr(String name, int defaultPower) {
        int power = Integer.getInteger(PROPERTY_PATH + name, defaultPower);
        return 1L << Math.clamp(power, 0, Integer.SIZE - 2);
    }

    // --- [ memset ] ---

    /**
     * Sets all bytes in a specified block of memory to a fixed value (usually zero).
     *
     * @param ptr   the starting memory address
     * @param value the value to set (memSet will convert it to unsigned byte)
     *///todo: JDK-8329331 has alleviated this problem, jmh is need here to
    public static void memSet(MemorySegment ptr, int value) {
        long bytes = ptr.byteSize();
        if (bytes == 0L)
            return;
        var b = (byte)(value & 0xFF);

        // On x64, setMemory has degraded performance with byte counts that are even.
        // Workaround by setting all but the last byte with setMemory, then setting the last byte separately.
        // Does not hurt on non-x64.
        var lastByteIndex = bytes - 1L;
        ptr.asSlice(0, lastByteIndex + (bytes & 1L)).fill(b);
        ptr.set(ValueLayout.JAVA_BYTE, lastByteIndex, b);
    }

    // --- [ memcpy ] ---
    /*
    Performance depends on -Djava.lang.foreign.native.threshold.power.copy=<PoT bytes> (default: 6)

    This code is tuned for:

      * call sites with constant (at runtime) byte counts
      * call sites with variable byte counts

    Constant byte counts prefer a Java implementation that can be specialized by the JIT.
    Variable byte counts prefer the Unsafe::copyMemory intrinsic for consistent performance. A Java implementation would have to optimize for the worst
    case (high byte counts) and that hurts the common case (lower byte counts) too much.

    In practice, the JIT optimizes the Java implementation in 3 different modes:

      * Normal accesses, no safepoint polling. Best for small copies, roughly up to 1 << 6 bytes.
      * Normal accesses, with safepoint polling. Safepoints hurt a bit, but are necessary to avoid GC stalls. Used for copies roughly up to 1 << 14 bytes.
      * Vectorized accesses, with safepoint polling. Best for large copies.

    The implementation below currently supports JIT specialization only up to NATIVE_THRESHOLD_COPY bytes.
    Uncomment the withSafepoint and withVectorization paths to test the other modes.
    Also note that client-side Java implementations cannot really compete with the JDK, which has lower unsafe access (ScopedMemoryAccess).

    TODO: re-evaluate in future JDKs
    JDK 25: up to NATIVE_THRESHOLD_COPY inlines nicely, optimization suffers after that.
    JDK 26: massively improved loop unrolling + autovectorization for constant call sites, but Unsafe remains competitive for variable call sites.
   */
    /**
     * Sets all bytes in a specified block of memory to a copy of another block.
     *
     * @param src the source memory address
     * @param dst the destination memory address
     */
    public static void memCopy(MemorySegment src, MemorySegment dst) {
        long bytes = src.byteSize();
        if (CHECKS) {
            check(dst, bytes);
        }
        if (bytes == 0)
            return;

        // On x64, copyMemory has degraded performance with byte counts that are multiple of 4.
        // Workaround by copying all but the last byte with copyMemory, then copying the last byte separately.
        // Does not hurt on non-x64.
        var lastByteIndex = bytes - 1L;
        var copyBytes     = lastByteIndex + (bytes & 1L);

        var S = src.asSlice(0, copyBytes);
        var D = dst.asSlice(0, copyBytes);

        D.copyFrom(S);

        dst.set(ValueLayout.JAVA_BYTE, lastByteIndex, src.get(ValueLayout.JAVA_BYTE, lastByteIndex));
    }


    // --- [ Array to MemorySegment memcpy ] ---

    private static void memcpy(MemorySegment src, MemorySegment dst, long offset, long bytes) {
        if (bytes < NATIVE_THRESHOLD_COPY) {
            var S = src.asSlice(offset, bytes);
            var D = dst.asSlice(0, bytes);

            D.copyFrom(S);
        } else {
            long lastByteIndex = bytes - 1L;
            long copyBytes     = lastByteIndex + (bytes & 1L);

            var S = src.asSlice(offset, copyBytes);
            var D = dst.asSlice(0, copyBytes);

            D.copyFrom(S);

            dst.set(ValueLayout.JAVA_BYTE, lastByteIndex, src.get(ValueLayout.JAVA_BYTE, offset + lastByteIndex));
        }
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(byte[] src, MemorySegment dst) {
        if (CHECKS) {
            check(dst, src.length);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, src.length);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(char[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 1);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(short[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 1);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(int[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 2);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(long[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 3);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(float[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 2);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies the source array to the current position of the destination memory segment.
     *
     * @param src the source array
     * @param dst the destination memory segment
     */
    public static void memCopy(double[] src, MemorySegment dst) {
        long bytes = apiGetBytes(src.length, 3);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, 0, bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(byte[] src, MemorySegment dst, int offset, int size) {
        if (CHECKS) {
            check(dst, size);
        }
        memcpy(MemorySegment.ofArray(src), dst, offset, size);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(char[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 1);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 1), bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(short[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 1);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 1), bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(int[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 2);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 2), bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(long[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 3);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 3), bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(float[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 2);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 2), bytes);
    }

    /**
     * Copies {@code size} elements from the source array, starting at {@code offset}, to the current position of the destination memory segment.
     *
     * @param src    the source array
     * @param dst    the destination memory segment
     * @param offset the offset into the source array
     * @param size   the number of elements to copy
     */
    public static void memCopy(double[] src, MemorySegment dst, int offset, int size) {
        long bytes = apiGetBytes(size, 3);
        if (CHECKS) {
            check(dst, bytes);
        }
        memcpy(MemorySegment.ofArray(src), dst, apiGetBytes(offset, 3), bytes);
    }

    // --- [ MemorySegment to Array memcpy ] ---

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, byte[] dst) {
        if (CHECKS) {
            check(src, dst.length);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, dst.length);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, char[] dst) {
        long bytes = apiGetBytes(dst.length, 1);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, short[] dst) {
        long bytes = apiGetBytes(dst.length, 1);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, int[] dst) {
        long bytes = apiGetBytes(dst.length, 2);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, long[] dst) {
        long bytes = apiGetBytes(dst.length, 3);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, float[] dst) {
        long bytes = apiGetBytes(dst.length, 2);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies the source memory segment to the destination array.
     *
     * @param src the source memory segment
     * @param dst the destination array
     */
    public static void memCopy(MemorySegment src, double[] dst) {
        long bytes = apiGetBytes(dst.length, 3);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), 0, bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, byte[] dst, int offset, int size) {
        if (CHECKS) {
            check(src, size);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 1), size);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, char[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 1);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 1), bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, short[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 1);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 1), bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, int[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 2);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 2), bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, long[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 3);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 3), bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, float[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 2);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 2), bytes);
    }

    /**
     * Copies {@code size} elements from the source memory segment to the destination array, starting at {@code offset}.
     *
     * @param src    the source memory segment
     * @param dst    the destination array
     * @param offset the offset into the destination array
     * @param size   the number of elements to copy
     */
    public static void memCopy(MemorySegment src, double[] dst, int offset, int size) {
        long bytes = apiGetBytes(size, 3);
        if (CHECKS) {
            check(src, bytes);
        }
        memcpy(src, MemorySegment.ofArray(dst), apiGetBytes(offset, 3), bytes);
    }

    public static boolean memGetBoolean(MemorySegment segment, long offset)                          { return UNSAFE.getBoolean(null, segment.address() + offset); }

    public static byte memGetByte(MemorySegment segment, long offset)                                { return UNSAFE.getByte(null, segment.address() + offset); }

    public static char memGetChar(MemorySegment segment, long offset)                                { return UNSAFE.getChar(null, segment.address() + offset); }
    public static char memGetCharAtIndex(MemorySegment segment, long index)                          { return UNSAFE.getChar(null, segment.address() + (index << 1)); }
    public static char memGetCharUnaligned(MemorySegment segment, long offset)                       { return UNSAFE.getCharUnaligned(null, segment.address() + offset); }
    public static char memGetCharUnalignedAtIndex(MemorySegment segment, long index)                 { return UNSAFE.getCharUnaligned(null, segment.address() + (index << 1)); }

    public static short memGetShort(MemorySegment segment, long offset)                              { return UNSAFE.getShort(null, segment.address() + offset); }
    public static short memGetShortAtIndex(MemorySegment segment, long index)                        { return UNSAFE.getShort(null, segment.address() + (index << 1)); }
    public static short memGetShortUnaligned(MemorySegment segment, long offset)                     { return UNSAFE.getShortUnaligned(null, segment.address() + offset); }
    public static short memGetShortUnalignedAtIndex(MemorySegment segment, long index)               { return UNSAFE.getShortUnaligned(null, segment.address() + (index << 1)); }

    public static int memGetInt(MemorySegment segment, long offset)                                  { return UNSAFE.getInt(null, segment.address() + offset); }
    public static int memGetIntAtIndex(MemorySegment segment, long index)                            { return UNSAFE.getInt(null, segment.address() + (index << 2)); }
    public static int memGetIntUnaligned(MemorySegment segment, long offset)                         { return UNSAFE.getIntUnaligned(null, segment.address() + offset); }
    public static int memGetIntUnalignedAtIndex(MemorySegment segment, long index)                   { return UNSAFE.getIntUnaligned(null, segment.address() + (index << 2)); }

    public static long memGetLong(MemorySegment segment, long offset)                                { return UNSAFE.getLong(null, segment.address() + offset); }
    public static long memGetLongAtIndex(MemorySegment segment, long index)                          { return UNSAFE.getLong(null, segment.address() + (index << 3)); }
    public static long memGetLongUnaligned(MemorySegment segment, long offset)                       { return UNSAFE.getLongUnaligned(null, segment.address() + offset); }
    public static long memGetLongUnalignedAtIndex(MemorySegment segment, long index)                 { return UNSAFE.getLongUnaligned(null, segment.address() + (index << 3)); }

    public static float memGetFloat(MemorySegment segment, long offset)                              { return UNSAFE.getFloat(null, segment.address() + offset); }
    public static float memGetFloatAtIndex(MemorySegment segment, long index)                        { return UNSAFE.getFloat(null, segment.address() + (index << 2)); }
    public static float memGetFloatUnaligned(MemorySegment segment, long offset)                     { return intBitsToFloat(UNSAFE.getIntUnaligned(null, segment.address() + offset)); }
    public static float memGetFloatUnalignedAtIndex(MemorySegment segment, long index)               { return intBitsToFloat(UNSAFE.getIntUnaligned(null, segment.address() + (index << 2))); }

    public static double memGetDouble(MemorySegment segment, long offset)                            { return UNSAFE.getDouble(null, segment.address() + offset); }
    public static double memGetDoubleAtIndex(MemorySegment segment, long index)                      { return UNSAFE.getDouble(null, segment.address() + (index << 3)); }
    public static double memGetDoubleUnaligned(MemorySegment segment, long offset)                   { return longBitsToDouble(UNSAFE.getLongUnaligned(null, segment.address() + offset)); }
    public static double memGetDoubleUnalignedAtIndex(MemorySegment segment, long index)             { return longBitsToDouble(UNSAFE.getLongUnaligned(null, segment.address() + (index << 3))); }

    public static void memPutBoolean(MemorySegment segment, long offset, boolean value)              { UNSAFE.putBoolean(null, segment.address() + offset, value); }

    public static void memPutByte(MemorySegment segment, long offset, byte value)                    { UNSAFE.putByte(null, segment.address() + offset, value); }

    public static void memPutChar(MemorySegment segment, long offset, char value)                    { UNSAFE.putChar(null, segment.address() + offset, value); }
    public static void memPutCharAtIndex(MemorySegment segment, long index, char value)              { UNSAFE.putChar(null, segment.address() + (index << 1), value); }
    public static void memPutCharUnaligned(MemorySegment segment, long offset, char value)           { UNSAFE.putCharUnaligned(null, segment.address() + offset, value); }
    public static void memPutCharUnalignedAtIndex(MemorySegment segment, long index, char value)     { UNSAFE.putCharUnaligned(null, segment.address() + (index << 1), value); }

    public static void memPutShort(MemorySegment segment, long offset, short value)                  { UNSAFE.putShort(null, segment.address() + offset, value); }
    public static void memPutShortAtIndex(MemorySegment segment, long index, short value)            { UNSAFE.putShort(null, segment.address() + (index << 1), value); }
    public static void memPutShortUnaligned(MemorySegment segment, long offset, short value)         { UNSAFE.putShortUnaligned(null, segment.address() + offset, value); }
    public static void memPutShortUnalignedAtIndex(MemorySegment segment, long index, short value)   { UNSAFE.putShortUnaligned(null, segment.address() + (index << 1), value); }

    public static void memPutInt(MemorySegment segment, long offset, int value)                      { UNSAFE.putInt(null, segment.address() + offset, value); }
    public static void memPutIntAtIndex(MemorySegment segment, long index, int value)                { UNSAFE.putInt(null, segment.address() + (index << 2), value); }
    public static void memPutIntUnaligned(MemorySegment segment, long offset, int value)             { UNSAFE.putIntUnaligned(null, segment.address() + offset, value); }
    public static void memPutIntUnalignedAtIndex(MemorySegment segment, long index, int value)       { UNSAFE.putIntUnaligned(null, segment.address() + (index << 2), value); }

    public static void memPutLong(MemorySegment segment, long offset, long value)                    { UNSAFE.putLong(null, segment.address() + offset, value); }
    public static void memPutLongAtIndex(MemorySegment segment, long index, long value)              { UNSAFE.putLong(null, segment.address() + (index << 3), value); }
    public static void memPutLongUnaligned(MemorySegment segment, long offset, long value)           { UNSAFE.putLongUnaligned(null, segment.address() + offset, value); }
    public static void memPutLongUnalignedAtIndex(MemorySegment segment, long index, long value)     { UNSAFE.putLongUnaligned(null, segment.address() + (index << 3), value); }

    public static void memPutFloat(MemorySegment segment, long offset, float value)                  { UNSAFE.putFloat(null, segment.address() + offset, value); }
    public static void memPutFloatAtIndex(MemorySegment segment, long index, float value)            { UNSAFE.putFloat(null, segment.address() + (index << 2), value); }
    public static void memPutFloatUnaligned(MemorySegment segment, long offset, float value)         { UNSAFE.putIntUnaligned(null, segment.address() + offset, floatToRawIntBits(value)); }
    public static void memPutFloatUnalignedAtIndex(MemorySegment segment, long index, float value)   { UNSAFE.putIntUnaligned(null, segment.address() + (index << 2), floatToRawIntBits(value)); }

    public static void memPutDouble(MemorySegment segment, long offset, double value)                { UNSAFE.putDouble(null, segment.address() + offset, value); }
    public static void memPutDoubleAtIndex(MemorySegment segment, long index, double value)          { UNSAFE.putDouble(null, segment.address() + (index << 3), value); }
    public static void memPutDoubleUnaligned(MemorySegment segment, long offset, double value)       { UNSAFE.putLongUnaligned(null, segment.address() + offset, doubleToRawLongBits(value)); }
    public static void memPutDoubleUnalignedAtIndex(MemorySegment segment, long index, double value) { UNSAFE.putLongUnaligned(null, segment.address() + (index << 3), doubleToRawLongBits(value)); }


    public static long memGetCLong(MemorySegment segment, long offset) {
        return CLONG_SIZE == 8 ? memGetLong(segment, offset) : memGetInt(segment, offset);
    }
    public static long memGetCLongAtIndex(MemorySegment segment, long index) {
        return CLONG_SIZE == 8 ? memGetLongAtIndex(segment, index) : memGetIntAtIndex(segment, index);
    }
    public static long memGetCLongUnaligned(MemorySegment segment, long offset) {
        return CLONG_SIZE == 8 ? memGetLongUnaligned(segment, offset) : memGetIntUnaligned(segment, offset);
    }
    public static long memGetCLongUnalignedAtIndex(MemorySegment segment, long index) {
        return CLONG_SIZE == 8 ? memGetLongUnalignedAtIndex(segment, index) : memGetIntUnalignedAtIndex(segment, index);
    }
    public static void memPutCLong(MemorySegment segment, long offset, long value) {
        if (CLONG_SIZE == 8) memPutLong(segment, offset,      value);
        else                 memPutInt (segment, offset, (int)value);
    }
    public static void memPutCLongAtIndex(MemorySegment segment, long index, long value) {
        if (CLONG_SIZE == 8) memPutLongAtIndex(segment, index,      value);
        else                 memPutIntAtIndex (segment, index, (int)value);
    }
    public static void memPutCLongUnaligned(MemorySegment segment, long offset, long value) {
        if (CLONG_SIZE == 8) memPutLongUnaligned(segment, offset,      value);
        else                 memPutIntUnaligned (segment, offset, (int)value);
    }
    public static void memPutCLongUnalignedAtIndex(MemorySegment segment, long index, long value) {
        if (CLONG_SIZE == 8) memPutLongUnalignedAtIndex(segment, index,      value);
        else                 memPutIntUnalignedAtIndex (segment, index, (int)value);
    }

    public static long memGetAddress(MemorySegment segment, long offset) {
        return POINTER_SIZE == 8 ? memGetLong(segment, offset) : memGetInt(segment, offset) & 0xFFFF_FFFFL;
    }
    public static long memGetAddressAtIndex(MemorySegment segment, long index) {
        return POINTER_SIZE == 8 ? memGetLongAtIndex(segment, index) : memGetIntAtIndex(segment, index) & 0xFFFF_FFFFL;
    }
    public static long memGetAddressUnaligned(MemorySegment segment, long offset) {
        return POINTER_SIZE == 8 ? memGetLongUnaligned(segment, offset) : memGetIntUnaligned(segment, offset) & 0xFFFF_FFFFL;
    }
    public static long memGetAddressUnalignedAtIndex(MemorySegment segment, long index) {
        return POINTER_SIZE == 8 ? memGetLongUnalignedAtIndex(segment, index) : memGetIntUnalignedAtIndex(segment, index) & 0xFFFF_FFFFL;
    }

    public static void memPutAddress(MemorySegment segment, long offset, long value) {
        if (POINTER_SIZE == 8) memPutLong(segment, offset,      value);
        else                   memPutInt (segment, offset, (int)value);
    }
    public static void memPutAddressAtIndex(MemorySegment segment, long index, long value) {
        if (POINTER_SIZE == 8) memPutLongAtIndex(segment, index,      value);
        else                   memPutIntAtIndex (segment, index, (int)value);
    }
    public static void memPutAddressUnaligned(MemorySegment segment, long offset, long value) {
        if (POINTER_SIZE == 8) memPutLongUnaligned(segment, offset,      value);
        else                   memPutIntUnaligned (segment, offset, (int)value);
    }
    public static void memPutAddressUnalignedAtIndex(MemorySegment segment, long index, long value) {
        if (POINTER_SIZE == 8) memPutLongUnalignedAtIndex(segment, index,      value);
        else                   memPutIntUnalignedAtIndex (segment, index, (int)value);
    }

    private static void check(MemorySegment segment, long size) {
        if (segment.byteSize() < size) {
            throwIAE(segment.byteSize(), size);
        }
    }

    private static void throwIAE(long segmentSize, long minimumSize) {
        throw new IllegalArgumentException("MemorySegment size is " + segmentSize + ", must be at least " + minimumSize);
    }

}