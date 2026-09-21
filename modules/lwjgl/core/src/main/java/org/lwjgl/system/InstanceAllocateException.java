/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

public class InstanceAllocateException extends RuntimeException {
    public InstanceAllocateException(String message) {
        super(message);
    }
    public InstanceAllocateException(Throwable cause) {
        super(cause);
    }
}
