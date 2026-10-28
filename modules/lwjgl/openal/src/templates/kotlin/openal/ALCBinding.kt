/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package openal

import org.lwjgl.generator.*
import java.io.*

fun NativeClass.capName(core: String) =
    if (templateName.startsWith(prefixTemplate)) {
        if (prefix == core)
            "Open$core${templateName.substring(core.length)}"
        else
            templateName
    } else {
        "${prefixTemplate}_$templateName"
    }

private val NativeClass.isCore: Boolean get() = templateName.startsWith("ALC")

private const val ALC_CAP_CLASS = "ALCCapabilities"

val ALCBinding = Generator.register(object : AbstractBinding(
    Module.OPENAL,
    ALC_CAP_CLASS,
    APICapabilities.JAVA_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("ALC") }

    private val functions by lazy {
        classes.getFunctionPointers {
            it.hasNativeFunctions && it.prefix == "ALC"
        }
    }

    private val checks = CapsChecks("MemoryStack stack, FunctionProviderLocal provider, long device, PointerBuffer caps, Set<String> ext", "stack, provider, device, caps", "ALC")

    override val ordinalFunctions by lazy {
        classes.asSequence()
            .filter { it.hasNativeFunctions && it.prefix == "ALC" }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Macro>() }
            .toList()
    }

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "ALC10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("\t\tlong $FUNCTION_ADDRESS = ALC.getICD().${function.name};")
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass) {
        printExtCheckMethod(checks, nativeClass, nativeClass.capName("ALC"), { !it.has(IgnoreMissing) }, functionOrdinals)
    }

    init {
        javaImport(
            "org.lwjgl.*",
            "java.util.function.IntFunction",
            "static org.lwjgl.system.Checks.*"
        )

    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("public final class $ALC_CAP_CLASS {")

        val functionSet = LinkedHashSet<String>()
        classes.asSequence()
            .filter { it.hasNativeFunctions && it.prefix == "ALC" }
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
            println("${t}public final boolean ${it.capName("ALC")};")
        }

        if (classes.any { it.hasNativeFunctions && it.prefix == "ALC" && isReducedCheck(it, { !it.has(IgnoreMissing) }, functionOrdinals) }) printCheckHelper(checks)

        print(
            """
    /** Device handle. */
    final long device;

    /** Off-heap array of the above function addresses. */
    final PointerBuffer addresses;

    $ALC_CAP_CLASS(FunctionProviderLocal provider, long device, Set<String> ext, IntFunction<PointerBuffer> bufferFactory) {
        this.device = device;

        PointerBuffer caps = bufferFactory.apply(${functions.size});
        MemoryStack stack = MemoryStack.stackGet();
"""
        )

        for (extension in classes) {
            val capName = extension.capName("ALC")
            if (extension.hasNativeFunctions && extension.prefix == "ALC") {
                printCapCheck(this, checks, extension, capName, "ext", { !it.has(IgnoreMissing) }, functionOrdinals, { "check_${it.templateName}(stack, provider, device, caps, ext)" })
            } else {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }

        println()
        printCapsAssignments(this, functionOrdinals) { "caps.get($it)" }

        print(
            """

        addresses = ThreadLocalUtil.setupAddressBuffer(caps);
    }

    /** Returns the buffer of OpenAL function pointers. */
    public PointerBuffer getAddressBuffer() {
        return addresses;
    }
"""
        )

        for (extension in classes) {
            if (extension.hasNativeFunctions && extension.prefix == "ALC") {
                checkExtensionFunctions(extension)
            }
        }

        println("\n}")
    }

})

// DSL Extensions

fun String.nativeClassALC(templateName: String, prefix: String = "ALC", postfix: String = "", init: (NativeClass.() -> Unit)? = null) =
    nativeClass(Module.OPENAL, templateName, prefix = prefix, prefixTemplate = "ALC", postfix = postfix, binding = ALCBinding, init = init)