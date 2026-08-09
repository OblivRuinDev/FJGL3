/*
 * Copyright OblivRuinDev. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import jdk.internal.access.*;

class JDK {
    static final JavaNioAccess nioAccess = SharedSecrets.getJavaNioAccess();
    private JDK() { }
}
