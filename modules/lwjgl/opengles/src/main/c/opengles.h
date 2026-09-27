/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
#pragma once
#include "common_tools.h"

#define APIENTRY

#define tlsGetFunction(index) (uintptr_t)((void **)(*__env)->reserved3)[index]
