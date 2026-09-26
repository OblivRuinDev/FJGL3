/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
module dev.oblivruin.fjgl.openxr {
    requires transitive dev.oblivruin.fjgl;

    requires static dev.oblivruin.fjgl.egl;
    requires static dev.oblivruin.fjgl.opengl;
    requires static dev.oblivruin.fjgl.vulkan;

    exports org.lwjgl.openxr;
}