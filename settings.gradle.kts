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
rootProject.name = "Root"

// Turn each LWJGL module directory into a Gradle subproject.
file("modules/lwjgl")
    .listFiles { file -> file.isDirectory }
    .sortedBy { it.name }
    .forEach { include(":modules:lwjgl:${it.name}") }