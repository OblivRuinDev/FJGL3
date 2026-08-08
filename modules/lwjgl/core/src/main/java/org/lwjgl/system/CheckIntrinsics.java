/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import java.util.*;

/** Java 9 version of {@code CheckIntrinsics}. */
public final class CheckIntrinsics {

    private CheckIntrinsics() {
    }

    public static int checkIndex(int index, int length) {
        return Objects.checkIndex(index, length);
    }

    public static int checkFromToIndex(int fromIndex, int toIndex, int length) {
        return Objects.checkFromToIndex(fromIndex, toIndex, length);
    }

    public static int checkFromIndexSize(int fromIndex, int size, int length) {
        return Objects.checkFromIndexSize(fromIndex, size, length);
    }

}