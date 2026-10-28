/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package egl

import org.lwjgl.generator.*
import java.io.*

val NativeClass.capName: String
    get() = if (templateName.startsWith(prefixTemplate)) templateName else "${prefixTemplate}_$templateName"

private const val CAPABILITIES_CLASS = "EGLCapabilities"

private val EGLBinding = Generator.register(object : APIBinding(
    Module.EGL,
    CAPABILITIES_CLASS,
    APICapabilities.JAVA_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("EGL") }

    private val addresses by lazy { classes.getFunctionPointers() }

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "CL10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = EGL.getCapabilities().${function.name};")
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

        // Client constructor
        print("\n$t$CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext) {\n")
        print("$t${t}this(ext, provider.getFunctionAddresses(")
        printWrapped(addresses.map { it.functionAddress })
        print("));\n    }\n")

        // Display constructor
        println("\n$t$CAPABILITIES_CLASS(EGLCapabilities caps, Set<String> ext) {")
        println(addresses.joinToString(",\n$t$t$t", prefix = "$t${t}this(ext,\n$t$t$t", postfix = "\n$t$t);\n$t}") { "caps.${it.name}" })

        // Common constructor
        println("\n${t}private $CAPABILITIES_CLASS(Set<String> ext, long... functions) {")
        print(addresses.mapIndexed { i, function -> "${function.name} = functions[$i];" }.joinToString("\n$t$t", prefix = "$t$t", postfix = "\n"))
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
        println("\n${t}}")

        println("""

    private boolean check(String extension, Set<String> ext, long... functions) {
        if (!ext.contains(extension)) {
            return false;
        }
        if (checkFunctions(functions)) {
            return true;
        }
        apiLog("[CL] " + extension + " was reported as available but an entry point is missing.");
        return false;
    }
    private boolean check(String extension, Set<String> ext, long function) {
        if (!ext.contains(extension)) {
            return false;
        }
        if (function != MemoryUtil.NULL) {
            return true;
        }
        apiLog("[CL] " + extension + " was reported as available but an entry point is missing.");
        return false;
    }
}""")
    }

})

fun String.nativeClassEGL(
    templateName: String,
    nativeSubPath: String = "",
    prefix: String = "EGL",
    prefixMethod: String = prefix.lowercase(),
    postfix: String = "",
    init: (NativeClass.() -> Unit)? = null
) = nativeClass(
    Module.EGL,
    templateName,
    nativeSubPath = nativeSubPath,
    prefix = prefix,
    prefixMethod = prefixMethod,
    postfix = postfix,
    binding = EGLBinding,
    init = init
)
