/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package core

import org.lwjgl.generator.*

fun manualExports() {
    Module.CORE.exports.apply {
        // functionMissingAbort is defined in org_lwjgl_system_ThreadLocalUtil.c. Its address is a constant, so it is exported here instead of through a JNI method.
        nativeDirective("extern void JNICALL functionMissingAbort(void);")
        registerManual(ExportsType.ADDRESS, "FUNCTION_MISSING_ABORT", "&functionMissingAbort")

        nativeImport("org_lwjgl_system_MemoryUtil.h")
        registerManual(ExportsType.INT, "CACHE_LINE_SIZE", "(int32_t)org_lwjgl_queryCacheLineSize()", runtime = true)
    }
}
