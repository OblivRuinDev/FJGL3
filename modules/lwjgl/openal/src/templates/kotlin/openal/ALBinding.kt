/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package openal

import org.lwjgl.generator.*
import java.io.*

private const val CAPABILITIES_CLASS = "ALCapabilities"

private val ALBinding = Generator.register(object : AbstractBinding(
    Module.OPENAL,
    CAPABILITIES_CLASS,
    APICapabilities.JAVA_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("AL") }

    private val functions by lazy { classes.getFunctionPointers() }

    private val checks = CapsChecks("MemoryStack stack, FunctionProvider provider, PointerBuffer caps, Set<String> ext", "stack, provider, caps", "AL")

    override val ordinalFunctions by lazy {
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Macro>() }
            .toList()
    }

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "AL10"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = AL.getICD().${function.name};")
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass) {
        printExtCheckMethod(checks, nativeClass, nativeClass.capName("AL"), { !it.has(IgnoreMissing) }, functionOrdinals)
    }

    init {
        javaImport(
            "org.lwjgl.*",
            "java.util.function.IntFunction",
            "static org.lwjgl.system.APIUtil.*",
            "static org.lwjgl.system.Checks.*"
        )

    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("""public final class $CAPABILITIES_CLASS {""")

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
            println("${t}public final boolean ${it.capName("AL")};")
        }

        if (classes.any { isReducedCheck(it, { !it.has(IgnoreMissing) }, functionOrdinals) }) printCheckHelper(checks)

        print("""
    /** Off-heap array of the above function addresses. */
    final PointerBuffer addresses;

    $CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext, IntFunction<PointerBuffer> bufferFactory) {
        PointerBuffer caps = bufferFactory.apply(${functions.size});
        MemoryStack stack = MemoryStack.stackGet();
""")

        for (extension in classes) {
            val capName = extension.capName("AL")
            if (extension.hasNativeFunctions) {
                printCapCheck(this, checks, extension, capName, "ext", { !it.has(IgnoreMissing) }, functionOrdinals, { "check_${it.templateName}(stack, provider, caps, ext)" })
            } else {
                print("\n$t$t$capName = ext.contains(\"$capName\");")
            }
        }

        println()
        printCapsAssignments(this, functionOrdinals) { "caps.get($it)" }

        print("""

        addresses = ThreadLocalUtil.setupAddressBuffer(caps);
    }

    /** Returns the buffer of OpenAL function pointers. */
    public PointerBuffer getAddressBuffer() {
        return addresses;
    }
""")

        for (extension in classes) {
            if (extension.hasNativeFunctions) {
                checkExtensionFunctions(extension)
            }
        }

        println("\n}")
    }

})

// DSL Extensions

fun String.nativeClassAL(templateName: String, prefixTemplate: String = "AL", postfix: String = "", init: (NativeClass.() -> Unit)? = null) =
    nativeClass(Module.OPENAL, templateName, prefix = "AL", prefixTemplate = prefixTemplate, postfix = postfix, binding = ALBinding, init = init)

private val DIRECT_CONTEXT = arrayOf(Parameter(ALCcontext.p, "context"))
fun Func.directContext() {
    val postfix = this.nativeClass.postfix
    val name = if (postfix.isEmpty())
        "${name}Direct"
    else
        "${name.substring(0, name.length - postfix.length)}Direct$postfix"

    this.nativeClass.addFunction(name, DependsOn("AL_EXT_direct_context")..Func(
        returns = this.returns,
        simpleName = name,
        name = name,
        nativeClass = this.nativeClass,
        parameters = DIRECT_CONTEXT + this.parameters
    ))
}