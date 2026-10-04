/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */

package org.lwjgl.generator

import java.lang.classfile.ClassBuilder
import java.lang.classfile.ClassFile.*
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.classfile.constantpool.*
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs.*
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.MethodHandleInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * The FFM alternative of [JNI].
 *
 * <p>The generated class has no native methods and no handle fields. Every adapter method loads its downcall handle from a dynamic constant created by
 * `Foreign.create` and invokes it with an exact `invokeExact` call-site descriptor, without a `try`/`catch` block.</p>
 *
 * <p>Only the native type string (`cType`) of each signature is kept. The cType is the sequence of native type letters, the return type first and the
 * parameters after it (e.g. `PP`, `VP`, `IPN`), so it already carries all the information required to generate both the fake source and the real bytecode.</p>
 */
open class FakeDowncall(className: String, critical: Boolean) : FakeGeneratorTarget(Module.CORE, className) {

    /** The value passed to {@code Foreign.create} for its {@code flags} argument. */
    private val handleFlags = if (critical) FOREIGN_CRITICAL else 0

    init {
        // Downcall is package-private, CriticalCall must be public (it is called from the other core sub-packages).
        flags = if (critical) ACC_PUBLIC or ACC_FINAL else ACC_FINAL
    }

    private val cTypes = ConcurrentHashMap.newKeySet<String>()

    private val sortedCTypes by lazy(LazyThreadSafetyMode.NONE) { cTypes.sorted() }

    /** Registers a function. Mirrors [JNI.register]. */
    internal fun register(function: Func) {
        val cType = function.downcallType
        cTypes.add(cType)
        // A C long is resolved to a Java int or long at runtime, so the adapters of both resolved signatures must exist for the dispatching adapter to
        // delegate to.
        if ('N' in cType) {
            cTypes.add(cType.replace('N', 'I'))
            cTypes.add(cType.replace('N', 'J'))
        }
    }

    override fun declareFake() {
        addFakeConstructor()
        sortedCTypes.forEach {
            addFakeMethod(it, adapterClass(it[0]), listOf(CD_long) + it.substring(1).map {
                adapterClass(it)
            })
        }
    }

    override fun ClassBuilder.gen() {
        val cp = constantPool()
        this@FakeDowncall.cp = cp
        bootstrapHandle = cp.methodHandleEntry(MethodHandleInfo.REF_invokeStatic,
            cp.methodRefEntry(CD_Foreign, "bootstrap", MTD_bootstrap))
        flagsEntry = cp.intEntry(handleFlags)
        withMethod(INIT_NAME, MTD_void, ACC_PRIVATE) {
            it.withCode {
                it.aload(0)
                    .invokespecial(CD_Object, INIT_NAME, MTD_void)
                    .return_()
            }
        }

        sortedCTypes.forEach { cType ->
            withMethod(cType, adapterType(cType), ACC_PUBLIC or ACC_STATIC) {
                it.withCode { emitAdapter(it, cType) }
            }
        }
    }


    private lateinit var cp: ConstantPoolBuilder
    private lateinit var bootstrapHandle: MethodHandleEntry
    private lateinit var flagsEntry: IntegerEntry
    private val CLONG_SIZE by lazy(LazyThreadSafetyMode.NONE) {
        cp.fieldRefEntry(CD_Pointer, "CLONG_SIZE", CD_int)
    }

    /**
     * Emits an adapter method.
     *
     * <p>When the signature contains a C {@code long} ({@code N}), the adapter does not embed a downcall handle. It dispatches, at runtime from
     * {@code Pointer.CLONG_SIZE}, to the adapter of the resolved signature (Java {@code int} or Java {@code long}), which owns the handle.</p>
     */
    private fun emitAdapter(cb: CodeBuilder, cType: String) {
        when {
            'N' in cType -> {
                val clong8 = cb.newLabel()
                cb.getstatic(CLONG_SIZE)
                    .loadConstant(8)
                    .if_icmpeq(clong8)
                emitDelegatingCall(cb, cType, cType.replace('N', 'I'), clongAsInt = true)
                cb.labelBinding(clong8)
                emitDelegatingCall(cb, cType, cType.replace('N', 'J'), clongAsInt = false)
            }
            else -> emitCall(cb, cType)
        }
    }

    /**
     * Emits a call to the adapter of the resolved signature, narrowing the C {@code long} parameters and widening the return value when it uses a Java
     * {@code int}.
     *
     * @param cType      the declared native type string, used for the local variable slots
     * @param target     the resolved native type string, where `N` has been replaced by `I` or `J`
     * @param clongAsInt whether the resolved signature uses a Java `int`
     */
    private fun emitDelegatingCall(cb: CodeBuilder, cType: String, target: String, clongAsInt: Boolean) {
        cb.lload(0)
        for (i in 1 until cType.length) {
            val slot = cb.parameterSlot(i)
            when {
                cType[i] == 'N' && clongAsInt -> cb.lload(slot).l2i()
                else                          -> cb.loadLocal(typeKind(cType[i]), slot)
            }
        }

        cb.invokestatic(ClassDesc.of(packageName, className), target, adapterType(target))

        when {
            cType[0] == 'N' && clongAsInt -> cb.i2l().lreturn()
            cType[0] == 'V'               -> cb.return_()
            else                          -> cb.return_(typeKind(cType[0]))
        }
    }

    /** Emits one complete handle invocation for a signature that contains no C {@code long}. */
    private fun emitCall(cb: CodeBuilder, cType: String) {
        // Foreign.create returns a handle whose type is exactly the adapter descriptor: the function address and every pointer are Java longs. The call
        // site therefore needs no MemorySegment conversions and its descriptor matches the adapter method's own descriptor, so they share the pool entry.
        emitHandleConstant(cb, cType)

        cb.lload(0)
        for (i in 1 until cType.length) {
            cb.loadLocal(typeKind(cType[i]), cb.parameterSlot(i))
        }

        cb.invokevirtual(CD_MethodHandle, "invokeExact", adapterType(cType))

        when (cType[0]) {
            'V' -> cb.return_()
            else -> cb.return_(typeKind(cType[0]))
        }
    }

    /**
     * Emits an `ldc` of a dynamic constant whose value is the downcall handle.
     *
     * The constant name is the `cType` string and the bootstrap is {@code Foreign.bootstrap}, which receives that name from the JVM. This keeps the static
     * arguments down to the handle flags and removes the per-signature `String` and `MethodType` constants from the pool.
     */
    private fun emitHandleConstant(cb: CodeBuilder, cType: String) {
        val pool = cp

        val bootstrap = pool.bsmEntry(
            bootstrapHandle,
            listOf<LoadableConstantEntry>(flagsEntry)
        )

        val nameAndType = pool.nameAndTypeEntry(pool.utf8Entry(cType), pool.utf8Entry(CD_MethodHandle.descriptorString()))
        cb.ldc(pool.constantDynamicEntry(bootstrap, nameAndType))
    }

}

object Downcall : FakeDowncall("Downcall", critical = false)

object CriticalCall : FakeDowncall("CriticalCall", critical = true)

/** The value of {@code Foreign.Flags.CRITICAL}. */
private const val FOREIGN_CRITICAL = 1

/** The `cType` of a function: the native type letters, the return type first and the parameters after it. */
val Func.downcallType
    get() = buildString {
        append(returns.nativeType.criticalCallLetter.toInt().toChar())
        parameters.asSequence()
            .filter { it !== EXPLICIT_FUNCTION_ADDRESS }
            .forEach { append(it.nativeType.criticalCallLetter.toInt().toChar()) }
    }

/** The letter used in the native type string. There is exactly one letter per distinct layout. */
val NativeType.criticalCallLetter
    get() = when {
        mapping === TypeMapping.VOID          -> 'V'
        isPointer                             -> 'P'
        mapping === PrimitiveMapping.CLONG    -> 'N'
        mapping === PrimitiveMapping.BOOLEAN  -> 'Z'
        mapping === PrimitiveMapping.BOOLEAN2 -> 'S'
        mapping === PrimitiveMapping.BOOLEAN4 -> 'I'
        mapping === PrimitiveMapping.BYTE     -> 'B'
        mapping === PrimitiveMapping.SHORT    -> 'S'
        mapping === PrimitiveMapping.INT      -> 'I'
        mapping === PrimitiveMapping.LONG     -> 'J'
        mapping === PrimitiveMapping.FLOAT    -> 'F'
        mapping === PrimitiveMapping.DOUBLE   -> 'D'
        else                                  -> throw IllegalArgumentException("Unsupported native type: $this")
    }.code.toByte()

// --[ BYTECODE GENERATION ]--

private val CD_Foreign = ClassDesc.of("org.lwjgl.system.Foreign")
private val CD_Pointer = ClassDesc.of("org.lwjgl.system.Pointer")

private val MTD_void = MethodTypeDesc.of(CD_void)

/**
 * The descriptor of `Foreign.bootstrap`, the bootstrap method of every downcall handle constant. The name of a dynamic constant is its `cType` string,
 * which the JVM passes to the bootstrap, so neither the `cType` `String` nor the `MethodType` is needed as a static argument.
 */
private val MTD_bootstrap = MethodTypeDesc.of(
    CD_MethodHandle,
    ClassDesc.of("java.lang.invoke.MethodHandles\$Lookup"),
    CD_String,
    CD_Class,
    CD_int
)

/** The Java type of an adapter method parameter (or of its return value) for a native type letter. */
private fun adapterClass(type: Char) = when (type) {
    'V'           -> CD_void
    'P', 'J', 'N' -> CD_long
    'Z'           -> CD_boolean
    'S'           -> CD_short
    'I'           -> CD_int
    'B'           -> CD_byte
    'F'           -> CD_float
    'D'           -> CD_double
    else          -> throw IllegalArgumentException("Unsupported cType letter: $type")
}

/** The JVM descriptor of the generated adapter method. */
private fun adapterType(cType: String) = MethodTypeDesc.of(adapterClass(cType[0]), *Array(cType.length - 1 + 1) {
    if (it == 0) {
        CD_long
    } else {
        adapterClass(cType[it])
    }
})

private fun typeKind(type: Char) = when (adapterClass(type)) {
    CD_long   -> TypeKind.LONG
    CD_double -> TypeKind.DOUBLE
    CD_float  -> TypeKind.FLOAT
    else      -> TypeKind.INT
}
