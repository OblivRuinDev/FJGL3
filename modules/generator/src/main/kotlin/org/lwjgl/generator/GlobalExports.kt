/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.generator

import java.io.PrintWriter
import java.util.concurrent.ConcurrentHashMap

/** The element type of an exported array. */
enum class ExportsType(val cType: String) {
    /** A `void*` address. Its size is platform dependent (`Pointer.POINTER_SIZE`); the reader shifts the index with `Pointer.POINTER_SHIFT`. */
    ADDRESS("void*"),
    /** A 64-bit integer. */
    LONG("int64_t"),
    /** A 32-bit integer. */
    INT("int32_t"),
    /** A 16-bit integer. */
    SHORT("int16_t"),
    /** An 8-bit integer. */
    BYTE("int8_t")
}

/**
 * Generates a C file that exports one global array per [ExportsType]. At runtime the address of each array is resolved with a symbol lookup and its values are
 * read with plain memory access, avoiding a JNI call per value.
 *
 * <p>A module owns a default instance ([Module.exports]). Additional instances may be created with different constructor arguments when a different grouping
 * or output file is required (for example one per platform-specific [nativeSubPath], whose generated C file has its own native preamble). An instance that has
 * no registered values produces no output.</p>
 *
 * @param nativeSubPath the sub-path of the generated C file, relative to `<module>/src/generated/c`. It also takes part in the exported symbol, so contexts
 * with different preambles never collide.
 * @param fileName the name of the generated C file, without extension
 */
class GlobalExports(
    val module: Module,
    val nativeSubPath: String = "",
    val fileName: String = "exports"
) {

    companion object {
        /** The contexts of every module, keyed by module key and native sub-path. */
        private val CONTEXTS = ConcurrentHashMap<String, GlobalExports>()

        internal fun of(module: Module, nativeSubPath: String): GlobalExports =
            CONTEXTS.computeIfAbsent("${module.path}\u0000$nativeSubPath") {
                // The native build maps source files to object files by base name only (regexpmapper `([\w\-]+)\.c` -> `\1.o`), so a sub-path's export file must
                // not share its base name with the module's default one. Otherwise `macos/exports.c` and `exports.c` both produce `exports.o` and one of them
                // overwrites the other (only macOS compiles both), dropping the overwritten file's symbols.
                val fileName = if (nativeSubPath.isEmpty())
                    "exports"
                else
                    "${nativeSubPath.map { c -> if (c.isLetterOrDigit()) c else '_' }.joinToString("")}_exports"
                GlobalExports(module, nativeSubPath, fileName)
            }

        /** Every context that has been created, for output generation. */
        internal val contexts: Collection<GlobalExports>
            get() = CONTEXTS.values
    }

    /** A registered array element: the C [statement] used verbatim and the [source] that registered it (kept for debugging). */
    class Entry(val statement: String, val source: String) {
        /**
         * True when [statement] is a plain identifier, which may be a variable's value (for example `stdin`) rather than a constant expression. Such an array
         * cannot be statically initialized and is filled when the library is loaded instead.
         */
        val isRuntime: Boolean
            get() = BARE_IDENTIFIER.matches(statement)
    }

    private val entries = LinkedHashMap<ExportsType, MutableList<Entry>>()

    /** The manually registered symbols, keyed by name, with the index of their address in the [ExportsType.ADDRESS] array. */
    private val manual = LinkedHashMap<String, Int>()

    /**
     * The native preamble shared by all registered statements. Statements from several classes may reference different symbols, so every registering class
     * merges its own imports and directives here.
     */
    internal val preamble = Preamble()

    /** The C symbol prefix shared by all exported arrays of this context. */
    private fun symbolPrefix() = buildString {
        append("org_lwjgl_").append(module.path.replace('.', '_'))
        if (nativeSubPath.isNotEmpty()) {
            append('_')
            nativeSubPath.forEach { append(if (it.isLetterOrDigit()) it else '_') }
        }
        append("_exports")
    }

    /** The C symbol of the array exported for the given [type]. It matches the `*org_lwjgl_*` pattern of the shared library export scripts. */
    fun symbol(type: ExportsType) = "${symbolPrefix()}_${type.name.lowercase()}"

    /** Registers a C include required by the registered statements (for example `ffi.h` for the libffi constants). */
    fun nativeImport(vararg files: String) {
        preamble.nativeImport(*files)
    }

    /**
     * Registers [statements] as elements of the array of the given [type]. The statements are emitted verbatim as array elements. Returns the index of every
     * registered element, in registration order.
     */
    @Synchronized
    fun register(type: ExportsType, vararg statements: String): IntArray {
        val list = entries.getOrPut(type) { ArrayList() }
        val base = list.size
        statements.forEach { list.add(Entry(it, it)) }
        return IntArray(statements.size) { base + it }
    }

    /** Registers a single element with an explicit debug [source], for when the source name differs from the [statement]. */
    @Synchronized
    fun register(type: ExportsType, source: String, statement: String): Int {
        val list = entries.getOrPut(type) { ArrayList() }
        list.add(Entry(statement, source))
        return list.size - 1
    }

    /** Registers the given C address expressions in the [ExportsType.ADDRESS] array. */
    fun registerAddresses(vararg statements: String) = register(ExportsType.ADDRESS, *statements)

    /**
     * Registers a manually named C symbol whose address goes into the [ExportsType.ADDRESS] array and returns its index. The index is exposed to Java as a
     * compile-time constant through the generated `ExportTable` class, so callers do not hardcode an index that may change between builds.
     */
    @Synchronized
    fun registerManual(name: String, statement: String): Int {
        val index = register(ExportsType.ADDRESS, name, statement)
        manual[name] = index
        return index
    }

    val isEmpty
        get() = entries.values.all { it.isEmpty() }

    /** The exported array types that have at least one registered value. */
    val types: List<ExportsType>
        get() = entries.filterValues { it.isNotEmpty() }.keys.toList()

    /** The manually registered symbols, keyed by name, with their index in the [ExportsType.ADDRESS] array. */
    internal val manualSymbols: Map<String, Int>
        get() = manual

    fun PrintWriter.gen() {
        print(HEADER)
        println()
        preamble.printNative(this, ::exportPreamble)
        println()

        val nonEmpty = entries.filterValues { it.isNotEmpty() }

        // Constant elements are statically initialized. Values that are not constant expressions (for example the value of `stdin`) are assigned when the library
        // is loaded and their slots are set to `0` here. Address elements are cast through `uintptr_t`: strict C forbids initializing a `void*` from a function
        // pointer, and a `const` pointer cannot be assigned to `void*` without a warning.
        println("DISABLE_WARNINGS()")
        nonEmpty.forEach { (type, list) ->
            println()
            if (list.all { it.isRuntime }) {
                println("JNIEXPORT ${type.cType} ${symbol(type)}[${list.size}];")
            } else {
                print("JNIEXPORT ${type.cType} ${symbol(type)}[${list.size}] = {")
                list.forEach { entry ->
                    println()
                    print(if (entry.isRuntime) "${t}0," else "$t${type.cast(entry.statement)},")
                }
                println()
                println("};")
            }
        }
        println()
        println("ENABLE_WARNINGS()")

        if (nonEmpty.any { (_, list) -> list.any { it.isRuntime } }) {
            println()
            println("EXPORTS_INIT(${symbolPrefix()}_init) {")
            println("${t}DISABLE_WARNINGS()")
            nonEmpty.forEach { (type, list) ->
                val name = symbol(type)
                list.forEachIndexed { index, entry ->
                    if (entry.isRuntime)
                        println("$t$name[$index] = ${type.cast(entry.statement)};")
                }
            }
            println("${t}ENABLE_WARNINGS()")
            println("}")
        }
    }

    /**
     * Generates the `ExportTable` Java class, which exposes the index of every manually registered symbol as a compile-time constant. The index is the position
     * of the symbol's address in the exported address array ([symbol]`(ExportsType.ADDRESS)`).
     */
    fun PrintWriter.genExportTable() {
        print(HEADER)
        println("package ${module.packageName};\n")
        println("/** The indices of manually registered C symbols in the exported address array of the ${module.key} library. */")
        println("public final class ExportTable {\n")
        manual.forEach { (name, index) -> println("${t}public static final int $name = $index;") }
        println()
        println("${t}private ExportTable() {}")
        println("}")
    }

}

/** In the exports file an implementation include (`*.c`) is replaced by its header, so that the implementation is not compiled a second time. */
private fun exportPreamble(expression: String) =
    expression.replace(EXPORT_IMPLEMENTATION_INCLUDE) { "#include \"${it.groupValues[1]}.h\"" }

private val EXPORT_IMPLEMENTATION_INCLUDE = Regex("""#include\s+"([^"]+)\.c"""")

private val BARE_IDENTIFIER = Regex("""^[A-Za-z_]\w*$""")

/**
 * When a statement goes into the exported `void*` array it is cast through `uintptr_t`. This is required because neither implicit conversion of a function
 * pointer to `void*` nor of a `const` pointer to `void*` is allowed by `-Wpedantic`/Clang warnings.
 */
private fun ExportsType.cast(statement: String) =
    if (this == ExportsType.ADDRESS) "(void*)(uintptr_t)($statement)" else statement
