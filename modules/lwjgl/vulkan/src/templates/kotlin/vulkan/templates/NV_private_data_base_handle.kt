/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 * MACHINE GENERATED FILE, DO NOT EDIT
 */
package vulkan.templates

import org.lwjgl.generator.*
import vulkan.*

val NV_private_data_base_handle = "NVPrivateDataBaseHandle".nativeClassVK("NV_private_data_base_handle", type = "device", postfix = "NV") {
    IntConstant(
        "NV_PRIVATE_DATA_BASE_HANDLE_SPEC_VERSION".."1"
    )

    StringConstant(
        "NV_PRIVATE_DATA_BASE_HANDLE_EXTENSION_NAME".."VK_NV_private_data_base_handle"
    )

    EnumConstant(
        "STRUCTURE_TYPE_PHYSICAL_DEVICE_PRIVATE_DATA_BASE_HANDLE_FEATURES_NV".."1000707000"
    )

    EnumConstant(
        "PRIVATE_DATA_SLOT_CREATE_BASE_OBJECT_HANDLE_BIT_NV".enum(0x00000001)
    )
}