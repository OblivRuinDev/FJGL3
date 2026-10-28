/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package vulkan

import org.lwjgl.generator.*
import vulkan.VKFunctionType.*
import java.io.*

private val NativeClass.capName: String
    get() = if (templateName.startsWith(prefix)) {
        if (prefix == "VK")
            "Vulkan${templateName.substring(2)}"
        else
            templateName
    } else {
        "${prefixTemplate}_$templateName"
    }

private const val CAPS_INSTANCE = "VKCapabilitiesInstance"
private const val CAPS_DEVICE = "VKCapabilitiesDevice"

private val EXTENSION_TYPES = HashMap<String, String>()

private val NativeClass.isInstanceExtension get() = EXTENSION_TYPES[templateName] == "instance" || templateName.startsWith("VK")
private val NativeClass.isDeviceExtension get() = EXTENSION_TYPES[templateName] == "device" || templateName.startsWith("VK")

private enum class VKFunctionType {
    PROC,
    GLOBAL,
    INSTANCE,
    DEVICE
}

private val Func.type: VKFunctionType
    get() = when {
        name == "vkGetInstanceProcAddr"                 -> PROC // dlsym/GetProcAddress
        parameters[0].nativeType !is WrappedPointerType -> GLOBAL // vkGetInstanceProcAddr: VK_NULL_HANDLE
        parameters[0].nativeType.let { it === VkInstance || it === VkPhysicalDevice } || EXTENSION_TYPES[nativeClass.templateName] == "instance"
                                                        -> INSTANCE // vkGetInstanceProcAddr: instance handle
        else                                            -> DEVICE // vkGetDeviceProcAddr: device handle
    }

private val Func.isInstanceFunction get() = type === INSTANCE && !has<Macro>()
private val Func.isDeviceFunction get() = type === DEVICE && !has<Macro>()

val VK_BINDING_INSTANCE = Generator.register(object : AbstractBinding(
    Module.VULKAN,
    CAPS_INSTANCE,
    APICapabilities.PARAM_CAPABILITIES
) {

    private val instanceChecks = CapsChecks("MemoryStack stack, FunctionProvider provider, long[] caps, Set<String> ext", "stack, provider, caps", "VK", supportsDependencies = true)

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "VK10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.print("$t${t}long $FUNCTION_ADDRESS = ")
        writer.println(if (function.has<Capabilities>())
            "${function.get<Capabilities>().expression}.${function.name};"
        else
            "${function.getParams { it.nativeType is WrappedPointerType }.first().name}${
                if (function.isInstanceFunction && !function.parameters[0].nativeType.let { it === VkInstance || it === VkPhysicalDevice })
                    ".getCapabilitiesInstance()"
                else
                    ".getCapabilities()"
            }.${function.name};"
        )
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass, commands: Map<String, Int>) {
        printExtCheckMethod(
            instanceChecks, nativeClass, nativeClass.capName, { it.isInstanceFunction }, commands,
            buildDependencyFlags(nativeClass.functions.filter { it.isInstanceFunction && it.has<DependsOn>() })
        )
    }

    init {
        javaImport("static org.lwjgl.system.Checks.*")
    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println(
            """
            Reports the enabled capabilities and function pointers of a Vulkan {@code VkInstance}.

            The addresses are cached for future use. This class also allows developers to query the capabilities made available to the Vulkan instance handle.
            """.toJavaDoc(indentation = "")
        )
        println("@SuppressWarnings(\"SimplifiableIfStatement\")")
        println("public class $CAPS_INSTANCE {")

        val classes = super.getClasses("VK")

        val instanceCommands = LinkedHashMap<String, Int>()
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .forEach {
                val functions = it.functions.asSequence()
                    .filter { cmd ->
                        if (cmd.isInstanceFunction) {
                            if (!instanceCommands.contains(cmd.name)) {
                                instanceCommands[cmd.name] = instanceCommands.size
                                return@filter true
                            }
                        }
                        false
                    }
                    .joinToString(",\n$t$t") { cmd -> cmd.name }

                if (functions.isNotEmpty()) {
                    println("\n$t/** Function pointers for ${it.templateName} */")
                    println("${t}public final long")
                    println("$t$t$functions;")
                }
            }

        println(
            """
    /** The Vulkan API version number. */
    public final int apiVersion;
"""
        )

        classes
            .filter { EXTENSION_TYPES[it.templateName] ?: "instance" == "instance" }
            .forEach {
                println(it.getCapabilityJavadoc())
                println("${t}public final boolean ${it.capName};")
            }

        if (classes.any { isReducedCheck(it, { func -> func.isInstanceFunction }, instanceCommands) }) printCheckHelper(instanceChecks)

        print(
            """
    $CAPS_INSTANCE(FunctionProvider provider, int apiVersion, Set<String> ext, Set<String> deviceExt) {
        this.apiVersion = apiVersion;

        long[] caps = new long[${instanceCommands.size}];
        MemoryStack stack = MemoryStack.stackGet();
"""
        )

        classes.forEach {
            val capName = it.capName
            if (it.functions.any { func -> func.isInstanceFunction }) {
                if (it.isInstanceExtension) {
                    printCapCheck(this, instanceChecks, it, capName, "ext", { func -> func.isInstanceFunction }, instanceCommands, { "check_${it.templateName}(stack, provider, caps, ext)" })
                } else {
                    printCapCheck(this, instanceChecks, it, capName, "deviceExt", { func -> func.isInstanceFunction }, instanceCommands, { "check_${it.templateName}(stack, provider, caps, deviceExt)" }, assign = false)
                }
            } else if (it.isInstanceExtension) {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }

        println()
        printCapsAssignments(this, instanceCommands) { "caps[$it]" }

        print(
            """
    }
""")

        for (extension in classes) {
            if (extension.functions.any { it.isInstanceFunction }) {
                checkExtensionFunctions(extension, instanceCommands)
            }
        }

        println("\n}")
    }

})

val VK_BINDING_DEVICE = Generator.register(object : GeneratorTarget(Module.VULKAN, CAPS_DEVICE) {

    /**
     * The shared generation logic lives in [AbstractBinding], whose members are only visible from inside an {@code AbstractBinding} subclasses. The device
     * capabilities class is generated by a plain [GeneratorTarget] (to keep its generated preamble untouched), so the emission is delegated through this
     * private stub that is never registered with the Generator.
     */
    private val shared = object : AbstractBinding(Module.VULKAN, CAPS_DEVICE) {

        override fun PrintWriter.generateJava() {
            error("This stub is never registered with the Generator.")
        }

        override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
            error("This stub is never registered with the Generator.")
        }

        private val deviceChecks = CapsChecks("MemoryStack stack, FunctionProvider provider, long[] caps, Set<String> ext", "stack, provider, caps", "VK", supportsDependencies = true)

        fun checkExtensionFunctions(writer: PrintWriter, nativeClass: NativeClass, commands: Map<String, Int>) {
            writer.printExtCheckMethod(
                deviceChecks, nativeClass, nativeClass.capName, { it.isDeviceFunction }, commands,
                buildDependencyFlags(nativeClass.functions.filter { it.isDeviceFunction && it.has<DependsOn>() })
            )
        }

        fun printCheckHelper(writer: PrintWriter, classes: List<NativeClass>, filter: (Func) -> Boolean, ordinals: Map<String, Int>) {
            if (classes.any { isReducedCheck(it, filter, ordinals) }) {
                writer.printCheckHelper(deviceChecks)
            }
        }

        fun printCapCheck(writer: PrintWriter, nativeClass: NativeClass, capName: String, extRef: String, filter: (Func) -> Boolean, ordinals: Map<String, Int>, fullCall: (NativeClass) -> String, assign: Boolean = true) {
            printCapCheck(writer, deviceChecks, nativeClass, capName, extRef, filter, ordinals, fullCall, assign)
        }

        fun printCapsAssignments(writer: PrintWriter, commands: Map<String, Int>) {
            printCapsAssignments(writer, commands) { "caps[$it]" }
        }
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass, commands: Map<String, Int>) {
        shared.checkExtensionFunctions(this, nativeClass, commands)
    }

    init {
        javaImport(
            "java.util.*",
            "org.lwjgl.system.*",

            "static org.lwjgl.system.Checks.*"
        )
    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println(
            """
            Reports the enabled capabilities and function pointers of a Vulkan {@code VkDevice}.

            The addresses are cached for future use. This class also allows developers to query the capabilities made available to the Vulkan device handle.
            """.toJavaDoc(indentation = "")
        )
        println("@SuppressWarnings(\"SimplifiableIfStatement\")")
        println("public class $CAPS_DEVICE {")

        val classes = VK_BINDING_INSTANCE.getClasses("VK")

        val deviceCommands = LinkedHashMap<String, Int>()
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .forEach {
                val functions = it.functions.asSequence()
                    .filter { cmd ->
                        if (cmd.type === DEVICE && !cmd.has<Macro>()) {
                            if (!deviceCommands.contains(cmd.name)) {
                                deviceCommands[cmd.name] = deviceCommands.size
                                return@filter true
                            }
                        }
                        false
                    }
                    .joinToString(",\n$t$t") { cmd -> cmd.name }

                if (functions.isNotEmpty()) {
                    println("\n$t/** Function pointers for ${it.templateName} */")
                    println("${t}public final long")
                    println("$t$t$functions;")
                }
            }

        println(
            """
    /** The Vulkan API version number. */
    public final int apiVersion;
"""
        )

        classes
            .filter { EXTENSION_TYPES[it.templateName] ?: "device" == "device" }
            .forEach {
            println(it.getCapabilityJavadoc())
            println("${t}public final boolean ${it.capName};")
        }

        shared.printCheckHelper(this, classes, { func -> func.isDeviceFunction }, deviceCommands)

        print(
            """
    $CAPS_DEVICE(FunctionProvider provider, VKCapabilitiesInstance capsInstance, int apiVersion, Set<String> ext) {
        this.apiVersion = apiVersion;

        long[] caps = new long[${deviceCommands.size}];
        MemoryStack stack = MemoryStack.stackGet();
"""
        )

        classes.forEach {
            val capName = it.capName
            if (it.functions.any { func -> func.isDeviceFunction }) {
                if (it.isDeviceExtension) {
                    shared.printCapCheck(this, it, capName, "ext", { func -> func.isDeviceFunction }, deviceCommands, { "check_${it.templateName}(stack, provider, caps, ext)" })
                } else {
                    shared.printCapCheck(this, it, capName, "capsInstance", { func -> func.isDeviceFunction }, deviceCommands, { "check_${it.templateName}(stack, provider, caps, capsInstance)" }, assign = false)
                }
            } else if (it.isDeviceExtension) {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }

        println()
        shared.printCapsAssignments(this, deviceCommands)

        print(
            """
    }
""")

        for (extension in classes) {
            if (extension.functions.any { it.isDeviceFunction }) {
                checkExtensionFunctions(extension, deviceCommands)
            }
        }

        println("\n}")
    }

})

// DSL Extensions

val GlobalCommand = Capabilities("VK.getGlobalCommands()")

fun String.nativeClassVK(
    templateName: String,
    type: String,
    prefix: String = "VK",
    prefixMethod: String = prefix.lowercase(),
    postfix: String = "",
    cinitSetRTConst: Boolean = true,
    init: (NativeClass.() -> Unit)? = null
): NativeClass {
    EXTENSION_TYPES[templateName] = type
    return nativeClass(
        Module.VULKAN,
        templateName,
        prefix = prefix,
        prefixMethod = prefixMethod,
        postfix = postfix,
        binding = VK_BINDING_INSTANCE,
        cinitSetRTConst = cinitSetRTConst,
        init = init
    )
}