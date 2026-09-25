/*
 * Copyright (c) 2026-present OblivRuinDev. All rights reserved.
 * License terms: https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md
 *
 * Modified from LWJGL source code.
 * Original copyright notice below.
 */
/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
import java.net.URI

plugins {
    `java-platform`
    `maven-publish`
    signing
}

val fjglVersion: String by project
val signingKeyId = "263D4349"
val signingKey: String? by project
val signingPassword: String? by project
val sonatypeUsername: String by project
val sonatypePassword: String by project

defaultTasks = mutableListOf("publish")
layout.buildDirectory.set(layout.projectDirectory.dir("bin/MAVEN"))
group = "dev.oblivruin.fjgl"

enum class BuildType {
    LOCAL,
    SNAPSHOT,
    RELEASE
}

data class Deployment(
    val type: BuildType,
    val repo: URI,
    val version: String
)

val deployment = when {
    hasProperty("release") -> Deployment(
        type = BuildType.RELEASE,
        repo = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/"),
        version = fjglVersion
    )
    hasProperty("snapshot") -> Deployment(
        type = BuildType.SNAPSHOT,
        repo = uri("https://central.sonatype.com/repository/maven-snapshots/"),
        version = "$fjglVersion-SNAPSHOT"
    )
    else -> Deployment(
        type = BuildType.LOCAL,
        repo = repositories.mavenLocal().url,
        version = "$fjglVersion-SNAPSHOT"
    )
}
version = deployment.version
println("${deployment.type.name} BUILD")

val artifactsRoot = layout.projectDirectory.asFile

enum class Platforms(val classifier: String) {
    FREEBSD("natives-freebsd"),
    LINUX("natives-linux"),
    LINUX_ARM64("natives-linux-arm64"),
    LINUX_ARM32("natives-linux-arm32"),
    LINUX_PPC64LE("natives-linux-ppc64le"),
    LINUX_RISCV64("natives-linux-riscv64"),
    MACOS("natives-macos"),
    MACOS_ARM64("natives-macos-arm64"),
    WINDOWS("natives-windows"),
    WINDOWS_ARM64("natives-windows-arm64");

    companion object {
        val ALL = values()
    }
}

data class CustomArtifacts(
    val classifiersForBOM: List<String>,
    val classifiers: List<String>
)

enum class Module(
    val artifact: String,
    val projectName: String,
    val projectDescription: String,
    vararg val platforms: Platforms,
    val custom: CustomArtifacts? = null
) {
    CORE("fjgl", "FJGL", "The FJGL core library.", *Platforms.ALL),
    ASSIMP(
        "fjgl-assimp", "FJGL - Assimp bindings",
        "A portable Open Source library to import various well-known 3D model formats in a uniform manner.",
        *Platforms.ALL
    ),
    BGFX(
        "fjgl-bgfx", "FJGL - bgfx bindings",
        "A cross-platform, graphics API agnostic rendering library. It provides a high performance, low level abstraction for common platform graphics APIs like OpenGL, Direct3D and Apple Metal.",
        *Platforms.ALL
    ),
    EGL(
        "fjgl-egl", "FJGL - EGL bindings",
        "An interface between Khronos rendering APIs such as OpenGL ES or OpenVG and the underlying native platform window system."
    ),
    FMOD(
        "fjgl-fmod", "FJGL - FMOD bindings",
        "An end-to-end solution for adding sound and music to any game."
    ),
    FREETYPE(
        "fjgl-freetype", "FJGL - FreeType bindings",
        "A freely available software library to render fonts.",
        *Platforms.ALL
    ),
    GLFW(
        "fjgl-glfw", "FJGL - GLFW bindings",
        "A multi-platform library for OpenGL, OpenGL ES and Vulkan development on the desktop. It provides a simple API for creating windows, contexts and surfaces, receiving input and events.",
        *Platforms.ALL
    ),
    HARFBUZZ(
        "fjgl-harfbuzz", "FJGL - HarfBuzz bindings",
        "A text shaping library that allows programs to convert a sequence of Unicode input into properly formatted and positioned glyph output — for any writing system and language.",
        *Platforms.ALL
    ),
    HWLOC(
        "fjgl-hwloc", "FJGL - hwloc bindings",
        "A portable abstraction of the hierarchical topology of modern architectures, including NUMA memory nodes, sockets, shared caches, cores and simultaneous multithreading.",
        *Platforms.ALL
    ),
    JAWT(
        "fjgl-jawt", "FJGL - JAWT bindings",
        "The AWT native interface."
    ),
    JEMALLOC(
        "fjgl-jemalloc", "FJGL - jemalloc bindings",
        "A general purpose malloc implementation that emphasizes fragmentation avoidance and scalable concurrency support.",
        *Platforms.ALL
    ),
    KTX(
        "fjgl-ktx", "FJGL - KTX (Khronos Texture) bindings",
        "A lightweight container for textures for OpenGL®, Vulkan® and other GPU APIs.",
        Platforms.FREEBSD,
        Platforms.LINUX, Platforms.LINUX_ARM64, Platforms.LINUX_ARM32, Platforms.LINUX_PPC64LE, Platforms.LINUX_RISCV64,
        Platforms.MACOS, Platforms.MACOS_ARM64,
        Platforms.WINDOWS, Platforms.WINDOWS_ARM64
    ),
    LLVM(
        "fjgl-llvm", "FJGL - LLVM/Clang bindings",
        "A collection of modular and reusable compiler and toolchain technologies.",
        *Platforms.ALL
    ),
    LMDB(
        "fjgl-lmdb", "FJGL - LMDB bindings",
        "A compact, fast, powerful, and robust database that implements a simplified variant of the BerkeleyDB (BDB) API.",
        *Platforms.ALL
    ),
    LZ4(
        "fjgl-lz4", "FJGL - LZ4 bindings",
        "A lossless data compression algorithm that is focused on compression and decompression speed.",
        *Platforms.ALL
    ),
    MESHOPTIMIZER(
        "fjgl-meshoptimizer", "FJGL - meshoptimizer bindings",
        "A library that provides algorithms to help optimize meshes.",
        *Platforms.ALL
    ),
    MIMALLOC(
        "fjgl-mimalloc", "FJGL - mimalloc bindings",
        "A compact general purpose allocator with excellent performance.",
        *Platforms.ALL
    ),
    MSDFGEN(
        "fjgl-msdfgen", "FJGL - msdfgen bindings",
        "Multi-channel signed distance field generator.",
        *Platforms.ALL
    ),
    NANOVG(
        "fjgl-nanovg", "FJGL - NanoVG & NanoSVG bindings",
        "A small antialiased vector graphics rendering library for OpenGL. Also includes NanoSVG, a simple SVG parser.",
        *Platforms.ALL
    ),
    NFD(
        "fjgl-nfd", "FJGL - Native File Dialog bindings",
        "A small C library that portably invokes native file open, folder select and file save dialogs.",
        *Platforms.ALL
    ),
    NUKLEAR(
        "fjgl-nuklear", "FJGL - Nuklear bindings",
        "A minimal state immediate mode graphical user interface toolkit.",
        *Platforms.ALL
    ),
    ODBC(
        "fjgl-odbc", "FJGL - ODBC bindings",
        "A C programming language interface that makes it possible for applications to access data from a variety of database management systems (DBMSs)."
    ),
    OPENAL(
        "fjgl-openal", "FJGL - OpenAL bindings",
        "A cross-platform 3D audio API appropriate for use with gaming applications and many other types of audio applications.",
        *Platforms.ALL
    ),
    OPENCL(
        "fjgl-opencl", "FJGL - OpenCL bindings",
        "An open, royalty-free standard for cross-platform, parallel programming of diverse processors found in personal computers, servers, mobile devices and embedded platforms."
    ),
    OPENGL(
        "fjgl-opengl", "FJGL - OpenGL bindings",
        "The most widely adopted 2D and 3D graphics API in the industry, bringing thousands of applications to a wide variety of computer platforms.",
        *Platforms.ALL
    ),
    OPENGLES(
        "fjgl-opengles", "FJGL - OpenGL ES bindings",
        "A royalty-free, cross-platform API for full-function 2D and 3D graphics on embedded systems - including consoles, phones, appliances and vehicles.",
        *Platforms.ALL
    ),
    OPENXR(
        "fjgl-openxr", "FJGL - OpenXR bindings",
        "A royalty-free, open standard that provides high-performance access to Augmented Reality (AR) and Virtual Reality (VR)—collectively known as XR—platforms and devices.",
        Platforms.FREEBSD,
        Platforms.LINUX, Platforms.LINUX_ARM64, Platforms.LINUX_ARM32, Platforms.LINUX_PPC64LE, Platforms.LINUX_RISCV64,
        Platforms.WINDOWS, Platforms.WINDOWS_X86, Platforms.WINDOWS_ARM64
    ),
    OPUS(
        "fjgl-opus", "FJGL - Opus bindings",
        "A totally open, royalty-free, highly versatile audio codec.",
        *Platforms.ALL
    ),
    PAR(
        "fjgl-par", "FJGL - par_shapes bindings",
        "Generate parametric surfaces and other simple shapes.",
        *Platforms.ALL
    ),
    REMOTERY(
        "fjgl-remotery", "FJGL - Remotery bindings",
        "A realtime CPU/GPU profiler hosted in a single C file with a viewer that runs in a web browser.",
        Platforms.FREEBSD,
        Platforms.LINUX, Platforms.LINUX_ARM64, Platforms.LINUX_ARM32, Platforms.LINUX_PPC64LE, Platforms.LINUX_RISCV64,
        Platforms.MACOS, Platforms.MACOS_ARM64,
        Platforms.WINDOWS, Platforms.WINDOWS_X86
    ),
    RENDERDOC(
        "fjgl-renderdoc", "FJGL - RenderDoc bindings",
        "An API to control the RenderDoc debugger."
    ),
    RPMALLOC(
        "fjgl-rpmalloc", "FJGL - rpmalloc bindings",
        "A public domain cross platform lock free thread caching 16-byte aligned memory allocator implemented in C.",
        *Platforms.ALL
    ),
    SDL(
        "fjgl-sdl", "FJGL - SDL bindings",
        "Simple DirectMedia Layer is a cross-platform development library designed to provide low level access to audio, keyboard, mouse, joystick, and graphics hardware.",
        *Platforms.ALL
    ),
    SHADERC(
        "fjgl-shaderc", "FJGL - Shaderc bindings",
        "A collection of libraries for shader compilation.",
        *Platforms.ALL
    ),
    SPNG(
        "fjgl-spng", "FJGL - spng bindings",
        "libspng (simple png) is a C library for reading and writing Portable Network Graphics (PNG) format files with a focus on security and ease of use.",
        *Platforms.ALL
    ),
    SPVC(
        "fjgl-spvc", "FJGL - SPIRV-Cross bindings",
        "A library for performing reflection on SPIR-V and disassembling SPIR-V back to high level languages.",
        *Platforms.ALL
    ),
    STB(
        "fjgl-stb", "FJGL - stb bindings",
        "Single-file public domain libraries for fonts, images, ogg vorbis files and more.",
        *Platforms.ALL
    ),
    TINYEXR(
        "fjgl-tinyexr", "FJGL - Tiny OpenEXR bindings",
        "A small library to load and save OpenEXR(.exr) images.",
        *Platforms.ALL
    ),
    TINYFD(
        "fjgl-tinyfd", "FJGL - Tiny File Dialogs bindings",
        "Provides basic modal dialogs.",
        *Platforms.ALL
    ),
    VMA(
        "fjgl-vma", "FJGL - Vulkan Memory Allocator bindings",
        "An easy to integrate Vulkan memory allocation library.",
        *Platforms.ALL
    ),
    VULKAN(
        "fjgl-vulkan", "FJGL - Vulkan bindings",
        "A new generation graphics and compute API that provides high-efficiency, cross-platform access to modern GPUs used in a wide variety of devices from PCs and consoles to mobile phones and embedded platforms.",
        Platforms.MACOS, Platforms.MACOS_ARM64
    ),
    XXHASH(
        "fjgl-xxhash", "FJGL - xxHash bindings",
        "An extremely fast hash algorithm, running at RAM speed limits.",
        *Platforms.ALL
    ),
    YOGA(
        "fjgl-yoga", "FJGL - Yoga bindings",
        "An open-source, cross-platform layout library that implements Flexbox.",
        *Platforms.ALL
    ),
    ZSTD(
        "fjgl-zstd", "FJGL - Zstandard bindings",
        "A fast lossless compression algorithm, targeting real-time compression scenarios at zlib-level and better compression ratios.",
        *Platforms.ALL
    );

    private fun directory(buildDir: String) = "./$buildDir/$artifact"

    private fun path() = "${directory("bin/MAVEN")}/$artifact"

    val isActive get() = File(directory("bin/RELEASE")).exists()

    fun hasArtifact(classifier: String) = File("${directory("bin/RELEASE")}/${artifact}-${classifier}.jar").exists()

    fun artifact(classifier: String? = null) =
        if (classifier === null)
            File("${path()}.jar").absoluteFile
        else
            File("${path()}-$classifier.jar").absoluteFile

}

fun PublishingExtension.setupRepository() {
    repositories {
        maven {
            url = deployment.repo

            if (deployment.type !== BuildType.LOCAL) {
                credentials {
                    username = sonatypeUsername
                    password = sonatypePassword
                }
            }
        }
    }
}

fun MavenPom.setupPom(pomName: String, pomDescription: String, pomPackaging: String) {
    name.set(pomName)
    description.set(pomDescription)
    url.set("https://github.com/OblivRuinDev/FJGL3")
    packaging = pomPackaging

    scm {
        connection.set("scm:git:https://github.com/OblivRuinDev/FJGL3.git")
        developerConnection.set("scm:git:https://github.com/OblivRuinDev/FJGL3.git")
        url.set("https://github.com/OblivRuinDev/FJGL3.git")
    }

    licenses {
        license {
            name.set("BSD-3-Clause")
            url.set("https://github.com/OblivRuinDev/FJGL3/blob/master/LICENSE.md")
            distribution.set("repo")
        }
    }

    developers {
        developer {
            id.set("OblivRuinDev")
            name.set("OblivRuinDev")
            email.set("contact@oblivruin.dev")
            url.set("https://github.com/OblivRuinDev")
        }
    }
}

Module.values().forEach { module ->
    project(":modules:lwjgl:${if (module === Module.CORE) "core" else module.artifact.removePrefix("fjgl-")}") {
        group = rootProject.group
        version = rootProject.version
        layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("bin/MAVEN/gradle/${module.artifact}"))

        plugins.apply("maven-publish")
        plugins.apply("signing")

        extensions.configure<PublishingExtension> {
            setupRepository()
            publications {
                if (module.isActive) {
                    val moduleVersion = deployment.version // do not inline: required for compatibility with --configuration-cache
                    create<MavenPublication>("maven${module.name}") {
                        artifactId = module.artifact
                        artifact(module.artifact())
                        if (module.custom != null) {
                            module.custom.classifiers.forEach {
                                artifact(module.artifact(it)) {
                                    classifier = it
                                }
                            }
                        }
                        if (deployment.type !== BuildType.LOCAL || module.hasArtifact("sources")) {
                            artifact(module.artifact("sources")) {
                                classifier = "sources"
                            }
                        }
                        if (deployment.type !== BuildType.LOCAL || module.hasArtifact("javadoc")) {
                            artifact(module.artifact("javadoc")) {
                                classifier = "javadoc"
                            }
                        }
                        module.platforms.forEach {
                            if (deployment.type !== BuildType.LOCAL || module.hasArtifact(it.classifier)) {
                                artifact(module.artifact(it.classifier)) {
                                    classifier = it.classifier
                                }
                            }
                        }

                        pom {
                            setupPom(module.projectName, module.projectDescription, "jar")

                            if (module != Module.CORE) {
                                /*
                                Ideally, we'd have the following structure:
                                -------------------------------------------
                                lwjgl
                                    lwjgl-windows (depends on lwjgl)
                                glfw (depends on lwjgl)
                                    glfw-windows (depends on glfw & lwjgl-windows)
                                stb (depends on lwjgl)
                                    stb-windows (depends on stb & lwjgl-windows)
                                -------------------------------------------
                                If a user wanted to use GLFW + stb in their project, running on
                                the Windows platform, they'd only have to define glfw-windows
                                and stb-windows as dependencies. This would automatically
                                resolve stb, glfw, lwjgl and lwjgl-windows as transitive
                                dependencies. Unfortunately, it is not possible to define such
                                a relationship between Maven artifacts when using classifiers.
                                A method to make this work is make the natives-<arch> classified
                                JARs separate artifacts. We do not do it for aesthetic reasons.
                                Instead, we assume that a tool is available (on the LWJGL website)
                                that automatically generates POM/Gradle dependency structures for
                                projects wanting to use LWJGL. The output is going to be verbose;
                                the above example is going to look like this in Gradle:
                                -------------------------------------------
                                compile 'org.lwjgl:lwjgl:$fjglVersion' // NOTE: this is optional, all binding artifacts have a dependency on lwjgl
                                    compile 'org.lwjgl:lwjgl:$fjglVersion:natives-$lwjglArch'
                                compile 'org.lwjgl:lwjgl-glfw:$fjglVersion'
                                    compile 'org.lwjgl:lwjgl-glfw:$fjglVersion:natives-$lwjglArch'
                                compile 'org.lwjgl:lwjgl-stb:$fjglVersion'
                                    compile 'org.lwjgl:lwjgl-stb:$fjglVersion:natives-$lwjglArch'
                                -------------------------------------------
                                and a whole lot more verbose in Maven. Hopefully, the automation
                                is going to alleviate the pain.
                                 */
                                withXml {
                                    asNode().appendNode("dependencies").apply {
                                        appendNode("dependency").apply {
                                            appendNode("groupId", "dev.oblivruin.fjgl")
                                            appendNode("artifactId", "fjgl")
                                            appendNode("version", moduleVersion)
                                            appendNode("scope", "compile")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        extensions.configure<SigningExtension> {
            useInMemoryPgpKeys(
                signingKeyId,
                signingKey,
                signingPassword
            )
            sign(extensions.getByType<PublishingExtension>().publications)
        }
    }
}

publishing {
    setupRepository()
    publications {
        val bomVersion = deployment.version // do not inline: required for compatibility with --configuration-cache
        create<MavenPublication>("fjglBOM") {
            from(components["javaPlatform"])
            artifactId = "fjgl-bom"

            pom {
                setupPom("FJGL BOM", "FJGL Bill of Materials.", "pom")

                withXml {
                    asElement().getElementsByTagName("dependencyManagement").item(0).apply {
                        asElement().getElementsByTagName("dependencies").item(0).apply {
                            Module.values().forEach { module ->
                                val classifiers =
                                    module.custom?.classifiersForBOM.orEmpty().asSequence() +
                                    module.platforms.map { it.classifier }

                                classifiers.forEach {
                                    appendChild(
                                        ownerDocument
                                            .createElement("dependency")
                                            .apply {
                                                appendChild(
                                                    ownerDocument
                                                        .createElement("groupId")
                                                        .apply { textContent = "dev.oblivruin.fjgl" }
                                                )
                                                appendChild(
                                                    ownerDocument
                                                        .createElement("artifactId")
                                                        .apply { textContent = module.artifact }
                                                )
                                                appendChild(
                                                    ownerDocument
                                                        .createElement("version")
                                                        .apply { textContent = bomVersion }
                                                )
                                                appendChild(
                                                    ownerDocument
                                                        .createElement("classifier")
                                                        .apply { textContent = it }
                                                )
                                            })
                                }
                            }
                        }
                    }

                    // Workaround for https://github.com/gradle/gradle/issues/7529
                    asNode()
                }
            }
        }
    }
}
tasks.named("publish") {
    dependsOn(project(":modules:lwjgl")
        .subprojects
        .map { it.tasks.named("publish") })
}

signing {
    useInMemoryPgpKeys(
        signingKeyId,
        signingKey,
        signingPassword
    )
    sign(publishing.publications)
}

val copyArchives = tasks.register<Copy>("copyArchives") {
    from("bin/RELEASE")
    include("**")
    destinationDir = layout.buildDirectory.asFile.get()
}
// run copyArchives before other tasks
allprojects {
    tasks.withType<GenerateMavenPom>().configureEach {
        dependsOn(copyArchives)
    }
    tasks.withType<Sign>().configureEach {
        dependsOn(copyArchives)
    }
    tasks.withType<PublishToMavenRepository>().configureEach {
        dependsOn(copyArchives)
    }
}

dependencies {
    constraints {
        Module.values().forEach { module ->
            api("dev.oblivruin.fjgl:${module.artifact}:$version")
        }
    }
}
