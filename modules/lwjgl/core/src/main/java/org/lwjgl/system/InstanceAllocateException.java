/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
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
