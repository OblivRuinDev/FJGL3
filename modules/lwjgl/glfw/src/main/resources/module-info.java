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
module dev.oblivruin.fjgl.glfw {
    requires transitive dev.oblivruin.fjgl;

    requires static dev.oblivruin.fjgl.vulkan;

    exports org.lwjgl.glfw;
}