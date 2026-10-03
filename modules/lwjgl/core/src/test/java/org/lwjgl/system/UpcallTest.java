/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.system;

import org.testng.annotations.*;

import java.lang.foreign.*;

import static org.testng.Assert.*;

@Test
public class UpcallTest {
    public void testArenaTypeCreateCoverage() {
        for (var type : Upcalls.ArenaType.values()) {
            Arena a = type.create();
            if (type.isCloseable()) {
                a.close();
            }
        }
    }
}
