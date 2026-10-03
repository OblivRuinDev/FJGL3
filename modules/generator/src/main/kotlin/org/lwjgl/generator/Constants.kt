/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl.generator

import java.io.*
import kotlin.math.*
import kotlin.reflect.*

// Extension properties for numeric literals.
inline val Int.b get() = this.toByte()
inline val Int.s get() = this.toShort()
inline val Long.i get() = this.toInt()

open class ConstantType<T : Any>(
    val javaType: String,
    val print: (T) -> String
) {
    constructor(
        type: KClass<T>,
        print: (T) -> String
    ) : this(type.java.simpleName, print)
}

val ByteConstant = ConstantType(Byte::class) { value ->
    val i = value.toInt() and 0xFF
    "0x%X".format(i).let {
        if (i < 0x80) it else "(byte)$it"
    }
}
val CharConstant = ConstantType(Char::class) { "'$it'" }
val ShortConstant = ConstantType(Short::class) { value ->
    val i = value.toInt() and 0xFFFF
    "0x%X".format(i).let {
        if (i < 0x8000) it else "(short)$it"
    }
}
val IntConstant = ConstantType(Int::class) { "0x%X".format(it) }
val LongConstant = ConstantType(Long::class) { "0x%XL".format(it) }
val FloatConstant = ConstantType(Float::class) { "%sf".format(it) }
val DoubleConstant = ConstantType(Double::class) { "%sd".format(it) }

val StringConstant = ConstantType(String::class) { if (it.contains(" + \"")) it else "\"$it\"" }
val UnquotedStringConstant = ConstantType(String::class) { it }

abstract class EnumValue

open class EnumIntValue(val value: Int? = null) : EnumValue()
class EnumIntValueExpression(val expression: String) : EnumIntValue(null)
val EnumConstant = ConstantType(EnumIntValue::class) { "0x%X".format(it) }

// TODO: this is ugly, try new DSL?
open class EnumByteValue(val value: Byte? = null) : EnumValue()
class EnumByteValueExpression(val expression: String) : EnumByteValue(null)
val EnumConstantByte = ConstantType(EnumByteValue::class) { "0x%X".format(it) }

open class EnumLongValue(val value: Long? = null) : EnumValue()
class EnumLongValueExpression(val expression: String) : EnumLongValue(null)
val EnumConstantLong = ConstantType(EnumLongValue::class) { "0x%X".format(it) }

open class Constant<out T : Any>(val name: String, val value: T?)
internal class ConstantExpression<out T : Any>(
    name: String,
    val expression: String,
    // Used for StringConstants only, false: wrap in quotes, true: print as is
    val unwrapped: Boolean
) : Constant<T>(name, null)

class ConstantBlock<T : Any>(
    val nativeClass: NativeClass,
    var access: Access,
    private val constantType: ConstantType<T>,
    vararg val constants: Constant<T>
) {

    private var noPrefix = false

    fun noPrefix(): ConstantBlock<T> {
        noPrefix = true
        return this
    }

    private fun getConstantName(name: String) = if (noPrefix) name else "${nativeClass.prefixConstant}$name"

    internal fun getClassLink(name: String) = if (noPrefix && nativeClass.prefixConstant.isNotEmpty())
        "${nativeClass.className}#$name"
    else
        "${nativeClass.className}#${nativeClass.prefixConstant}$name"

    private fun generateEnumInt(rootBlock: ArrayList<Constant<Number>>) {
        var value = 0
        var formatType = 1 // 0: hex, 1: decimal
        for (c in constants) {
            if (c is ConstantExpression) {
                @Suppress("UNCHECKED_CAST")
                rootBlock.add(c as ConstantExpression<Int>)
                continue
            }

            (c.value as EnumIntValue).let { ev ->
                rootBlock.add(when {
                    ev is EnumIntValueExpression -> {
                        try {
                            value = Integer.parseInt(ev.expression) + 1 // decimal
                            formatType = 1 // next values will be decimal
                        } catch(_: NumberFormatException) {
                            if (ev.expression.startsWith("0x", ignoreCase = true)) {
                                try {
                                    value = Integer.parseInt(ev.expression.substring(2), 16) + 1 // hex
                                } catch(_: Exception) {
                                }
                            }
                            formatType = 0 // next values will be hex
                        }
                        ConstantExpression(c.name, ev.expression, false)
                    }
                    ev.value != null          -> {
                        value = ev.value + 1
                        formatType = 0
                        Constant(c.name, ev.value)
                    }
                    else                      -> {
                        if (formatType == 1)
                            ConstantExpression(c.name, (value++).toString(), false)
                        else
                            Constant(c.name, value++)
                    }
                })
            }
        }
    }

    private fun generateEnumByte(rootBlock: ArrayList<Constant<Number>>) {
        var value = 0
        var formatType = 1 // 0: hex, 1: decimal
        for (c in constants) {
            if (c is ConstantExpression) {
                @Suppress("UNCHECKED_CAST")
                rootBlock.add(c as ConstantExpression<Byte>)
                continue
            }

            (c.value as EnumByteValue).let { ev ->
                rootBlock.add(when {
                    ev is EnumByteValueExpression -> {
                        try {
                            value = java.lang.Byte.parseByte(ev.expression) + 1 // decimal
                            formatType = 1 // next values will be decimal
                        } catch(_: NumberFormatException) {
                            if (ev.expression.startsWith("0x", ignoreCase = true)) {
                                try {
                                    value = java.lang.Byte.parseByte(ev.expression.substring(2), 16) + 1 // hex
                                } catch (_: Exception) {
                                }
                            }
                            formatType = 0 // next values will be hex
                        }
                        ConstantExpression(c.name, ev.expression, false)
                    }
                    ev.value != null          -> {
                        value = ev.value + 1
                        formatType = 0
                        Constant(c.name, ev.value)
                    }
                    else                      -> {
                        if (formatType == 1)
                            ConstantExpression(c.name, (value++).toString(), false)
                        else
                            Constant(c.name, value++)
                    }
                })
            }
        }
    }

    private fun generateEnumLong(rootBlock: ArrayList<Constant<Number>>) {
        var value = 0L
        var formatType = 1 // 0: hex, 1: decimal
        for (c in constants) {
            if (c is ConstantExpression) {
                @Suppress("UNCHECKED_CAST")
                rootBlock.add(c as ConstantExpression<Long>)
                continue
            }

            (c.value as EnumLongValue).let { ev ->
                rootBlock.add(when {
                    ev is EnumLongValueExpression -> {
                        try {
                            value = java.lang.Long.parseLong(ev.expression) + 1L // decimal
                            formatType = 1 // next values will be decimal
                        } catch(_: NumberFormatException) {
                            if (ev.expression.startsWith("0x", ignoreCase = true)) {
                                try {
                                    value = java.lang.Long.parseLong(ev.expression.substring(2), 16) + 1L // hex
                                } catch (_: Exception) {
                                }
                            }
                            formatType = 0 // next values will be hex
                        }
                        ConstantExpression(c.name, ev.expression, false)
                    }
                    ev.value != null          -> {
                        value = ev.value + 1L
                        formatType = 0
                        Constant(c.name, ev.value)
                    }
                    else                      -> {
                        if (formatType == 1)
                            ConstantExpression(c.name, (value++).toString(), false)
                        else
                            Constant(c.name, value++)
                    }
                })
            }
        }
    }

    private var resolvedValue: Pair<ConstantType<*>, List<Constant<*>>>? = null

    private fun resolved(): Pair<ConstantType<*>, List<Constant<*>>> {
        resolvedValue?.let { return it }

        val result: Pair<ConstantType<*>, List<Constant<*>>> = if (constantType === EnumConstant || constantType === EnumConstantByte || constantType === EnumConstantLong) {
            // Increment/update the current enum value while iterating the enum constants.
            val rootBlock = ArrayList<Constant<Number>>()

            val constantTypeRender = if (constantType === EnumConstant) {
                generateEnumInt(rootBlock)
                IntConstant
            } else if (constantType === EnumConstantByte) {
                generateEnumByte(rootBlock)
                ByteConstant
            } else {
                generateEnumLong(rootBlock)
                LongConstant
            }

            constantTypeRender to rootBlock
        } else {
            constantType to constants.toList()
        }

        resolvedValue = result
        return result
    }

    /** The names of the constants that cannot be initialized inline, because their value is not a compile-time constant. */
    private var runtimeNames: Set<String> = emptySet()

    /** Private expression macros (name -> parameter names and expression) that are inlined at their call sites. */
    private var expressionMacros: Map<String, Pair<List<String>, String>> = emptyMap()

    /** The name and, for expressions, the value expression of every resolved constant. Used to compute the runtime constants of the whole class. */
    internal fun allConstants(): List<Pair<String, String?>> {
        val (_, constants) = resolved()
        return constants.map { constant -> getConstantName(constant.name) to (constant as? ConstantExpression)?.expression }
    }

    /**
     * Emits the field declaration. Compile-time constants are initialized inline (they remain constant variables), while runtime constants are assigned in
     * the class initializer by [generateInitializers].
     */
    internal fun generate(
        writer: PrintWriter,
        runtimeNames: Set<String> = emptySet(),
        expressionMacros: Map<String, Pair<List<String>, String>> = emptyMap()
    ) {
        this.runtimeNames = runtimeNames
        this.expressionMacros = expressionMacros

        val (type, constants) = resolved()

        writer.println()
        writer.print("$t${access.modifier}static final ${type.javaType}")

        val indent = if (constants.size == 1) {
            " "
        } else {
            writer.print('\n')
            "$t$t"
        }

        val alignment = constants.map { it.name.length }.fold(0) { left, right -> max(left, right) }

        constants.forEachWithMore { constant, more ->
            if (more)
                writer.println(',')
            writer.print("$indent${getConstantName(constant.name)}")
            (0 until alignment - constant.name.length).forEach { writer.print(' ') }
            if (!isRuntime(constant)) {
                writer.print(" = ")
                writer.print(constantValue(type, constant))
            }
        }
        writer.println(";")
    }

    /** Emits a `NAME = value;` assignment for every runtime constant. */
    internal fun generateInitializers(writer: PrintWriter) {
        val (type, constants) = resolved()

        constants.forEach { constant ->
            if (isRuntime(constant)) {
                writer.print("$t${t}${getConstantName(constant.name)} = ")
                writer.print(constantValue(type, constant))
                writer.println(";")
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun constantValue(type: ConstantType<*>, constant: Constant<*>): String =
        if (constant is ConstantExpression) {
            val value = if (type !== StringConstant || constant.unwrapped) constant.expression else (type as ConstantType<Any>).print(constant.expression)
            inlineMacroCalls(value, expressionMacros)
        } else
            (type as ConstantType<Any>).print(constant.value!!)

    private fun isRuntime(constant: Constant<*>) = getConstantName(constant.name) in runtimeNames

}

/** True when the expression is a runtime (non compile-time constant) expression, i.e. it invokes a method. */
internal fun isRuntimeExpression(expression: String) = METHOD_CALL.containsMatchIn(expression)

/**
 * Replaces calls to expression macros with their expression, substituting call arguments for parameters.
 * Expansion is recursive so nested macros are handled in a single pass, and recursive definitions are rejected.
 */
internal fun inlineMacroCalls(expression: String, macros: Map<String, Pair<List<String>, String>>): String {
    if (macros.isEmpty() || expression.isEmpty())
        return expression

    return expandMacroCalls(expression, macros, emptySet())
}

private fun expandMacroCalls(
    expression: String,
    macros: Map<String, Pair<List<String>, String>>,
    expanding: Set<String>
): String {
    val result = StringBuilder(expression.length)
    var index = 0

    while (index < expression.length) {
        val c = expression[index]
        if (!c.isJavaIdentifierStart()) {
            result.append(c)
            index++
            continue
        }

        val start = index++
        while (index < expression.length && expression[index].isJavaIdentifierPart())
            index++

        val name = expression.substring(start, index)
        val definition = macros[name]
        if (definition == null) {
            result.append(name)
            continue
        }

        var open = index
        while (open < expression.length && expression[open].isWhitespace())
            open++

        if (open >= expression.length || expression[open] != '(') {
            result.append(name)
            continue
        }

        val close = matchingParen(expression, open)
        if (close < 0) {
            result.append(name)
            continue
        }

        check(name !in expanding) {
            "Recursive expression macro: $name"
        }

        val arguments = splitArguments(expression.substring(open + 1, close))
        val (parameters, body) = definition
        var expanded = body
        parameters.forEachIndexed { i, parameter ->
            if (i < arguments.size)
                expanded = replaceIdentifier(expanded, parameter, arguments[i])
        }

        result.append('(')
        result.append(expandMacroCalls(expanded, macros, expanding + name))
        result.append(')')
        index = close + 1
    }

    return result.toString()
}

private fun replaceIdentifier(expression: String, name: String, replacement: String): String {
    if (expression.isEmpty() || name.isEmpty())
        return expression

    val result = StringBuilder(expression.length)
    var index = 0

    while (index < expression.length) {
        val match = expression.indexOf(name, index)
        if (match < 0) {
            result.append(expression, index, expression.length)
            break
        }

        val before = match == 0 || !expression[match - 1].isJavaIdentifierPart()
        val end = match + name.length
        val after = end == expression.length || !expression[end].isJavaIdentifierPart()
        if (before && after) {
            result.append(expression, index, match)
            result.append(replacement)
            index = end
        } else {
            result.append(expression, index, end)
            index = end
        }
    }

    return result.toString()
}

private fun matchingParen(s: String, openIndex: Int): Int {
    var depth = 0
    for (i in openIndex until s.length) {
        when (s[i]) {
            '(' -> depth++
            ')' -> {
                depth--
                if (depth == 0) return i
            }
        }
    }
    return -1
}

private fun splitArguments(s: String): List<String> {
    if (s.isBlank())
        return emptyList()

    val arguments = ArrayList<String>()
    var depth = 0
    var start = 0
    for (i in s.indices) {
        when (s[i]) {
            '(', '[' -> depth++
            ')', ']' -> depth--
            ',' -> if (depth == 0) {
                arguments.add(s.substring(start, i).trim())
                start = i + 1
            }
        }
    }
    arguments.add(s.substring(start).trim())
    return arguments
}

private val METHOD_CALL = Regex("""[A-Za-z_$][\w$.]*\(""")
