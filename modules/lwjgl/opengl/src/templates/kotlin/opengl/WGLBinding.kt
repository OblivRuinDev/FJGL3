/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package opengl

import org.lwjgl.generator.*
import java.io.*

private const val CAPABILITIES_CLASS = "WGLCapabilities"

val WGLBinding = Generator.register(object : AbstractBinding(
    Module.OPENGL,
    CAPABILITIES_CLASS,
    APICapabilities.JAVA_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("WGL") }

    private val functions by lazy { classes.getFunctionPointers() }

    private val checks = CapsChecks("MemoryStack stack, FunctionProvider provider, long[] caps, Set<String> ext", "stack, provider, caps", "WGL")

    override val ordinalFunctions by lazy {
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Macro>() }
            .toList()
    }

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = GL.getCapabilitiesWGL().${function.name};")
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass) {
        printExtCheckMethod(checks, nativeClass, nativeClass.capName, { it -> !it.has(IgnoreMissing) }, functionOrdinals)
    }

    init {
        javaImport(
            "static org.lwjgl.system.APIUtil.*",
            "static org.lwjgl.system.Checks.*"
        )

    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("public final class $CAPABILITIES_CLASS {")

        val functionSet = LinkedHashSet<String>()
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .forEach {
                val functions = it.functions.asSequence()
                    .filter { cmd ->
                        if (!cmd.has<Macro>()) {
                            if (functionSet.add(cmd.name)) {
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

        println()

        classes.forEach {
            println(it.getCapabilityJavadoc())
            println("${t}public final boolean ${it.capName};")
        }

        if (classes.any { isReducedCheck(it, { it -> !it.has(IgnoreMissing) }, functionOrdinals) }) printCheckHelper(checks)

        print("""
    $CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext) {
        long[] caps = new long[${functions.size}];
        MemoryStack stack = MemoryStack.stackGet();
""")

        for (extension in classes) {
            val capName = extension.capName
            if (extension.hasNativeFunctions) {
                printCapCheck(this, checks, extension, capName, "ext", { it -> !it.has(IgnoreMissing) }, functionOrdinals, { "check_${it.templateName}(stack, provider, caps, ext)" })
            } else {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }

        println()
        printCapsAssignments(this, functionOrdinals) { "caps[$it]" }
        print("""
    }
""")

        for (extension in classes) {
            if (!extension.hasNativeFunctions) {
                continue
            }

            checkExtensionFunctions(extension)
        }

        println("\n}")
    }

})

fun String.nativeClassWGL(templateName: String, postfix: String = "", init: (NativeClass.() -> Unit)? = null) = nativeClass(
    Module.OPENGL,
    templateName,
    prefix = "WGL",
    postfix = postfix,
    binding = WGLBinding,
    init = init
)