/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
module dev.oblivruin.fjgl.nanovg {
    requires transitive dev.oblivruin.fjgl;

    requires static dev.oblivruin.fjgl.bgfx;
    requires static dev.oblivruin.fjgl.opengl;
    requires static dev.oblivruin.fjgl.opengles;

    exports org.lwjgl.nanovg;
}