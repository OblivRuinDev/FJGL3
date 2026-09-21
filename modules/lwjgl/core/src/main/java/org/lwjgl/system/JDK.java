/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.system;

import jdk.internal.access.*;

class JDK {
    static final JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();
    static final JavaLangAccess langAccess = SharedSecrets.getJavaLangAccess();
    private JDK() { }
}
