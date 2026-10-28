/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package opengles

import org.lwjgl.generator.*
import org.lwjgl.generator.Generator.Companion.register
import java.io.*

val NativeClass.capName: String
    get() = if (templateName.startsWith(prefixTemplate)) templateName else "${prefixTemplate}_$templateName"

private const val CAPABILITIES_CLASS = "GLESCapabilities"

private val GLESBinding = register(object : AbstractBinding(
    Module.OPENGLES,
    CAPABILITIES_CLASS,
    APICapabilities.JNI_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("GLES") }

    private val functions by lazy { classes.getFunctionPointers() }

    private val checks = CapsChecks("MemoryStack stack, FunctionProvider provider, PointerBuffer caps, Set<String> ext", "stack, provider, caps", "GLES", supportsDependencies = true)

    override val ordinalFunctions by lazy {
        classes.asSequence()
            .filter { it.hasNativeFunctions }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Macro>() }
            .toList()
    }

    override fun getFunctionOrdinal(function: Func) = functionOrdinals[function.name]!!

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "GLES20"

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = GLES.getICD().${function.name};")
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass) {
        printExtCheckMethod(
            checks, nativeClass, nativeClass.capName, { it -> !it.has(IgnoreMissing) }, functionOrdinals,
            buildDependencyFlags(nativeClass.functions.filter { it.has<DependsOn>() })
        )
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
        println("""public final class $CAPABILITIES_CLASS {

    static final int ADDRESS_BUFFER_SIZE = ${functions.size};""")

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
    /** Off-heap array of the above function addresses. */
    final PointerBuffer addresses;

    $CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext, IntFunction<PointerBuffer> bufferFactory) {
        PointerBuffer caps = bufferFactory.apply(ADDRESS_BUFFER_SIZE);
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
        printCapsAssignments(this, functionOrdinals) { "caps.get($it)" }

        print("""

        addresses = ThreadLocalUtil.setupAddressBuffer(caps);
    }

    /** Returns the buffer of OpenGL ES function pointers. */
    public PointerBuffer getAddressBuffer() {
        return addresses;
    }
""")

        for (extension in classes) {
            if (!extension.hasNativeFunctions) {
                continue
            }

            checkExtensionFunctions(extension)
        }

        println("""
    private static boolean hasDSA(Set<String> ext) {
        return ext.contains("GL_ARB_direct_state_access") || ext.contains("GL_EXT_direct_state_access");
    }

}""")
    }

})

fun String.nativeClassGLES(
    templateName: String,
    nativeSubPath: String = "",
    prefix: String = "GL",
    prefixMethod: String = prefix.lowercase(),
    postfix: String = "",
    init: (NativeClass.() -> Unit)? = null
) = nativeClass(
    Module.OPENGLES,
    templateName,
    nativeSubPath = nativeSubPath,
    prefix = prefix,
    prefixMethod = prefixMethod,
    postfix = postfix,
    binding = GLESBinding,
    init = {
        if (init != null) {
            init()
        }
        if (!skipNative) {
            nativeImport("opengles.h")
        }
    }
)