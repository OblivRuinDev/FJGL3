/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import org.testng.annotations.*;

import java.lang.reflect.*;
import java.util.*;

import static org.testng.Assert.*;

@Test
public class DowncallTest {
    static final Method[] DC_METHODS = methodsOf(Downcall.class);
    static final Method[] CC_METHODS = methodsOf(CriticalCall.class);

    /** Only the generated adapters, not the public methods inherited from {@link Object}. */
    private static Method[] methodsOf(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
            .filter(method -> Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers()))
            .toArray(Method[]::new);
    }

    public void testNullSymbol_downcall() {
        assertNullSymbolThrows("Downcall", DC_METHODS);
    }

    public void testNullSymbol_criticalCall() {
        assertNullSymbolThrows("CriticalCall", CC_METHODS);
    }

    /**
     * Invokes every generated adapter with a NULL function address.
     *
     * <p>The FFM linker rejects a NULL address with an {@link IllegalArgumentException}. This both verifies the generated adapters and forces the dynamic
     * constant (the downcall handle) to be resolved, so a malformed signature or an invalid constant dynamic fails here too. It therefore covers the whole
     * binding surface without needing native functions to actually call.</p>
     */
    private static void assertNullSymbolThrows(String className, Method[] methods) {
        for (var method : methods) {
            // Reflective invocation wraps whatever the adapter throws in an InvocationTargetException.
            var exception = expectThrows(
                InvocationTargetException.class,
                () -> method.invoke(null, getArgs0(method.getParameterTypes()))
            );
            var cause = exception.getCause();
            assertTrue(
                cause instanceof IllegalArgumentException,
                className + "." + method.getName() + " threw " + cause
            );
        }
    }

    private static Object[] getArgs0(Class<?>[] parameterTypes) {
        Object[] args = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            args[i] = emptyArg(parameterTypes[i]);
        }
        return args;
    }

    private static Object emptyArg(Class<?> c) {
        if (c == long.class) {
            return 0L;
        } else if (c == int.class) {
            return 0;
        } else if (c == boolean.class) {
            return false;
        } else if (c == float.class) {
            return 0.0F;
        } else if (c == double.class) {
            return 0x0d;
        } else if (c == short.class) {
            return (short) 0;
        } else if (c == byte.class) {
            return (byte) 0;
        }
        throw new IllegalArgumentException("Unknown argument type: " + c);
    }
}
