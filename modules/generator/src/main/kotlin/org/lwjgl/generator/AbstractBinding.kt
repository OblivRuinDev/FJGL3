/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 */
package org.lwjgl.generator

import java.io.PrintWriter

/** The pattern matching a plain extension name in a [DependsOn] reference. */
private val DEPENDENCY_NAME = "[A-Za-z0-9_]+".toRegex()

/** The per-capabilities-class state of the reduced extension-check emission. */
class CapsChecks(
    /** The check method parameter list, including the trailing ext parameter, e.g. `MemoryStack stack, FunctionProvider provider, long[] caps, Set<String> ext`. */
    val signature: String,
    /** The check call arguments preceding `ext`, e.g. `stack, provider, caps`. */
    val args: String,
    /** The API name used by reportMissing, e.g. `VK`. */
    val api: String,
    /** Whether the binding emits flag-gated indices for functions with a [DependsOn] modifier; such checks never use the reduced forms. */
    val supportsDependencies: Boolean = false
) {
    internal var printed = false
}

/**
 * Base class for API bindings with a generated capabilities class. It hosts the generation logic that is shared by all bindings, so that the per-binding
 * classes only customize the parts that genuinely differ: the source of the function ordinals, the check-call arguments and the dependency semantics.
 */
abstract class AbstractBinding(
    module: Module,
    className: String,
    apiCapabilities: APICapabilities = APICapabilities.NONE
) : APIBinding(module, className, apiCapabilities) {

    /**
     * The functions that receive an ordinal in this binding's capabilities class, in ordinal order. Bindings that build their ordinals differently (e.g.
     * from a command table) leave this empty and pass their own maps to the print helpers.
     */
    protected open val ordinalFunctions: List<Func> = emptyList()

    /** Function name caps index, deduplicated in ordinal order. */
    protected val functionOrdinals: Map<String, Int> by lazy(LazyThreadSafetyMode.NONE) {
        LinkedHashMap<String, Int>().also { ordinals ->
            ordinalFunctions.forEach { func ->
                if (!ordinals.contains(func.name)) {
                    ordinals[func.name] = ordinals.size
                }
            }
        }
    }

    /** The Java expression that gates [func]'s [DependsOn] reference: a plain extension name becomes `ext.contains(...)`, any other expression is used verbatim. */
    protected open fun getDependencyExpression(func: Func): String = func.get<DependsOn>().reference.let {
        if (DEPENDENCY_NAME.matches(it)) "ext.contains(\"$it\")" else it
    }

    /** A map of distinct dependency expression to flag index, with flags numbered in first-appearance order. */
    protected fun buildDependencyFlags(
        candidates: Sequence<Func>,
        expression: (Func) -> String = ::getDependencyExpression
    ): LinkedHashMap<String, Int> = candidates
        .map(expression)
        .foldIndexed(LinkedHashMap()) { index, map, expr ->
            if (!map.containsKey(expr)) {
                map[expr] = index
            }
            map
        }

    /** Prints the `int flagN = <expression> ? 0 : Integer.MIN_VALUE;` locals of a check method, hoisting the `ext.contains` checks that appear in more than one
     * flag into boolean locals. */
    protected fun PrintWriter.printDependencyFlags(dependencies: Map<String, Int>) {
        if (dependencies.isEmpty()) {
            return
        }

        val contains = "ext\\.contains\\(\"([^\"]+)\"\\)".toRegex()
        val counts = HashMap<String, Int>()
        dependencies.keys.forEach { expr -> contains.findAll(expr).forEach { counts.merge(it.groupValues[1], 1, Int::plus) } }
        val hoisted = counts.filterValues { 1 < it }
        println()
        hoisted.forEach { (token, _) -> print("\n$t${t}boolean ${hoistName(token)} = ext.contains(\"$token\");") }
        dependencies.forEach { (expression, index) ->
            var expr = expression
            hoisted.forEach { (token, _) -> expr = expr.replace("ext.contains(\"$token\")", hoistName(token)) }
            print("\n$t${t}int flag$index = $expr ? 0 : Integer.MIN_VALUE;")
        }
    }

    /** Returns the name of the local that hoists the given [token] argument of an `ext.contains` check. */
    private fun hoistName(token: String) = if (token.startsWith("OpenGL")) "core${token.substring(6)}" else "c$token"

    /** Prints the `name = <caps accessor>;` assignments of a capabilities class initializer. */
    protected fun printCapsAssignments(
        writer: PrintWriter,
        ordinals: Map<String, Int>,
        accessor: (Int) -> String
    ) {
        ordinals.forEach { (name, index) ->
            writer.print("\n$t$t$name = ${accessor(index)};")
        }
    }

    /**
     * Prints a `checkFunction`/`checkFunctions` call for the functions of [nativeClass] selected by [filter], preferring a contiguous ordinal range (no
     * int[] allocation) and falling back to an explicit index array.
     *
     * @param call            the call arguments preceding the function indices, e.g. `stack, provider, caps` or `stack, provider, device, caps`
     * @param ordinals        function name 鈫?caps index
     * @param dependencyIndex optionally maps a function with a [DependsOn] modifier to its `flagN + index` expression; when provided, dependent functions
     *                        disable the contiguous fast-path
     */
    protected fun PrintWriter.printFunctionChecks(
        nativeClass: NativeClass,
        call: String,
        ordinals: Map<String, Int>,
        filter: (Func) -> Boolean,
        dependencyIndex: ((Func) -> String?)? = null
    ) {
        val funcs = nativeClass.functions.filter(filter).toList()
        val indices = funcs.map { ordinals.getValue(it.name) }
        if (funcs.isNotEmpty()
            && (dependencyIndex == null || funcs.none { dependencyIndex(it) != null })
            && indices.zipWithNext().all { (a, b) -> b == a + 1 }
        ) {
            // A contiguous range: no int[] allocation is required.
            if (indices.size == 1) {
                print("checkFunction($call, ${indices[0]}, \"${funcs[0].name}\")")
            } else {
                print("checkFunctions($call, ${indices.first()}, ${indices.first() + funcs.size},")
                nativeClass.printPointers(this, { "\"${it.name}\"" }, filter)
                print(")")
            }
        } else {
            print("checkFunctions($call, new int[] {")
            if (dependencyIndex == null) {
                // Sort by caps index, so the indices ascend while every index keeps its function paired.
                val sorted = funcs.sortedBy { ordinals.getValue(it.name) }
                printWrapped(sorted.map { ordinals.getValue(it.name).toString() })
                print("},")
                printWrapped(sorted.map { "\"${it.name}\"" })
            } else {
                nativeClass.printPointers(this, { func -> dependencyIndex(func) ?: ordinals[func.name].toString() }, filter)
                print("},")
                nativeClass.printPointers(this, { "\"${it.name}\"" }, filter)
            }
            print(")")
        }
    }

    /** Prints [items] comma-separated with the same line-wrapping style as [NativeClass.printPointers]. */
    protected fun PrintWriter.printWrapped(items: List<String>) {
        print("\n$t$t$t")
        var lineSize = 12
        items.forEachIndexed { index, item ->
            if (0 < index) {
                print(", ")
                lineSize += 2
            }
            lineSize += item.length
            if (160 <= lineSize) {
                print("\n$t$t$t")
                lineSize = 12 + item.length
            }
            print(item)
        }
        print("\n$t$t")
    }

    /**
     * Whether the extension check of [nativeClass] uses a reduced inline form: at least one checked function and, when the binding emits dependency flags, no
     * function with a [DependsOn] modifier. The helper used depends on the shape: a single function → `checkExt2`, multiple contiguous functions →
     * `checkExt1`, otherwise `checkExt3`.
     */
    protected fun isReducedCheck(
        nativeClass: NativeClass,
        filter: (Func) -> Boolean,
        ordinals: Map<String, Int>,
        supportsDependencies: Boolean = false
    ): Boolean {
        val funcs = nativeClass.functions.filter(filter).toList()
        return funcs.isNotEmpty() && !(supportsDependencies && funcs.any { it.has<DependsOn>() })
    }

    /**
     * Prints the shared `checkExt1` helper of a capabilities class
     *
     * <pre>{@code
     * private static boolean checkExt1(signature, String extName, int from, int to, String... functions) {
     *     if (!ext.contains(extName)) {
     *         return false;
     *     }
     *     return checkFunctions(args, from, to, functions) || reportMissing(api, extName);
     * }
     * }</pre>
     *
     * once, before it is used. It must be called before the class initializer is generated, because a method cannot be nested inside it.
     */
    protected fun PrintWriter.printCheckHelper(checks: CapsChecks) {
        if (checks.printed) {
            return
        }
        checks.printed = true
        println("""

    private static boolean checkExt1(String extName, ${checks.signature}, int from, int to, String... functions) {
        if (!ext.contains(extName)) {
            return false;
        }
        return checkFunctions(${checks.args}, from, to, functions) || reportMissing("${checks.api}", extName);
    }

    private static boolean checkExt2(String extName, ${checks.signature}, int index, String function) {
        if (!ext.contains(extName)) {
            return false;
        }
        return checkFunction(${checks.args}, index, function) || reportMissing("${checks.api}", extName);
    }

    private static boolean checkExt3(String extName, ${checks.signature}, int[] indices, String... functions) {
        if (!ext.contains(extName)) {
            return false;
        }
        return checkFunctions(${checks.args}, indices, functions) || reportMissing("${checks.api}", extName);
    }""")
    }

    /**
     * Prints the reduced inline extension check call, for use in a class initializer:
     * `checkExt1(args, ext, "capName", from, to, functions)`. The caller prints the surrounding assignment and the terminating semicolon.
     */
    protected fun PrintWriter.printReducedCheckCall(
        checks: CapsChecks,
        nativeClass: NativeClass,
        capName: String,
        extRef: String,
        filter: (Func) -> Boolean,
        ordinals: Map<String, Int>
    ) {
        val funcs = nativeClass.functions.filter(filter).toList()
        if (funcs.size == 1) {
            val func = funcs.single()
            print("checkExt2(\"$capName\", ${checks.args}, $extRef, ${ordinals.getValue(func.name)}, \"${func.name}\")")
        } else if (funcs.map { ordinals.getValue(it.name) }.zipWithNext().all { (a, b) -> b == a + 1 }) {
            val from = ordinals.getValue(funcs.first().name)
            print("checkExt1(\"$capName\", ${checks.args}, $extRef, $from, ${from + funcs.size},")
            nativeClass.printPointers(this, { "\"${it.name}\"" }, filter)
            print(")")
        } else {
            // Multiple non-contiguous functions: sort by caps index, so the indices ascend while every index keeps its function paired.
            val sorted = funcs.sortedBy { ordinals.getValue(it.name) }
            print("checkExt3(\"$capName\", ${checks.args}, $extRef, new int[] {")
            printWrapped(sorted.map { ordinals.getValue(it.name).toString() })
            print("},")
            printWrapped(sorted.map { "\"${it.name}\"" })
            print(")")
        }
    }

    /**
     * Prints one capabilities check line of a class initializer: the reduced inline call when [isReducedCheck] accepts [nativeClass], otherwise a call to
     * its full `check_<extension>` method, emitted by [printExtCheckMethod].
     *
     * @param fullCall returns the full-form call expression for the class, e.g. `check_GL11(stack, provider, caps, ext)`
     * @param assign   when true, the result is assigned as `$capName = `; when false, the call is a standalone statement
     */
    protected fun printCapCheck(
        writer: PrintWriter,
        checks: CapsChecks,
        nativeClass: NativeClass,
        capName: String,
        extRef: String,
        filter: (Func) -> Boolean,
        ordinals: Map<String, Int>,
        fullCall: (NativeClass) -> String,
        assign: Boolean = true
    ) {
        writer.print("\n$t$t")
        if (assign) {
            writer.print("$capName = ")
        }
        if (isReducedCheck(nativeClass, filter, ordinals, checks.supportsDependencies)) {
            writer.printReducedCheckCall(checks, nativeClass, capName, extRef, filter, ordinals)
        } else {
            writer.print(fullCall(nativeClass))
        }
        writer.print(";")
    }

    /**
     * Prints the full form of a `check_<extension>` method, with direct `checkFunction`/`checkFunctions` calls. Extension checks that [isReducedCheck]
     * accepts are not emitted here; their calls are inlined into the class initializer instead ([printCapCheck]).
     */
    protected fun PrintWriter.printExtCheckMethod(
        checks: CapsChecks,
        nativeClass: NativeClass,
        capName: String,
        filter: (Func) -> Boolean,
        ordinals: Map<String, Int>,
        dependencies: Map<String, Int> = emptyMap()
    ) {
        if (isReducedCheck(nativeClass, filter, ordinals, checks.supportsDependencies)) {
            return
        }

        val dependencyIndex = if (dependencies.isEmpty())
            null
        else
            { func: Func ->
                if (func.has<DependsOn>()) "flag${dependencies[getDependencyExpression(func)]} + ${ordinals.getValue(func.name)}" else null
            }

        print("\n${t}private static boolean check_${nativeClass.templateName}(${checks.signature}) {")
        print("""
        if (!ext.contains("$capName")) {
            return false;
        }""")
        printDependencyFlags(dependencies)
        print("\n\n$t${t}return ")
        printFunctionChecks(nativeClass, checks.args, ordinals, filter, dependencyIndex)
        println(" || reportMissing(\"${checks.api}\", \"$capName\");")
        println("$t}")
    }
}
