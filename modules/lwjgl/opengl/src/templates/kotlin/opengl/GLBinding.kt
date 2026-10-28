/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package opengl

import org.lwjgl.generator.*
import java.io.*

val NativeClass.capName: String
    get() = if (templateName.startsWith(prefixTemplate)) {
        if (prefix == "GL")
            "OpenGL${templateName.substring(2)}"
        else
            templateName
    } else {
        "${prefixTemplate}_$templateName"
    }

private val CORE_PATTERN = "GL\\d\\dC".toRegex()
val NativeClass.isCore: Boolean
    get() = CORE_PATTERN.matches(templateName)

private const val CAPABILITIES_CLASS = "GLCapabilities"

private val checks = CapsChecks("MemoryStack stack, FunctionProvider provider, PointerBuffer caps, Set<String> ext", "stack, provider, caps", "GL", supportsDependencies = true)

val GLBinding = Generator.register(object : AbstractBinding(
    Module.OPENGL,
    CAPABILITIES_CLASS,
    APICapabilities.JNI_CAPABILITIES
) {

    private val classes by lazy { super.getClasses("GL") }

    private val functions by lazy {
        classes
            .asSequence()
            .filter { it.hasNativeFunctions }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Reuse>() }
            .toList()
    }

    override val ordinalFunctions by lazy {
        classes.asSequence()
            .filter { !it.isCore && it.hasNativeFunctions }
            .flatMap { it.functions.asSequence() }
            .filter { !it.has<Macro>() }
            .toList()
    }

    override fun getFunctionOrdinal(function: Func) = functionOrdinals[function.name]!!

    private val Sequence<Func>.hasDeprecated: Boolean
        get() = this.any { it has DeprecatedGL }

    override fun shouldCheckFunctionAddress(function: Func): Boolean = function.nativeClass.templateName != "GL11" || function has DeprecatedGL

    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long $FUNCTION_ADDRESS = GL.getICD().${function.name};")
    }

    private fun PrintWriter.checkExtensionFunctions(nativeClass: NativeClass) {
        if (nativeClass.isCore) {
            return
        }
        if (!nativeClass.functions.hasDeprecated && isReducedCheck(nativeClass, { !it.has(IgnoreMissing) }, functionOrdinals, checks.supportsDependencies)) {
            // The reduced forms are inlined in the class initializer.
            return
        }

        val capName = nativeClass.capName
        val hasDeprecated = nativeClass.functions.hasDeprecated

        print("\n${t}private static boolean check_${nativeClass.templateName}(MemoryStack stack, FunctionProvider provider, PointerBuffer caps, Set<String> ext")
        if (hasDeprecated) print(", boolean fc")
        print(""") {
        if (!ext.contains("$capName")) {
            return false;
        }""")
        val dependencies = buildDependencyFlags(nativeClass.functions.filter { it.has<DependsOn>() }) { func ->
            val expr = getDependencyExpression(func)
            if (func has DeprecatedGL)
                "!fc || ${if (expr.contains(' ')) "($expr)" else expr}"
            else
                expr
        }
        printDependencyFlags(dependencies)

        print("\n\n$t${t}return (")
        if (hasDeprecated) {
            print("(fc || ")
            printFunctionChecks(nativeClass, "stack, provider, caps", functionOrdinals, filter = { it has DeprecatedGL && !it.has<DependsOn>() })
            print(") && ")
        }
        printFunctionChecks(nativeClass, "stack, provider, caps", functionOrdinals, if (hasDeprecated)
            { it -> (!it.has(DeprecatedGL) || it.has<DependsOn>()) && !it.has(IgnoreMissing) }
        else
            { it -> !it.has(IgnoreMissing) }) { func ->
            val index = functionOrdinals[func.name]
            if (func.has<DependsOn>()) {
                val expression = getDependencyExpression(func).let {
                    if (func has DeprecatedGL) "!fc || ${if (it.contains(' ')) "($it)" else it}" else it
                }
                "flag${dependencies[expression]} + $index"
            } else {
                null
            }
        }
        println(") || reportMissing(\"GL\", \"$capName\");")
        println("$t}")
    }

    private val presentCores = HashMap<String, String>()

    private fun checkExtensionPresent(core: String, extension: String) {
        presentCores[extension] = core
    }

    private val PRESENT_CALL = "([A-Za-z]\\w*)\\(ext\\)".toRegex()

    override fun getDependencyExpression(func: Func): String {
        val expr = super.getDependencyExpression(func)
        // Inline the checkExtensionPresent gates of promoted functions.
        return PRESENT_CALL.replace(expr) { m ->
            val core = presentCores[m.groupValues[1]] ?: return@replace m.value
            "ext.contains(\"OpenGL$core\") || ext.contains(\"GL_${m.groupValues[1]}\")"
        }
    }

    init {
        javaImport(
            "org.lwjgl.*",
            "java.util.function.IntFunction",
            "static org.lwjgl.system.APIUtil.*",
            "static org.lwjgl.system.Checks.*",
            "static org.lwjgl.system.MemoryUtil.*"
        )

        // TODO: some are unused
        checkExtensionPresent("30", "ARB_framebuffer_object")
        checkExtensionPresent("30", "ARB_map_buffer_range")
        checkExtensionPresent("30", "ARB_vertex_array_object")
        checkExtensionPresent("31", "ARB_copy_buffer")
        checkExtensionPresent("31", "ARB_texture_buffer_object") // TextureBuffer
        checkExtensionPresent("31", "ARB_uniform_buffer_object") // TransformFeedbackBufferBase, TransformFeedbackBufferRange
        checkExtensionPresent("33", "ARB_instanced_arrays")
        checkExtensionPresent("33", "ARB_sampler_objects")
        checkExtensionPresent("40", "ARB_transform_feedback2")
        checkExtensionPresent("41", "ARB_vertex_attrib_64bit")
        checkExtensionPresent("41", "ARB_separate_shader_objects")
        checkExtensionPresent("42", "ARB_texture_storage")
        checkExtensionPresent("43", "ARB_texture_storage_multisample")
        checkExtensionPresent("43", "ARB_vertex_attrib_binding")
        checkExtensionPresent("43", "ARB_invalidate_subdata")
        checkExtensionPresent("43", "ARB_texture_buffer_range")
        checkExtensionPresent("43", "ARB_clear_buffer_object")
        checkExtensionPresent("43", "ARB_framebuffer_no_attachments")
        checkExtensionPresent("44", "ARB_buffer_storage")
        checkExtensionPresent("44", "ARB_clear_texture")
        checkExtensionPresent("44", "ARB_multi_bind")
        checkExtensionPresent("44", "ARB_query_buffer_object")
    }

    override fun PrintWriter.generateJava() {
        generateJavaPreamble()
        println("""public final class $CAPABILITIES_CLASS {

    static final int ADDRESS_BUFFER_SIZE = ${functions.size};""")

        val functionSet = LinkedHashSet<String>()
        classes.asSequence()
            .filter { !it.isCore && it.hasNativeFunctions }
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
        classes.asSequence()
            .filter { !it.isCore }
            .forEach {
                println(it.getCapabilityJavadoc())
                println("${t}public final boolean ${it.capName};")
            }

        if (classes.any { !it.isCore && it.hasNativeFunctions && !it.functions.hasDeprecated && isReducedCheck(it, { !it.has(IgnoreMissing) }, functionOrdinals, checks.supportsDependencies) }) printCheckHelper(checks)

        println("""
    /** When true, deprecated functions are not available. */
    public final boolean forwardCompatible;

    /** Off-heap array of the above function addresses. */
    final PointerBuffer addresses;

    $CAPABILITIES_CLASS(FunctionProvider provider, Set<String> ext, boolean fc, IntFunction<PointerBuffer> bufferFactory) {
        forwardCompatible = fc;

        PointerBuffer caps = bufferFactory.apply(ADDRESS_BUFFER_SIZE);
        MemoryStack stack = MemoryStack.stackGet();""")

        for (extension in classes) {
            if (extension.isCore) {
                continue
            }
            val capName = extension.capName
            if (extension.hasNativeFunctions) {
                if (!extension.functions.hasDeprecated && isReducedCheck(extension, { !it.has(IgnoreMissing) }, functionOrdinals, checks.supportsDependencies)) {
                    print("\n$t$t$capName = ")
                    printReducedCheckCall(checks, extension, capName, "ext", { !it.has(IgnoreMissing) }, functionOrdinals)
                    print(";")
                } else {
                    print("\n$t$t$capName = check_${extension.templateName}(stack, provider, caps, ext")
                    if (extension.functions.hasDeprecated) print(", fc")
                    print(");")
                }
            } else
                print("\n$t$t$capName = ext.contains(\"$capName\");")
        }

        println()
        printCapsAssignments(this, functionOrdinals) { "caps.get($it)" }

        print("""

        addresses = ThreadLocalUtil.setupAddressBuffer(caps);
    }

    /** Returns the buffer of OpenGL function pointers. */
    public PointerBuffer getAddressBuffer() {
        return addresses;
    }

    /** Ensures that the lwjgl_opengl shared library has been loaded. */
    public static void initialize() {
        // intentionally empty to trigger static initializer
    }
""")

        for (extension in classes) {
            if (extension.isCore || !extension.hasNativeFunctions) {
                continue
            }

            checkExtensionFunctions(extension)
        }

        println("""${t}private static boolean hasDSA(Set<String> ext) {
        return ext.contains("GL45") || ext.contains("GL_ARB_direct_state_access") || ext.contains("GL_EXT_direct_state_access");
    }
""")

        println("\n}")
    }

})

// DSL Extensions

fun String.nativeClassGL(
    templateName: String,
    prefix: String = "GL",
    prefixMethod: String = prefix.lowercase(),
    postfix: String = "",
    init: (NativeClass.() -> Unit)? = null
) = nativeClass(
    Module.OPENGL,
    templateName,
    prefix = prefix,
    prefixMethod = prefixMethod,
    postfix = postfix,
    binding = GLBinding,
    init = {
        if (init != null) {
            init()
        }
        if (!skipNative) {
            nativeImport("opengl.h")
        }
    }
)