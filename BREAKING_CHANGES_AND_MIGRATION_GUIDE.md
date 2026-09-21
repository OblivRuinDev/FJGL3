# FJGL3 Breaking Changes and Migration Guide

This guide documents the breaking changes introduced by the recent restructuring of the generated native-memory view classes in FJGL3.

The changes are centered around three related areas:

* `Struct` generation and allocation
* `CustomBuffer` allocation and subclassing
* `StructBuffer` element type and size metadata

The overall goal is to change the object model used by generated bindings. `Struct` and `CustomBuffer` instances are fundamentally **views over native memory**, rather than ordinary Java objects whose identity depends on constructor execution or subclass-specific factory methods.

Because the state required to represent these views is already known from the native address and class-level metadata, derived instances can be created directly using `Unsafe.allocateInstance`. This eliminates a large amount of generated boilerplate and runtime metadata.

Across the generated bindings, the redesign removes **7,000+ generated instance methods** and **3,000+ runtime constants and the struct-reference objects they represented**.

---

## 1. Affected Commits

The relevant commits are:

| Commit     | Description                                                                      | Migration impact |
| ---------- | -------------------------------------------------------------------------------- | ---------------- |
| `2073aa94` | Make `Struct` constructors public and remove generated `create()` overrides      | Breaking         |
| `ef41da2f` | Centralize `CustomBuffer` allocation and deprecate `self()` / `create()`         | Breaking         |
| `ffad0989` | Replace `StructBuffer` element factories with `getElementClass()` and `sizeof()` | Breaking         |

---

# 2. The New Object Model

Previously, generated native-memory types relied heavily on subclass-specific Java object construction.

A typical generated type contained some combination of:

```java
protected MyStruct(long address, ByteBuffer container) {
    super(address, container);
}

@Override
protected MyStruct create(long address, ByteBuffer container) {
    return new MyStruct(address, container);
}
```

and buffers similarly contained:

```java
@Override
protected MyBuffer self() {
    return this;
}

@Override
protected MyBuffer create(
    long address,
    ByteBuffer container,
    int mark,
    int position,
    int limit,
    int capacity
) {
    return new MyBuffer(
        address,
        container,
        mark,
        position,
        limit,
        capacity
    );
}
```

`StructBuffer` additionally used an element prototype:

```java
private static final MyStruct ELEMENT_FACTORY =
    MyStruct.create(-1L);
```

The new model removes this object-oriented allocation hierarchy.

The important information for a native-memory view is instead:

```text
native address
backing container
buffer state
element class
element size
```

The Java object itself is therefore primarily a **typed view over an address**.

When a derived view is required, the runtime can allocate the object without invoking the subclass constructor:

```java
Unsafe.allocateInstance(type)
```

and initialize the fields owned by the base class directly.

This is safe under the new design because the subclass does not contain Java-side state that must be initialized by its constructor.

---

# 3. Struct Changes

## 3.1 Struct constructors are now public

Generated constructors changed from the previous protected form:

```java
protected MyStruct(long address, ByteBuffer container) {
    super(address, container);
}
```

to:

```java
public MyStruct(long address, ByteBuffer container) {
    super(address, container);
}
```

Custom `Struct` implementations should follow the new visibility.

However, the fact that the constructor is public does **not** mean that the runtime now depends on constructor invocation for every struct view.

Derived views may be created through the centralized factory instead.

---

## 3.2 Generated `create()` overrides have been removed

Generated structs no longer contain a per-class implementation such as:

```java
@Override
protected MyStruct create(long address, ByteBuffer container) {
    return new MyStruct(address, container);
}
```

Instead, the common implementation can create the requested class directly.

When explicit allocation is required, use:

```java
Struct.create(
    MyStruct.class,
    address,
    container
);
```

The important difference is that the allocation mechanism no longer needs every generated class to provide an instance factory.

This eliminates thousands of otherwise identical generated methods.

---

# 4. Critical Rule: Struct Subclasses Must Not Depend on Instance State

This is one of the most important consequences of the new allocation model.

A derived `Struct` may be created using:

```java
Unsafe.allocateInstance(...)
```

which means that the subclass constructor does not necessarily execute.

Consequently, this pattern is invalid:

```java
public final class MyStruct extends Struct<MyStruct> {

    private final SomeObject state;

    public MyStruct(long address, ByteBuffer container) {
        super(address, container);
        this.state = createState();
    }
}
```

The `state` field cannot be assumed to be initialized for instances produced through the new allocation path.

The same applies to instance field initializers:

```java
private final Foo foo = createFoo();
```

They are not guaranteed to execute when an instance is materialized using `Unsafe.allocateInstance`.

### Migration

Struct subclasses should contain no Java instance state that is required for correct operation.

Prefer:

```java
public final class MyStruct extends Struct<MyStruct> {

    public static final int SIZEOF = 16;

    public MyStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    public long fieldAddress() {
        return address() + FIELD_OFFSET;
    }
}
```

State should generally be represented by:

* native memory,
* static final metadata,
* the native address,
* or values derived from immutable class metadata.

---

# 5. CustomBuffer Changes

`CustomBuffer` received a similar architectural change.

Previously, every generated buffer needed subclass-specific implementations of:

```java
self()
```

and:

```java
create(
    long address,
    ByteBuffer container,
    int mark,
    int position,
    int limit,
    int capacity
)
```

The new implementation centralizes this functionality in `CustomBuffer`.

---

## 5.1 `self()` is no longer a subclass extension point

Previously generated buffers commonly contained:

```java
@Override
protected MyBuffer self() {
    return this;
}
```

This is no longer necessary.

The base implementation can simply perform the appropriate self-cast:

```java
(SELF) this
```

Existing overrides are deprecated as part of the migration path and should be removed from custom implementations.

---

## 5.2 `CustomBuffer.create()` is centralized

Instead of requiring:

```java
@Override
protected MyBuffer create(
    long address,
    ByteBuffer container,
    int mark,
    int position,
    int limit,
    int capacity
) {
    return new MyBuffer(
        address,
        container,
        mark,
        position,
        limit,
        capacity
    );
}
```

allocation is centralized:

```java
CustomBuffer.create(
    MyBuffer.class,
    address,
    container,
    mark,
    position,
    limit,
    capacity
);
```

Normal buffer operations such as slicing and duplication can therefore use the common implementation without requiring every generated buffer class to provide its own factory.

---

# 6. Critical Rule: CustomBuffer Subclasses Must Not Depend on Instance Fields

The same restriction that applies to `Struct` applies to `CustomBuffer`.

This is unsafe:

```java
public final class MyBuffer extends CustomBuffer<MyBuffer> {

    private int customState;

}
```

A derived buffer allocated through `Unsafe.allocateInstance` only receives the state explicitly initialized by `CustomBuffer`.

The allocation mechanism does not execute the subclass constructor and does not automatically reproduce arbitrary subclass fields.

Therefore, custom buffer state should not depend on Java instance fields.

Instead, prefer:

```text
native memory
static metadata
address-derived state
base CustomBuffer state
```

For example, if a value can be calculated from the current address and element size, calculate it when needed rather than storing it as a subclass field.

---

# 7. StructBuffer Changes

The largest visible API change in `StructBuffer` is the removal of the element prototype mechanism.

Previously, generated buffers used something similar to:

```java
private static final MyStruct ELEMENT_FACTORY =
    MyStruct.create(-1L);
```

and:

```java
@Override
protected MyStruct getElementFactory() {
    return ELEMENT_FACTORY;
}
```

This mechanism is now gone.

---

## 7.1 Replace `getElementFactory()` with `getElementClass()`

The new generated implementation uses:

```java
@Override
public Class<?> getElementClass() {
    return MyStruct.class;
}
```

The element type is now represented directly by a `Class<?>`.

This is more accurate conceptually: the buffer needs to know **which class represents an element**, not hold an otherwise meaningless Java object whose only purpose is to communicate its class.

---

## 7.2 Remove `ELEMENT_FACTORY`

Delete code such as:

```java
private static final MyStruct ELEMENT_FACTORY =
    MyStruct.create(-1L);
```

The prototype object no longer has a purpose.

It was previously necessary because the old API represented element type through an object instance.

The new API represents the same information directly:

```java
MyStruct.class
```

This eliminates a large number of unnecessary runtime objects and the generated constants referencing them.

---

## 7.3 `sizeof()` is now explicit

`StructBuffer` now requires the element size directly.

A generated buffer should provide:

```java
@Override
public int sizeof() {
    return MyStruct.SIZEOF;
}
```

The expected invariant is:

```text
getElementClass() == MyStruct.class
sizeof() == MyStruct.SIZEOF
```

These two pieces of metadata completely describe the type and memory stride of a struct buffer element.

---

# 8. Why `ELEMENT_FACTORY` Was Removed

The old implementation effectively used:

```text
Class
  ↓
prototype object
  ↓
getElementFactory()
  ↓
prototype.getClass()
```

to communicate type information.

The new implementation is:

```text
Class
  ↓
getElementClass()
```

There is no reason to instantiate a fake struct simply to obtain its runtime class.

The old prototype also represented an invalid native address in many cases. Such an object has no semantic value as a native-memory view.

The new implementation therefore avoids both:

* the allocation of the prototype object, and
* the runtime constant that retains it.

This contributes directly to the reduction in generated/runtime footprint.

---

# 9. StructBuffer Element Access

Struct buffer element access can now be conceptually reduced to:

```java
Struct.create(
    type,
    address + Integer.toUnsignedLong(index) * sizeof,
    container
);
```

where:

```text
type   = getElementClass()
sizeof = sizeof()
```

The element does not need to be created through an object-specific factory.

This is one of the central reasons the new design can eliminate generated `create()` implementations.

---

# 10. StructBuffer Migration Example

### Before

A generated buffer could contain:

```java
private static final MyStruct ELEMENT_FACTORY =
    MyStruct.create(-1L);

@Override
protected MyStruct getElementFactory() {
    return ELEMENT_FACTORY;
}

@Override
protected MyBuffer self() {
    return this;
}

@Override
protected MyBuffer create(
    long address,
    ByteBuffer container,
    int mark,
    int position,
    int limit,
    int capacity
) {
    return new MyBuffer(
        address,
        container,
        mark,
        position,
        limit,
        capacity
    );
}
```

### After

The generated implementation only needs the type metadata:

```java
@Override
public int sizeof() {
    return MyStruct.SIZEOF;
}

@Override
public Class<?> getElementClass() {
    return MyStruct.class;
}
```

No element prototype is required.

No `self()` override is required.

No subclass-specific `create()` implementation is required.

---

# 11. Custom Struct Migration

## Before

```java
public final class MyStruct extends Struct<MyStruct> {

    protected MyStruct(long address, ByteBuffer container) {
        super(address, container);
    }

    @Override
    protected MyStruct create(long address, ByteBuffer container) {
        return new MyStruct(address, container);
    }
}
```

## After

```java
public final class MyStruct extends Struct<MyStruct> {

    public MyStruct(long address, ByteBuffer container) {
        super(address, container);
    }
}
```

If explicit construction from an address is required:

```java
MyStruct struct = Struct.create(
    MyStruct.class,
    address,
    container
);
```

---

# 12. CustomBuffer Migration

## Before

```java
public final class MyBuffer extends CustomBuffer<MyBuffer> {

    @Override
    protected MyBuffer self() {
        return this;
    }

    @Override
    protected MyBuffer create(
        long address,
        ByteBuffer container,
        int mark,
        int position,
        int limit,
        int capacity
    ) {
        return new MyBuffer(
            address,
            container,
            mark,
            position,
            limit,
            capacity
        );
    }
}
```

## After

```java
public final class MyBuffer extends CustomBuffer<MyBuffer> {
}
```

If explicit derived allocation is required:

```java
MyBuffer buffer = CustomBuffer.create(
    MyBuffer.class,
    address,
    container,
    -1,
    0,
    capacity,
    capacity
);
```

---

# 13. Custom StructBuffer Migration

## Before

```java
private static final MyStruct ELEMENT_FACTORY =
    MyStruct.create(-1L);

@Override
protected MyStruct getElementFactory() {
    return ELEMENT_FACTORY;
}

@Override
protected MyBuffer self() {
    return this;
}

@Override
protected MyBuffer create(
    long address,
    ByteBuffer container,
    int mark,
    int position,
    int limit,
    int capacity
) {
    return new MyBuffer(
        address,
        container,
        mark,
        position,
        limit,
        capacity
    );
}
```

## After

```java
@Override
public int sizeof() {
    return MyStruct.SIZEOF;
}

@Override
public Class<?> getElementClass() {
    return MyStruct.class;
}
```

That is the intended generated structure.

---

# 14. Searching for Affected Code

When migrating a custom binding or extension, search for:

```text
getElementFactory
ELEMENT_FACTORY
self()
create(long, ByteBuffer
extends Struct
extends CustomBuffer
extends StructBuffer
```

Also search for subclass instance fields in custom `Struct` and `CustomBuffer` implementations.

Particular attention should be paid to constructors containing side effects or assignments to fields.

---

# 15. Constructor Semantics Have Changed

A subtle but important consequence of the new allocation mechanism is that constructors are no longer guaranteed to run for every Java object representing a native view.

For example:

```java
public MyStruct(long address, ByteBuffer container) {
    super(address, container);
    initializeSomething();
}
```

must not be relied upon for the correctness of every `MyStruct` instance.

The following operations may materialize views without invoking that constructor:

* struct element access,
* struct buffer iteration,
* spliterator access,
* buffer slicing,
* buffer duplication,
* centralized `Struct.create()`,
* centralized `CustomBuffer.create()`.

Therefore, constructors should primarily establish the base-class state when ordinary Java construction is used. They should not be the only place where required subclass state is initialized.

---

# 16. Instance Field Semantics

The following pattern should be considered incompatible with the new design:

```java
public final class MyStruct extends Struct<MyStruct> {

    private final int state;

    public MyStruct(long address, ByteBuffer container) {
        super(address, container);
        state = calculateState();
    }
}
```

Likewise:

```java
public final class MyBuffer extends CustomBuffer<MyBuffer> {

    private final Object state;

    public MyBuffer(...) {
        ...
        state = createState();
    }
}
```

The reason is not merely performance.

The fundamental problem is that these objects are now allowed to be created without executing their subclass constructors.

A subclass field therefore has no guarantee that its constructor-defined invariant exists.

### Recommended alternatives

Use:

```java
static final
```

for immutable class-level metadata.

Use:  `address()` for native-memory identity.

Use:  `sizeof()` for element size.

Use:  `getElementClass()` for element type.

Use native memory itself for state that belongs to the native object.

Use computed values when state can be derived cheaply from the address and static metadata.

---

# 17. Unsafe Allocation Is Intentional

The use of `Unsafe.allocateInstance` is deliberate.

These generated types are not ordinary stateful Java domain objects.

They are native-memory accessors whose essential identity is:

```text
(address, type, layout)
```

Executing a Java constructor for every derived view would provide little semantic value while requiring every generated subclass to maintain a construction path.

The centralized allocator instead allows the runtime to:

1. allocate the exact requested class;
2. bypass subclass construction;
3. initialize the base view state;
4. return the typed native-memory view.

This makes the allocation mechanism independent of generated subclass boilerplate.

---

# 18. Generated-Code Reduction

The redesign is also motivated by generated-code size and runtime metadata.

The old architecture multiplied several mechanisms across thousands of generated classes:

```text
Struct.create()
Struct.create() override
CustomBuffer.self()
CustomBuffer.create() override
ELEMENT_FACTORY
getElementFactory()
```

Most implementations were mechanically identical.

The new architecture moves those operations into the common base classes and represents metadata directly:

```text
Class<?> elementClass
int sizeof
native address
base buffer state
```

The result is a substantial reduction in generated code.

The current redesign removes:

* **7,000+ generated instance methods**
* **3,000+ runtime constants and their struct-reference objects**

The exact runtime performance impact depends on workload and JVM behavior. These figures describe the structural reduction in the generated/runtime representation rather than claiming a proportional application-level speedup.

---

# 19. Why the New Design Is More Appropriate for Native Views

The old model implicitly treated generated structs as conventional Java objects:

```text
constructor
    ↓
object state
    ↓
factory method
    ↓
derived object
```

The new model treats them according to their actual role:

```text
native address
      ↓
type/layout metadata
      ↓
Java view
```

For a native-memory binding, the latter is a more direct representation.

A struct at address `A` does not become a different native struct because a different Java object was allocated.

Likewise, a buffer element at:

```text
A + index * sizeof
```

does not require a pre-existing Java prototype object.

The Java object exists primarily to provide typed access to the native memory.

---

# 20. Migration Checklist

## Struct

* [ ] Make custom struct constructors public.
* [ ] Remove generated or custom `create(long, ByteBuffer)` overrides.
* [ ] Do not depend on constructor execution for required state.
* [ ] Remove subclass instance fields where possible.
* [ ] Move immutable metadata to static fields.
* [ ] Derive native state from `address()` where appropriate.
* [ ] Use `Struct.create(MyStruct.class, ...)` when explicit allocation is required.

## CustomBuffer

* [ ] Remove `self()` overrides.
* [ ] Remove subclass-specific `create(...)` overrides.
* [ ] Do not depend on subclass instance fields.
* [ ] Use `CustomBuffer.create(MyBuffer.class, ...)` when explicit allocation is required.
* [ ] Verify that all required buffer state is represented by the base class.

## StructBuffer

* [ ] Remove `ELEMENT_FACTORY`.
* [ ] Remove `getElementFactory()`.
* [ ] Implement `getElementClass()`.
* [ ] Implement `sizeof()`.
* [ ] Ensure `getElementClass()` identifies the exact element type.
* [ ] Ensure `sizeof()` equals the size of one element.
* [ ] Remove obsolete `self()` and `create()` overrides.

## Generated bindings

* [ ] Update the generator.
* [ ] Regenerate bindings.
* [ ] Recompile all generated modules.
* [ ] Recompile downstream projects.
* [ ] Search custom extensions for the removed APIs.
* [ ] Do not manually restore generated factories.

---

# 21. API Mapping

| Old API / Pattern                                    | New API / Pattern                 |
| ---------------------------------------------------- | --------------------------------- |
| `Struct` subclass `create()`                         | `Struct.create(Class, ...)`       |
| Protected generated Struct constructor               | Public Struct constructor         |
| `CustomBuffer.self()` override                       | Inherited base implementation     |
| `CustomBuffer.create()` override                     | `CustomBuffer.create(Class, ...)` |
| `ELEMENT_FACTORY`                                    | Removed                           |
| `getElementFactory()`                                | `getElementClass()`               |
| Element prototype object                             | `Class<?>` metadata               |
| Implicit StructBuffer element size through prototype | Explicit `sizeof()`               |
| Per-class allocation logic                           | Centralized allocation            |
| Constructor-based derived-view initialization        | Base-class initialization         |

---

# 22. Final Design Rule

The most important rule for custom FJGL3 native-memory view classes is:

> **A `Struct` or `CustomBuffer` subclass must be valid when materialized from its native address and base-class metadata without executing the subclass constructor.**

In practical terms:

```text
Do not require:
    constructor side effects
    subclass allocation hooks
    prototype instances
    subclass instance state

Prefer:
    native memory
    static metadata
    address()
    sizeof()
    getElementClass()
    centralized allocation
```

This keeps custom types compatible with the new `Unsafe.allocateInstance` allocation model and preserves the primary goals of the redesign:

* fewer generated methods,
* fewer runtime objects,
* less generated metadata,
* less allocation machinery,
* less virtual dispatch,
* simpler generated classes,
* and a representation that more accurately reflects the fact that FJGL3 structs and buffers are native-memory views.
