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
package core.templates

import org.lwjgl.generator.*

val MemoryAccessJNI = "MemoryAccessJNI".nativeClass(Module.CORE) {
    nativeImport(
        "<stdlib.h>",
        "<stdint.h>"
    )
    javaImport("static org.lwjgl.system.MemoryUtil.*")

    access = Access.INTERNAL

    nativeDirective(
        """#ifdef LWJGL_WINDOWS
    static void* __aligned_alloc(size_t alignment, size_t size) {
        return _aligned_malloc(size, alignment);
    }
    #define __aligned_free _aligned_free
#else
    #if defined(__USE_ISOC11)
        #define __aligned_alloc aligned_alloc
    #else
        static void* __aligned_alloc(size_t alignment, size_t size) {
            void *p;
            return posix_memalign(&p, alignment, size) ? NULL : p;
        }
    #endif
    #define __aligned_free free
#endif
""")

    arrayOf(
        "malloc" to "void * (*) (size_t)",
        "calloc" to "void * (*) (size_t, size_t)",
        "realloc" to "void * (*) (void *, size_t)",
        "free" to "void (*) (void *)"
    ).forEach { (name, signature) ->
        macro..Address..signature.handle(
            name,
            void()
        )
    }

    Code(
        nativeValue = "(jlong)(uintptr_t)&__aligned_alloc"
    )..macro..Address.."void * (*) (size_t, size_t)".handle(
        "aligned_alloc",
        void()
    )

    Code(
        nativeValue = "(jlong)(uintptr_t)&__aligned_free"
    )..macro..Address.."void (*) (void *)".handle(
        "aligned_free",
        void()
    )
}