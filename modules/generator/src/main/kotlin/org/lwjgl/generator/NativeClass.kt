/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.generator

import java.io.*
import java.nio.file.*
import kotlin.math.*

const val EXT_FLAG = ""

enum class CallingConvention(val method: String) {
    DEFAULT("invoke"),
    @Deprecated(message = "Windows x86 is not supported")
    STDCALL("call") // __stdcall on Windows, default on other systems
}

enum class APICapabilities {
    NONE,
    JNI_CAPABILITIES,
    JAVA_CAPABILITIES,
    PARAM_CAPABILITIES
}

/**
 * The Generator can be customized with binding-specific overrides using this class. This class must implemented for bindings that are loaded dynamically. It
 * is not necessary for libraries that are static compiled/linked into the LWJGL natives.
 */
abstract class APIBinding(
    module: Module,
    className: String,
    val apiCapabilities: APICapabilities = APICapabilities.NONE
) : GeneratorTarget(module, className) {

    init {
        javaImport(
            "org.lwjgl.system.*",
            "java.util.Set"
        )
    }

    private val _classes: MutableList<NativeClass> = ArrayList()

    fun getClasses(corePrefix: String): List<NativeClass> {
        val classes = ArrayList(_classes)
        classes.sortWith { o1, o2 ->
            // Core functionality first, extensions after
            val isCore1 = o1.templateName.startsWith(corePrefix)
            val isCore2 = o2.templateName.startsWith(corePrefix)

            if (isCore1 xor isCore2)
                (if (isCore1) -1 else 1)
            else
                o1.templateName.compareTo(o2.templateName, ignoreCase = true)
        }
        return classes
    }

    protected fun List<NativeClass>.getFunctionPointers(predicate: (NativeClass) -> Boolean = { it.hasNativeFunctions }) = this.asSequence()
        .filter(predicate)
        .flatMap { it.functions.asSequence() }
        .filter { !it.has<Reuse>() && !it.has<Macro>() }
        .toList()

    fun addClass(clazz: NativeClass) {
        _classes.add(clazz)
    }

    // For NativeClass, we use the reflected method to retrieve the containing file.
    // If there are multiple files that contribute to the NativeClass definition,
    // the last modified time is the most recent of all files.
    override fun getSourceFileName(): String? = null
    override fun getLastModified(root: String, fileName: String): Long = max(
        super.getLastModified(root, fileName),
        Paths.get(root, "templates").lastModified()
    )

    abstract fun generateFunctionAddress(writer: PrintWriter, function: Func)

    // OVERRIDES

    open fun getFunctionOrdinal(function: Func): Int = 0

    /** Can be overridden to generate binding-specific alternative methods. */
    internal open fun generateAlternativeMethods(
        writer: PrintWriter,
        function: Func,
        transforms: MutableMap<QualifiedType, Transform>
    ) = Unit

    /** Can be overridden to implement a custom condition for checking the function address. */
    open fun shouldCheckFunctionAddress(function: Func) = apiCapabilities.ordinal > 1 && !function.hasExplicitFunctionAddress

    /** Can be overridden to add custom parameter checks. */
    open fun addParameterChecks(
        checks: MutableList<String>,
        mode: GenerationMode,
        parameter: Parameter,
        hasTransform: Parameter.(FunctionTransform<Parameter>) -> Boolean
    ) = Unit

}

/** An APIBinding without an associated capabilities class.  */
abstract class SimpleBinding(
    module: Module,
    private val libraryExpression: String
) : APIBinding(module, "*") { // TODO
    override fun PrintWriter.generateJava() = Unit
    override fun generateFunctionAddress(writer: PrintWriter, function: Func) {
        writer.println("$t${t}long ${if (function has Address) RESULT else FUNCTION_ADDRESS} = Functions.${function.simpleName};")
    }

    abstract fun generateFunctionSetup(writer: PrintWriter, nativeClass: NativeClass)

    protected fun PrintWriter.generateFunctionsClass(nativeClass: NativeClass, javadoc: String) {
        val bindingFunctions = nativeClass.functions.filter { !it.hasExplicitFunctionAddress && !it.has<Macro>() }
        if (bindingFunctions.none())
            return

        print(javadoc)

        val alignment = bindingFunctions.maxOf { it.simpleName.length }

        println("""
    public static final class Functions {

        private Functions() {}

        /** Function address. */
        public static final long
            ${bindingFunctions.joinToString(separator = ",\n$t$t$t", postfix = ";") {
            "${it.simpleName}${" ".repeat(alignment - it.simpleName.length)} = ${if (it has IgnoreMissing)
                "apiGetFunctionAddressOptional($libraryExpression, ${it.functionAddress})"
            else
                "apiGetFunctionAddress($libraryExpression, ${it.functionAddress})"}"
        }}

    }""")
    }
}

/** Creates a simple APIBinding that stores the shared library and function pointers inside the binding class. The shared library is never unloaded. */
fun simpleBinding(
    module: Module,
    libraryName: String = module.name.lowercase(),
    libraryExpression: String = "\"$libraryName\"",
    bundledWithLWJGL: Boolean = false,
    preamble: String? = null
) = object : SimpleBinding(module, libraryName.uppercase()) {
    // TODO: Sync HARFBUZZ_BINDING if this changes
    override fun generateFunctionSetup(writer: PrintWriter, nativeClass: NativeClass) {
        val libraryReference = libraryName.uppercase()

        with(writer) {
            if (preamble != null) {
                println(preamble)
            }
            println("\n${t}private static final SharedLibrary $libraryReference = Library.loadNative(${nativeClass.className}.class, \"${module.java}\", $libraryExpression${if (bundledWithLWJGL) ", true" else ""});")
            generateFunctionsClass(nativeClass, "\n$t/** Contains the function pointers loaded from the $libraryName {@link SharedLibrary}. */")
            println("""
    /** Returns the $libraryName {@link SharedLibrary}. */
    public static SharedLibrary getLibrary() {
        return $libraryReference;
    }""")
        }
    }
}

/** Creates a simple APIBinding that delegates function pointer loading to this APIBinding. */
fun APIBinding.delegate(
    libraryExpression: String
) = object : SimpleBinding(module, libraryExpression) {
    override fun generateFunctionSetup(writer: PrintWriter, nativeClass: NativeClass) {
        writer.generateFunctionsClass(nativeClass, "\n$t/** Contains the function pointers loaded from {@code $libraryExpression}. */")
    }
}

class NativeClass internal constructor(
    module: Module,
    className: String,
    nativeSubPath: String,
    val templateName: String = className,
    val prefix: String,
    val prefixMethod: String,
    val prefixConstant: String,
    val prefixTemplate: String,
    val postfix: String,
    val binding: APIBinding?,
    internal val callingConvention: CallingConvention,
    val cinitSetRTConst: Boolean = true
) : GeneratorTargetNative(module, className, nativeSubPath) {
    companion object {
        private val VOID_ARGS = Parameter(void, ANONYMOUS)
    }

    var extends: NativeClass? = null

    private val constantBlocks = ArrayList<ConstantBlock<*>>()

    /** The local variables declared in the class initializer, in declaration order: name -> (type, expression). */
    private val cinitVariables = LinkedHashMap<String, Pair<String, String>>()

    /** A constant getter that is merged into the single `initNative` JNI call, with the array type and index of its value. */
    internal class InitNativeEntry(val func: Func, val arrayType: String, val index: Int)

    /** The merged constant getters, in declaration order. Java and C generation both read this same list, so their order and indices agree. */
    internal val initNativeEntries: List<InitNativeEntry> by lazy(LazyThreadSafetyMode.NONE) {
        val counts = HashMap<String, Int>()
        // Filter the original functions, not genFunctions, which may also contain the array overloads added by registerFunctions().
        _functions.values.filter { it.isInitNativeCandidate }.map { func ->
            val arrayType = func.initNativeArrayType
            val index = counts.getOrDefault(arrayType, 0)
            counts[arrayType] = index + 1
            InitNativeEntry(func, arrayType, index)
        }
    }

    /** The primitive array parameters of `initNative`, in first-appearance order, with their element count. */
    private val initNativeArrays: List<Pair<String, Int>> by lazy(LazyThreadSafetyMode.NONE) {
        val arrays = LinkedHashMap<String, Int>()
        initNativeEntries.forEach { arrays.merge(it.arrayType, 1, Int::plus) }
        arrays.entries.map { it.key to it.value }
    }

    /**
     * The replacements that read a constant macro's value from the merged array, keyed by the generated native method name. The body of a constant macro
     * calls its native method, which is replaced by the array read.
     */
    internal val initNativeConstantReads: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        initNativeEntries
            .filter { it.func.has<Macro> { constant } }
            .associate { it.func.initNativeCallName to "${initNativeArrayName(it.arrayType)}[${it.index}]" }
    }

    /** The replacements that read a private constant getter's value from the merged array, keyed by the getter's Java method name. */
    internal val initNativeGetterReads: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        initNativeEntries
            .filter { !it.func.has<Macro> { constant } }
            .associate { it.func.name to "${initNativeArrayName(it.arrayType)}[${it.index}]" }
    }

    private val _functions = LinkedHashMap<String, Func>()
    val functions: Sequence<Func>
        get() = _functions.values.asSequence()

    // same as above + array overloads
    private val genFunctions: MutableList<Func> by lazy(LazyThreadSafetyMode.NONE) {
        ArrayList(_functions.values)
    }

    private val customMethods = ArrayList<String>()

    internal val hasBody
        get() = binding is SimpleBinding || constantBlocks.isNotEmpty() || hasNativeFunctions || customMethods.isNotEmpty()

    val hasNativeFunctions
        get() = _functions.isNotEmpty()

    val link get() = "{@link ${this.className} ${this.templateName}}"

    override fun processDocumentation(documentation: String, forcePackage: Boolean): String {
        return processDocumentation(documentation, prefixConstant, prefixMethod, forcePackage = forcePackage)
    }

    private val constantLinks: Map<String, String> by lazy(LazyThreadSafetyMode.NONE) {
        val map = HashMap<String, String>()

        constantBlocks
            .forEach { block ->
                block.constants.forEach {
                    map[it.name] = block.getClassLink(it.name)
                }
            }

        functions
            .filter { it has macro }
            .forEach {
                map[it.name] = "$className#${it.name}"
            }

        map
    }

    override fun getFieldLink(field: String): String? = constantLinks[field]
    override fun getMethodLink(method: String): String? = _functions[method].let {
        if (it == null)
            null
        else
            "$className#${it.name}()"
    }

    internal fun registerFunctions(generateArrayOverloads: Boolean) {
        functions.asSequence()
            .filter { it.critical && !it.has<Macro>() }
            .forEach {
                CriticalCall.register(it)
            }

        if (binding != null) {
            functions.asSequence()
                // This will generate additional signatures that cover the entire
                // GL/GLES API. They will not be used by LWJGL, but may be useful
                // to users. Using !it.hasCustomJNI here will eliminate them.
                .filter { !it.hasCustomJNIWithIgnoreAddress && (!it.has<Macro>() || !it.get<Macro>().function) }
                .forEach {
                    JNI.register(it)
                    // Downcall is the FFM alternative of JNI: every function without array parameters can also be invoked through it.
                    if (!it.hasParam { param -> param.nativeType is ArrayType<*> })
                        Downcall.register(it)
                }
        }

        genFunctions
        if (!generateArrayOverloads)
            return

        functions.asSequence()
            .filter(Func::hasArrayOverloads)
            .forEach { func ->
                val multiTypeParams = func.parameters.filter { it.has<MultiType>() }
                val autoSizeResultOutParams = func.parameters.count { it.isAutoSizeResultOut }

                if (multiTypeParams.isEmpty() || func.parameters.any { it.isArrayParameter(autoSizeResultOutParams) }) {
                    val overload = Func(
                        returns = func.returns,
                        simpleName = func.simpleName,
                        name = func.name,
                        nativeClass = this@NativeClass,
                        parameters = func.parameters.asSequence().map {
                            if (it.isArrayParameter(autoSizeResultOutParams))
                                it
                                    .copy(ArrayType(it.nativeType as PointerType<*>))
                                    .removeArrayModifiers()
                            else
                                func[it.name].removeArrayModifiers()
                        }.toList().toTypedArray()
                    ).copyModifiers(func)

                    if (!overload.hasCustomJNI)
                        JNI.registerArray(overload)

                    genFunctions.add(overload)
                }

                if (multiTypeParams.isEmpty())
                    return@forEach

                val multiType = multiTypeParams.first().get<MultiType>()
                multiType.types.asSequence()
                    .filter { it !== PointerMapping.DATA_POINTER }
                    .let {
                        if (multiType.byteArray)
                            sequenceOf(PointerMapping.DATA_BYTE) + it
                        else
                            it
                    }
                    .forEach { autoType ->
                        val overload = Func(
                            returns = func.returns,
                            simpleName = func.simpleName,
                            name = func.name,
                            nativeClass = this@NativeClass,
                            parameters = func.parameters.asSequence().map {
                                if (it.isArrayParameter(autoSizeResultOutParams))
                                    it
                                        .copy(ArrayType(it.nativeType as PointerType<*>))
                                        .removeArrayModifiers()
                                else if (it.has<MultiType>())
                                    it
                                        .copy(ArrayType(it.nativeType as PointerType<*>, autoType))
                                        .removeArrayModifiers()
                                        .replaceModifier<Check> { check ->
                                            Check("${check.expression.let { expression ->
                                                if (expression.contains(' ')) "($expression)" else expression
                                            }} >> ${autoType.byteShift}")
                                        }
                                else
                                    func[it.name].removeArrayModifiers()
                            }.toList().toTypedArray()
                        ).copyModifiers(func)

                        overload.parameters.asSequence().filter { param ->
                            param.has<AutoSize>() && multiTypeParams.any { param.get<AutoSize>().hasReference(it.name) }
                        }.forEach {
                            // TODO: This is correct for now, but we may want to add a flag to AutoSize for better control
                            fun getAutoSizeFactor(factor: AutoSizeFactor, byteShift: Int): AutoSizeFactor? {
                                if (factor.operator == "/")
                                    return null

                                try {
                                    val value = factor.expression.toInt() * (if (factor.operator == "<<") -1 else 1) - byteShift
                                    if (value == 0)
                                        return null

                                    return if (value < 0)
                                        AutoSizeFactor.shl("${-value}")
                                    else
                                        AutoSizeFactor.shr("$value")
                                } catch (_: NumberFormatException) {
                                    return null
                                }
                            }

                            val autoSize = it.get<AutoSize>()
                            it.replaceModifier(
                                if (autoSize.factor == null)
                                    AutoSizeShl(
                                        autoType.byteShift,
                                        autoSize.reference,
                                        *autoSize.dependent
                                    )
                                else
                                    AutoSize(
                                        autoSize.reference,
                                        *autoSize.dependent,
                                        factor = getAutoSizeFactor(autoSize.factor, autoType.byteShift.toInt())
                                    )
                            )
                        }

                        if (!overload.hasCustomJNI)
                            JNI.registerArray(overload)

                        genFunctions.add(overload)
                    }
            }
    }

    private fun registerLink(
        name: String,
        link: String,
        registry: MutableMap<String, String>,
        duplicate: MutableSet<String>
    ) {
        val prev = registry[name]
        when {
            prev == null               -> registry[name] = link
            link.length < prev.length  -> { // Short link == shorter class == usually core API
                registry[name] = link
                duplicate.remove(name) // sometimes there are more than 2 definitions of the same symbol
            }
            link.length == prev.length -> duplicate.add(name)
        }
    }

    internal fun registerLinks(
        tokens: MutableMap<String, String>,
        duplicateTokens: MutableSet<String>,
        functions: MutableMap<String, String>,
        duplicateFunctions: MutableSet<String>
    ) {
        constantBlocks.forEach { block ->
            block.constants.forEach {
                registerLink(it.name, block.getClassLink(it.name), tokens, duplicateTokens)
            }
        }

        this.functions.asSequence().filter { !it.has<Reuse>() }.forEach {
            if (it has macro)
                registerLink(it.simpleName, "$className#${it.name}", tokens, duplicateTokens)
            else
                registerLink(it.simpleName, "$className#${it.name}()", functions, duplicateFunctions)
        }
    }

    override fun PrintWriter.generateJava() {
        print(HEADER)
        println("package $packageName;\n")

        val hasFunctions = _functions.isNotEmpty()
        if (hasFunctions || binding is SimpleBinding) {
            // TODO: This is horrible. Refactor so that we build imports after code generation.
            if (functions.any {
                (it.returns.nativeType.isReference && it.returnsNull) || it.parameters.any { param ->
                    param.nativeType.isReference && param.has(nullable)
                } || it.has<MapPointer>()
            }) {
                println("import org.jspecify.annotations.*;\n")
            }

            val hasBuffers = functions.any { it.returns.nativeType.isPointerData || it.hasParam { param -> param.nativeType.isPointerData } }

            if (hasBuffers) {
                if (functions.any {
                    (it.returns.isBufferPointer && it.returns.nativeType.mapping !== PointerMapping.DATA_POINTER && it.returns.nativeType !is CharSequenceType)
                    ||
                    it.hasParam { param -> param.isBufferPointer && param.nativeType.mapping !== PointerMapping.DATA_POINTER }
                })
                    println("import java.nio.*;\n")

                val needsCustomBuffer: NativeType.() -> Boolean = {
                    this is PointerType<*> && this.elementType.run { this is PointerType<*> || (mapping == PrimitiveMapping.POINTER && this !is StructType) || mapping == PrimitiveMapping.CLONG }
                }
                if (functions.any {
                    it.returns.nativeType.needsCustomBuffer() || it.hasParam { param ->
                        param.nativeType.needsCustomBuffer() || param.has<MultiType> { types.contains(PointerMapping.DATA_POINTER) || types.contains(PointerMapping.DATA_CLONG) }
                    }
                })
                    println("import org.lwjgl.*;\n")
            }

            val functions = this@NativeClass.functions
                .filter { !it.has<Reuse>() }

            val hasLibFFI = skipNative && functions.any { (it.returns.isStructValue || it.parameters.any { param -> param.nativeType is StructType }) && !it.has<Macro>() }
            val hasMemoryStack = (hasBuffers && functions.any { func ->
                func.hasParam {
                    it.nativeType is PointerType<*> &&
                    (
                        it.has<Return>() ||
                        it.has<SingleValue>() ||
                        (it.isAutoSizeResultOut && func.hideAutoSizeResultParam) ||
                        it.has<PointerArray>() ||
                        (it.nativeType is CharSequenceType && it.isInput)
                    )
                }
            }) || hasLibFFI

            if ((hasFunctions || binding != null) && module !== Module.CORE) {
                println("import org.lwjgl.system.*;")
            }

            val staticImports = ArrayList<String>()

            if (hasFunctions) {
                if (binding is SimpleBinding || (binding != null && functions.any { it.has<MapPointer>() }) || hasLibFFI)
                    staticImports.add("org.lwjgl.system.APIUtil.*")
                if ((binding != null && binding.apiCapabilities.ordinal >= 2) || functions.any { func ->
                        func.hasParam { param ->
                            param.nativeType is PointerType<*> && (param.has<Check>() || func.getReferenceParam<AutoSize>(param.name).let {
                                if (it == null)
                                    (!param.has<Nullable>() || param.nativeType is CharSequenceType) && param.nativeType.elementType !is StructType
                                else
                                    it.get<AutoSize>().reference != param.name // dependent auto-size
                            })
                        } || (module.arrayOverloads && func.hasArrayOverloads) || (func.has<IgnoreMissing>() && binding?.apiCapabilities != APICapabilities.JNI_CAPABILITIES)
                    })
                    staticImports.add("org.lwjgl.system.Checks.*")
            }
            if (binding != null && functions.any { !it.hasCustomJNI || it.hasArrayOverloads })
                staticImports.add("org.lwjgl.system.JNI.*")
            if (hasLibFFI) {
                println("import org.lwjgl.system.libffi.*;")
                staticImports.add("org.lwjgl.system.libffi.LibFFI.*")
            }
            if (hasMemoryStack)
                staticImports.add("org.lwjgl.system.MemoryStack.*")
            if ((hasBuffers && functions.any {
                it.returns.isBufferPointer || it.hasParam { param ->
                    param.nativeType.let { type -> type is PointerType<*> && type.mapping !== PointerMapping.OPAQUE_POINTER && (type.elementType !is StructType || param.has<Nullable>()) }
                }
            }) || hasLibFFI) {
                staticImports.add("org.lwjgl.system.MemoryUtil.*")
                if (!hasMemoryStack && functions.any { func ->
                    func.hasParam {
                        it.has<MultiType> { types.contains(PointerMapping.DATA_POINTER) } && func.hasAutoSizeFor(it)
                    }
                })
                    staticImports.add("org.lwjgl.system.Pointer.*")
            }
            if (staticImports.isNotEmpty()) {
                println()
                for (import in staticImports) {
                    println("import static $import;")
                }
            }
            println()
        }

        preamble.printJava(this)

        val isOpen = access === Access.PUBLIC && (hasFunctions || extends != null)
        print("${access.modifier}${if (isOpen) "" else "final "}class $className")
        extends.let {
            if (it != null)
                print(" extends ${it.className}")
        }
        println(" {")

        if (hasFunctions && (binding == null || functions.any(Func::hasCustomJNI)) && (module.library != null || binding !is SimpleBinding)) {
            println(if (module.library == null)
                "\n${t}static { Library.initialize(); }"
            else
                module.library.expression(module)
                    .let { library ->
                        if (library.contains('\n'))
                            """
    static {
        ${library.trim()}
    }"""
                        else if (library.endsWith(");"))
                            "\n${t}static { $library }"
                        else
                            "\n${t}static { Library.loadSystem(System::load, System::loadLibrary, $className.class, \"${module.java}\", Platform.mapLibraryNameBundled(\"$library\")); }"
                    })
        }
        if (binding is SimpleBinding) {
            binding.generateFunctionSetup(this, this@NativeClass)
        }

        if (functions.any { it.critical }) {
            TODO("Not implementation")
//            val lookupFunctions = genFunctions.filter { it.criticalUsesLookup }.toList()
//
//            println()
//            lookupFunctions.forEach { func ->
//                println("${t}private static final long ${func.criticalMethodName};")
//            }
//            println()
//            println("${t}static {")
//            println("$t${t}${lookupStatement ?: "SymbolsLookup lookup = SymbolsLookup.cast(Linker.nativeLinker().defaultLookup());"}")
//            lookupFunctions.forEach { func ->
//                println("$t${t}${func.criticalMethodName} = lookup.find0(${func.functionAddress});")
//            }
//            println("$t}")
//            println()
        }

        // Constant macros (e.g. `macro..Address..ffi_type.p(...)`) produce a field instead of a method. Their field is declared together with the regular
        // constants, and the value is assigned in the class initializer below.
        val constantMacros = genFunctions.filter { it.has<Macro> { constant } && !it.has(private) && !it.has<Reuse>() }
        // Class initializer variables are local to the initializer, so any constant that references one is a runtime constant.
        val runtimeConstants = if (cinitSetRTConst)
            computeRuntimeConstantNames(constantBlocks, constantMacros.map { it.name } + cinitVariables.keys)
        else
            emptySet()

        // Private expression macros are only used to define constants, so their expression is inlined at the call sites and the method is removed.
        val inlinableMacros = if (cinitSetRTConst)
            genFunctions
                .filter { it.has<Macro> { expression != null } && it.has(private) && !it.has<Reuse>() }
                .associate { it.name to (it.parameters.map { parameter -> parameter.name } to it.get<Macro>().expression!!) }
        else
            emptyMap()

        constantBlocks.forEach {
            it.generate(this, runtimeConstants, inlinableMacros, initNativeGetterReads)
        }

        // Constants initializer. It is emitted before the methods and the custom static fields, so that any field initializer that references a constant sees
        // the assigned value. When disabled, the constant macros are initialized inline by generateMethods.
        if (cinitSetRTConst) {
            constantMacros.forEach { func -> func.appendConstantField(this) }

            if (constantMacros.isNotEmpty() || runtimeConstants.isNotEmpty() || cinitVariables.isNotEmpty() || initNativeEntries.isNotEmpty()) {
                // A cache variable is declared for every struct result type of the constant macros and reused by their assignments.
                val resultVars = constantMacros
                    .filter { it.returns.isStructValue }
                    .map { it.returns.nativeType.javaMethodType }
                    .distinct()
                    .mapIndexed { index, type -> type to if (index == 0) RESULT else "$RESULT${index + 1}" }
                    .toMap()

                print("\n    static {\n")
                if (initNativeEntries.isNotEmpty()) {
                    initNativeArrays.forEach { (type, count) -> print("        ${type}[] ${initNativeArrayName(type)} = new ${type}[$count];\n") }
                    print("        initNative(${initNativeArrays.joinToString(", ") { initNativeArrayName(it.first) }});\n")
                }
                cinitVariables.forEach { (name, definition) -> print("        ${definition.first} $name = ${definition.second};\n") }
                resultVars.forEach { (type, variable) -> print("        $type $variable;\n") }
                constantMacros.forEach { func -> func.generateConstantInitializer(this, resultVars) }
                constantBlocks.forEach { block -> block.generateInitializers(this) }
                print("    }\n")
            }

            if (initNativeEntries.isNotEmpty()) {
                print("\n    private static native void initNative(${initNativeArrays.joinToString(", ") { "${it.first}[] ${initNativeArrayName(it.first)}" }});\n")
            }
        } else {
            check(cinitVariables.isEmpty()) {
                "cinitVariable() cannot be used in ${className}: cinitSetRTConst is false"
            }
        }

        if (hasFunctions || binding is SimpleBinding) {
            printCustomMethods(static = true)

            // This allows binding classes to be "statically" extended. Not a good practice, but usable with static imports.
            print("""
    ${if (isOpen) "protected" else "private"} $className() {
        throw new UnsupportedOperationException();
    }
""")
        } else {
            print("\n$t${if (isOpen) "protected" else "private"} $className() {}\n")
        }

        genFunctions.forEach { func ->
            if (func.name !in inlinableMacros && !func.isInitNativeCandidate && !func.hasParam { it.nativeType is ArrayType<*> })
                print("\n$t// --- [ ${func.name} ] ---\n")
            try {
                func.generateMethods(this)
            } catch (e: Exception) {
                throw RuntimeException("Uncaught exception while generating method: $className.${func.simpleName}", e)
            }
        }

        printCustomMethods(static = false)

        print("\n}")
    }

    override val skipNative get() = functions.none { it.hasCustomJNI && !it.has<Reuse>() }

    override fun PrintWriter.generateNative() {
        print(HEADER)
        preamble.printNative(this)

        if (binding != null) {
            // Generate typedefs for casting the function pointers
            println()
            functions.asSequence().filter { !it.critical && it.hasCustomJNI && !it.has<Reuse>() && !it.isInitNativeCandidate }.forEach {
                it.generateFunctionDefinition(this)
            }
        }

        println("\nEXTERN_C_ENTER")

        genFunctions.asSequence().filter { !it.critical && it.hasCustomJNI && !it.has<Reuse>() && !it.isInitNativeCandidate }.forEach {
            println()
            it.generateFunction(this)
        }

        if (initNativeEntries.isNotEmpty()) {
            println()
            generateInitNative()
        }

        println("\nEXTERN_C_EXIT")
    }

    /**
     * Emits the merged JNI initializer: it fills each primitive array with the values of all the constant getters of this class, so that the class initializer
     * only performs a single JNI call.
     */
    private fun PrintWriter.generateInitNative() {
        print("JNIEXPORT void JNICALL Java_${nativeFileNameJNI}_initNative(JNIEnv *$JNIENV, jclass clazz")
        initNativeArrays.forEach { (type, _) -> print(", ${jniArrayType(type)} ${initNativeArrayName(type)}") }
        println(") {")

        println("$t${"UNUSED_PARAM"}(clazz)")

        initNativeArrays.forEach { (type, _) ->
            println("$t${jniPrimitiveType(type)} *${initNativeArrayName(type)}_ptr = (*$JNIENV)->Get${type.upperCaseFirst}ArrayElements($JNIENV, ${initNativeArrayName(type)}, NULL);")
        }

        initNativeEntries.forEach { entry ->
            print("$t${initNativeArrayName(entry.arrayType)}_ptr[${entry.index}] = ")
            entry.func.generateNativeValueExpression(this)
            println(';')
        }

        initNativeArrays.forEach { (type, _) ->
            println("$t(*$JNIENV)->Release${type.upperCaseFirst}ArrayElements($JNIENV, ${initNativeArrayName(type)}, ${initNativeArrayName(type)}_ptr, 0);")
        }

        println("}")
    }

    internal fun nativeDirectivesWarning() {
        if (preamble.hasNativeDirectives)
            println("${t}Unnecessary native directives in: ${module.packageKotlin}.$templateName")
    }

    fun printPointers(
        out: PrintWriter,
        printPointer: (func: Func) -> String = Func::name,
        filter: ((Func) -> Boolean)? = null
    ) {
        out.print("\n$t$t$t")

        val functions = _functions.values.let { if (filter == null) it.filter { func -> !func.has<Macro>() } else it.filter(filter) }

        var lineSize = 12
        functions.forEachWithMore { func, more ->
            if (more) {
                out.print(", ")
                lineSize += 2
            }

            val pointer = printPointer(func)

            lineSize += pointer.length
            if (160 <= lineSize) {
                out.print("\n$t$t$t")
                lineSize = 12 + pointer.length
            }

            out.print(pointer)
        }

        out.print("\n$t$t")
    }

    // DSL extensions

    /** May be used to split init methods that end up too large to be compilable to a single class. */
    fun split(init: (NativeClass.() -> Unit)) {
        this.init()
    }

    operator fun <T : Any> ConstantType<T>.invoke(vararg constants: Constant<T>, access: Access = Access.PUBLIC): ConstantBlock<T> {
        val block = ConstantBlock(this@NativeClass, access, this, *constants)
        constantBlocks.add(block)
        return block
    }

    /**
     * Declares a local variable of the class initializer with the specified type and expression, and returns its name. It is used to factor out repeated
     * expressions from the constants (e.g. `ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN`). Declaring the same name with the same type and expression
     * more than once is a no-op.
     */
    fun cinitVariable(name: String, type: String, expression: String): String {
        val existing = cinitVariables[name]
        check(existing == null || existing == (type to expression)) {
            "Conflicting class initializer variable: $name is already declared as ${existing?.first} = ${existing?.second}"
        }
        cinitVariables[name] = type to expression
        return name
    }

    /** Adds a new constant. */
    operator fun <T : Any> String.rangeTo(value: T) = Constant(this, value)

    operator fun <T : Any> String.rangeTo(expression: String): Constant<T> = ConstantExpression(this, expression, false)

    /** Adds a new String constant whose value is an expression. */
    infix fun String.expr(expression: String): Constant<String> = ConstantExpression(this, expression, true)

    /** Adds a new enum constant. */
    val String.enum get() = Constant(this, EnumIntValue())
    fun String.enum(value: Int) =
        Constant(this, EnumIntValue(value))
    fun String.enum(expression: String) =
        Constant(this, EnumIntValueExpression(expression))

    // TODO: this is ugly, try new DSL?
    val String.enumByte get() = Constant(this, EnumByteValue())
    fun String.enum(value: Byte) =
        Constant(this, EnumByteValue(value))
    fun String.enumByte(expression: String) =
        Constant(this, EnumByteValueExpression(expression))

    val String.enumLong get() = Constant(this, EnumLongValue())
    fun String.enum(value: Long) =
        Constant(this, EnumLongValue(value))
    fun String.enumLong(expression: String) =
        Constant(this, EnumLongValueExpression(expression))

    operator fun DataType.invoke(name: String) =
        Parameter(this, name)

    operator fun VoidType.invoke() = VOID_ARGS
    operator fun VoidType.invoke(
        className: String,
        vararg signature: Parameter,
        nativeType: String = ANONYMOUS,
        init: (CallbackFunction.() -> Unit)? = null
    ) = createCallback(this, nativeType, className, init, *signature)

    operator fun DataType.invoke(
        className: String,
        vararg signature: Parameter,
        nativeType: String = ANONYMOUS,
        init: (CallbackFunction.() -> Unit)? = null
    ) = createCallback(this, nativeType, className, init, *signature)

    private fun createCallback(
        returns: NativeType,
        nativeType: String,
        className: String,
        init: (CallbackFunction.() -> Unit)?,
        vararg signature: Parameter
    ): FunctionType {
        val callback = CallbackFunction(this@NativeClass.module, className, nativeType, returns, *(
            if (signature.size == 1 && signature[0].nativeType === void) {
                emptyArray()
            } else {
                signature
            }
        ))
        if (init != null)
            callback.init()
        Generator.register(callback)
        Generator.register(CallbackInterface(callback))
        return FunctionType(callback)
    }

    fun AutoSize(reference: String, vararg dependent: String, factor: AutoSizeFactor? = null) =
        org.lwjgl.generator.AutoSize(reference, *dependent, factor = factor)

    /** Marks the parameter to be replaced with .remaining() on the buffer parameter specified by reference. */
    fun AutoSize(div: Int, reference: String, vararg dependent: String) =
        when {
            div < 1                    -> throw IllegalArgumentException()
            div == 1                   -> AutoSize(reference, *dependent)
            Integer.bitCount(div) == 1 -> AutoSizeShr(Integer.numberOfTrailingZeros(div).toString(), reference, *dependent)
            else                       -> AutoSizeDiv(div.toString(), reference, dependent = dependent)
        }

    fun AutoSizeDiv(expression: String, reference: String, vararg dependent: String) =
        AutoSize(reference, *dependent, factor = AutoSizeFactor.div(expression))

    fun AutoSizeMul(expression: String, reference: String, vararg dependent: String) =
        AutoSize(reference, *dependent, factor = AutoSizeFactor.mul(expression))

    fun AutoSizeShr(expression: String, reference: String, vararg dependent: String) =
        AutoSize(reference, *dependent, factor = AutoSizeFactor.shr(expression))

    fun AutoSizeShl(expression: String, reference: String, vararg dependent: String) =
        AutoSize(reference, *dependent, factor = AutoSizeFactor.shl(expression))

    /** Marks a pointer parameter as nullable. */
    val nullable get() = org.lwjgl.generator.nullable

    operator fun VoidType.invoke(
        name: String,
        vararg parameters: Parameter,
        noPrefix: Boolean = false
    ) = createFunction(ReturnValue(this), name, noPrefix, *parameters)

    operator fun DataType.invoke(
        name: String,
        vararg parameters: Parameter,
        noPrefix: Boolean = false
    ) = createFunction(ReturnValue(this), name, noPrefix, *parameters)

    private fun createFunction(
        returns: ReturnValue,
        name: String,
        noPrefix: Boolean,
        vararg parameters: Parameter
    ): Func {
        val params = if (parameters.size == 1 && parameters[0].nativeType === void) {
            emptyArray()
        } else {
            parameters
        }
        val overload = name.indexOf('@').let { if (it == -1) name else name.substring(0, it) }
        return addFunction(name, Func(
            returns = returns,
            simpleName = if (noPrefix || (overload[0].isJavaIdentifierStart() && !JAVA_KEYWORDS.contains(overload))) overload else "$prefixMethod$overload",
            name = if (noPrefix) overload else "$prefixMethod$overload",
            nativeClass = this@NativeClass,
            parameters = params
        ))
    }

    fun addFunction(name: String, func: Func): Func {
        require(_functions.put(name, func) == null) {
            "The $name function is already defined in ${this@NativeClass.className}."
        }

        return CaptureCallState.apply(func)
    }

    fun customMethod(method: String) {
        customMethods.add(method.trim())
    }

    private fun PrintWriter.printCustomMethods(static: Boolean) {
        customMethods
            .filter { it.startsWith("static {") == static }
            .forEach {
                println("\n$t$it")
            }
    }

    operator fun NativeClass.get(functionName: String) = _functions[functionName] ?: throw IllegalArgumentException("Referenced function does not exist: $templateName.$functionName")

    fun reuse(nativeClass: NativeClass, functionName: String) : Func {
        val reference = nativeClass[functionName]

        val func = Reuse(nativeClass)..Func(
            returns = reference.returns,
            simpleName = reference.simpleName,
            name = reference.name,
            nativeClass = this,
            parameters = reference.parameters
        ).copyModifiers(reference)

        this._functions[functionName] = func
        return func
    }

    operator fun Func.get(paramName: String): Parameter = getParam(paramName).let {
        if (it === EXPLICIT_FUNCTION_ADDRESS || it === JNI_ENV)
            it
        else
            it.copy()
    }

    fun getCapabilityJavadoc(): String {
        return "When true, {@code $templateName} is supported.".toJavaDoc()
    }

}

// DSL extensions

fun String.nativeClass(
    module: Module,
    templateName: String = this,
    nativeSubPath: String = "",
    prefix: String = "",
    prefixMethod: String = prefix.lowercase(),
    prefixConstant: String = if (prefix.isEmpty() || prefix.endsWith('_')) prefix else "${prefix}_",
    prefixTemplate: String = prefix,
    postfix: String = "",
    binding: APIBinding? = null,
    callingConvention: CallingConvention = module.callingConvention,
    cinitSetRTConst: Boolean = true,
    init: (NativeClass.() -> Unit)? = null
): NativeClass {
    val ext = NativeClass(module, this, nativeSubPath, templateName, prefix, prefixMethod, prefixConstant, prefixTemplate, postfix, binding, callingConvention, cinitSetRTConst)
    if (init != null)
        ext.init()

    binding?.addClass(ext)

    return ext
}
/**
 * Computes the names of constants that cannot be initialized inline.
 *
 * A constant is runtime when it is explicitly seeded, contains a method call, or depends on another runtime
 * constant. Dependencies are collected once and propagated through the graph instead of repeatedly rescanning
 * every constant until a fixed point is reached.
 */
private fun computeRuntimeConstantNames(blocks: List<ConstantBlock<*>>, seed: List<String>): Set<String> {
    val constants = blocks.flatMap { it.allConstants() }
    // The referenced names include the seeded (macro) constants, so that a block constant that references a macro field is also detected as runtime.
    val names = HashSet<String>(constants.size + seed.size)
    constants.mapTo(names) { it.first }
    names.addAll(seed)
    val dependents = HashMap<String, MutableList<String>>()

    for ((name, expression) in constants) {
        if (expression == null)
            continue

        if (isRuntimeExpression(expression))
            continue

        for (reference in constantReferences(expression, names))
            dependents.getOrPut(reference) { ArrayList() }.add(name)
    }

    val runtime = HashSet(seed)
    val queue = java.util.ArrayDeque<String>()
    queue.addAll(seed)

    for ((name, expression) in constants) {
        if (expression != null && isRuntimeExpression(expression) && runtime.add(name))
            queue.addLast(name)
    }

    while (queue.isNotEmpty()) {
        val runtimeName = queue.removeFirst()
        for (dependent in dependents[runtimeName].orEmpty()) {
            if (runtime.add(dependent))
                queue.addLast(dependent)
        }
    }

    return runtime
}

/** Returns constant identifiers referenced by a Java expression. String/character literals and comments are ignored. */
private fun constantReferences(expression: String, names: Set<String>): Sequence<String> = sequence {
    var index = 0
    while (index < expression.length) {
        when (val c = expression[index]) {
            '"', '\'' -> {
                val quote = c
                index++
                while (index < expression.length) {
                    if (expression[index] == '\\') {
                        index += 2
                    } else if (expression[index] == quote) {
                        index++
                        break
                    } else {
                        index++
                    }
                }
            }
            '/' -> when {
                index + 1 < expression.length && expression[index + 1] == '/' -> {
                    index = expression.indexOf('\n', index + 2).let { if (it < 0) expression.length else it + 1 }
                }
                index + 1 < expression.length && expression[index + 1] == '*' -> {
                    val end = expression.indexOf("*/", index + 2)
                    index = if (end < 0) expression.length else end + 2
                }
                else -> index++
            }
            else -> {
                if (!c.isJavaIdentifierStart()) {
                    index++
                    continue
                }

                val start = index++
                while (index < expression.length && expression[index].isJavaIdentifierPart())
                    index++

                val name = expression.substring(start, index)
                if (name in names)
                    yield(name)
            }
        }
    }
}

/** The name of the `initNative` primitive array parameter that carries values of the specified Java primitive type. */
private fun initNativeArrayName(type: String) = "__${type}s"

/** The JNI array type of the specified Java primitive type. */
private fun jniArrayType(type: String) = "j${type}Array"

/** The JNI scalar type of the specified Java primitive type. */
private fun jniPrimitiveType(type: String) = "j$type"
