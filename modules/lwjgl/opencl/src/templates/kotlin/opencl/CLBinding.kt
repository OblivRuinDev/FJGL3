/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package opencl

import org.lwjgl.generator.*
import java.io.*

private val NativeClass.capName: String
    get() = if (templateName.startsWith(prefix)) {
        if (prefix == "CL")
            "OpenCL${templateName.substring(2)}"
        else
            templateName
    } else {
        "${prefixTemplate}_$templateName"
    }

private const val CAPABILITIES_CLASS = "CLCapabilities"

private val CLBinding = Generator.register(object : APIBinding(
    Module.OPENCL,
    CAPABILITIES_CLASS,
    APICapabilities.JAVA_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("CL") }

    private val addresses by lazy { classes.getFunctionPointers() }

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "CL10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = CL.getICD().${function.name};")
    }

    /** Prints [items] comma-separated with the same line-wrapping style as [NativeClass.printPointers]. */
    private fun PrintWriter.printWrapped(items: List<String>) {
        print("\n${t}${t}${t}")
        var lineSize = 12
        items.forEachIndexed { index, item ->
            if (0 < index) {
                print(", ")
                lineSize += 2
            }
            lineSize += item.length
            if (160 <= lineSize) {
                print("\n${t}${t}${t}")
                lineSize = 12 + item.length
            }
            print(item)
        }
        print("\n${t}${t}")
    }

    init {
        javaImport(
            "static org.lwjgl.system.APIUtil.*",
            "static org.lwjgl.system.Checks.*"
        )
    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("public class $CAPABILITIES_CLASS {")

        println("\n${t}public final long")
        println(addresses.joinToString(",\n$t$t", prefix = "$t$t", postfix = ";\n", transform = Func::name))

        classes.forEach {
            println(it.getCapabilityJavadoc())
            println("${t}public final boolean ${it.capName};")
        }

        // ICD/Platform constructor
        println("\n$t$CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext) {")
        print("$t${t}this(ext, provider.getFunctionAddresses(")
        printWrapped(addresses.map { it.functionAddress })
        print("));\n    }\n")

        // Device constructor
        println("\n$t$CAPABILITIES_CLASS(CLCapabilities caps, Set<String> ext) {")
        println(addresses.joinToString(",\n$t$t$t", prefix = "$t${t}this(ext,\n$t$t$t", postfix = "\n$t$t);\n$t}") { "caps.${it.name}" })

        // Common constructor
        println("\n${t}private $CAPABILITIES_CLASS(Set<String> ext, long... functions) {")
        println(addresses.mapIndexed { i, function -> "${function.name} = functions[$i];" }.joinToString("\n$t$t", prefix = "$t$t"))
        for (extension in classes) {
            val capName = extension.capName
            if (extension.hasNativeFunctions) {
                print("\n$t$t$capName = check(\"$capName\", ext")
                // A native class of macro functions only has no entry points to verify; the empty function list is skipped to avoid a trailing comma.
                if (extension.functions.any { !it.has<Macro>() }) {
                    print(",")
                    extension.printPointers(this)
                }
                println(");")
            } else {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }
        print("""
    }

    private boolean check(String extName, Set<String> ext, long... functions) {
        if (!ext.contains(extName)) {
            return false;
        }
        if (checkFunctions(functions)) {
            return true;
        }
        apiLog("[CL] " + ext + " was reported as available but an entry point is missing.");
        return false;
    }
    private boolean check(String extName, Set<String> ext, long function) {
        if (!ext.contains(extName)) {
            return false;
        }
        if (function != MemoryUtil.NULL) {
            return true;
        }
        apiLog("[CL] " + ext + " was reported as available but an entry point is missing.");
        return false;
    }
}""")
    }

})

// DSL Extensions

fun String.nativeClassCL(templateName: String, postfix: String = "", cinitSetRTConst: Boolean = true, init: (NativeClass.() -> Unit)? = null) =
    nativeClass(Module.OPENCL, templateName, prefix = "CL", postfix = postfix, prefixTemplate = "cl", binding = CLBinding, cinitSetRTConst = cinitSetRTConst, init = init)
