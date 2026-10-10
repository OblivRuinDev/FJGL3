/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

import jdk.internal.access.*;
import jdk.internal.misc.*;

class JDK {
    static final JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();
    static final JavaLangAccess langAccess = SharedSecrets.getJavaLangAccess();
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    private JDK() { }
}
