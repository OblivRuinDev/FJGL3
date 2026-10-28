/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package openxr

import org.lwjgl.generator.*
import openxr.XRFunctionType.*
import java.io.*

private val NativeClass.capName: String
    get() = if (templateName.startsWith(prefix)) {
        if (prefix == "XR")
            "OpenXR${templateName.substring(2)}"
        else
            templateName
    } else {
        "${prefixTemplate}_$templateName"
    }

private enum class XRFunctionType {
    PROC,
    GLOBAL,
    INSTANCE
}

private val Func.type: XRFunctionType
    get() = when {
        name == "xrGetInstanceProcAddr"                 -> PROC // dlsym/GetProcAddress
        parameters[0].nativeType !is WrappedPointerType -> GLOBAL // xrGetInstanceProcAddr: XR_NULL_HANDLE
        else                                            -> INSTANCE // xrGetDeviceProcAddr: instance handle
    }

private val Func.isInstanceFunction get() = type === INSTANCE && !has<Macro>()

val XR_BINDING_INSTANCE = Generator.register(object : AbstractBinding(
    Module.OPENXR,
    "XRCapabilities",
    APICapabilities.PARAM_CAPABILITIES
) {

    private val checks = CapsChecks("MemoryStack stack, FunctionProvider provider, long[] caps, java.util.Set<String> ext", "stack, provider, caps", "XR", supportsDependencies = true)

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "XR10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.print("$t${t}long $FUNCTION_ADDRESS = ")
        writer.println(if (function.has<Capabilities>())
            "${function.get<Capabilities>().expression}.${function.name};"
        else
            "${function.getParams { it.nativeType is WrappedPointerType }.first().name}.getCapabilities().${function.name};"
        )
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass, commands: Map<String, Int>) {
        printExtCheckMethod(
            checks, nativeClass, nativeClass.capName, { it.isInstanceFunction }, commands,
            buildDependencyFlags(nativeClass.functions.filter { it.isInstanceFunction && it.has<DependsOn>() })
        )
    }

    init {
        javaImport("static org.lwjgl.system.Checks.*")

    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("@SuppressWarnings(\"SimplifiableIfStatement\")")
        println("public class XRCapabilities {")

        val classes = super.getClasses("XR")

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
                    println("\n$t// ${it.templateName}")
                    println("${t}public final long")
                    println("$t$t$functions;")
                }
            }

        println(
            """
    /** The OpenXR API version number. */
    public final long apiVersion;
"""
        )

        classes
            .forEach {
                println(it.getCapabilityJavadoc())
                println("${t}public final boolean ${it.capName};")
            }

        if (classes.any { isReducedCheck(it, { func -> func.isInstanceFunction }, instanceCommands) }) printCheckHelper(checks)

        print(
            """
    XRCapabilities(FunctionProvider provider, long apiVersion, Set<String> ext) {
        this.apiVersion = apiVersion;

        long[] caps = new long[${instanceCommands.size}];
        MemoryStack stack = MemoryStack.stackGet();
"""
        )

        classes.forEach {
            val capName = it.capName
            if (it.functions.any { func -> func.isInstanceFunction }) {
                printCapCheck(this, checks, it, capName, "ext", { func -> func.isInstanceFunction }, instanceCommands, { "check_${it.templateName}(stack, provider, caps, ext)" })
            } else {
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

// DSL Extensions

val GlobalCommand = Capabilities("XR.getGlobalCommands()")

fun String.nativeClassXR(
    templateName: String,
    type: String,
    prefix: String = "XR",
    prefixMethod: String = prefix.lowercase(),
    postfix: String = "",
    init: (NativeClass.() -> Unit)? = null
): NativeClass {
    check(type == "instance")
    return nativeClass(
        Module.OPENXR,
        templateName,
        prefix = prefix,
        prefixMethod = prefixMethod,
        postfix = postfix,
        binding = XR_BINDING_INSTANCE,
        init = init
    )
}