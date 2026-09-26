/*
 * Copyright LWJGL. All rights reserved.
 * License terms: https://www.lwjgl.org/license
 */
package org.lwjgl;

import org.jspecify.annotations.*;

import java.io.*;
import java.lang.module.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/** This class can be used to query the LWJGL version. */
public final class Version {

    /** Current version of library. */
    public static final int
        VERSION_MAJOR    = 3,
        VERSION_MINOR    = 5,
        VERSION_REVISION = 0;

    /** The development state of the current build. */
    public static final BuildType BUILD_TYPE = BuildType.STABLE;

    private static final String versionPlain =
        String.valueOf(VERSION_MAJOR) +
        '.' + VERSION_MINOR +
        '.' + VERSION_REVISION + BUILD_TYPE.postfix;

    private static final String version;
    static {
        String  result    = "-snapshot";
        Package org_lwjgl = Version.class.getPackage();

        String specVersion = org_lwjgl.getSpecificationVersion();
        String implVersion = org_lwjgl.getImplementationVersion();
        if (specVersion != null && implVersion != null) {
            result = createImplementation(specVersion, implVersion);
        } else lab: {
            Module module = Version.class.getModule();
            if ("dev.oblivruin.fjgl".equals(module.getName())) {
                String moduleVersion = module.getDescriptor()
                    .version()
                    .map(ModuleDescriptor.Version::toString)
                    .orElse(null);

                if (moduleVersion != null) {
                    int plusIndex = moduleVersion.indexOf('+');
                    if (plusIndex != -1) {
                        result = createImplementation(
                            moduleVersion.substring(0, plusIndex),
                            moduleVersion.substring(plusIndex + 1)
                        );
                        break lab;
                    }
                }
            }
            String version1 = findImplementationFromManifest();
            if (version1 != null) {
                result = version1;
            }
        }

        version = versionPlain + result;
    }

    private Version() {
    }

    public static void main(String[] args) {
        System.out.println(version);
        System.err.println(versionPlain);
    }

    /** Returns the LWJGL version. */
    public static String getVersion() {
        return version;
    }

    /** The development state of the current build. */
    public enum BuildType {
        /** Work in progress, unstable. */
        ALPHA("a"),
        /** Feature complete, unstable. */
        BETA("b"),
        /** Feature complete, stable, official release. */
        STABLE("");

        public final String postfix;

        BuildType(String postfix) {
            this.postfix = postfix;
        }
    }

    static String createImplementation(String specVersion, String implVersion) {
        String build = "+" + (implVersion.startsWith("build ") && 6 < implVersion.length() ? implVersion.substring(6) : implVersion);

        if (specVersion.contains("SNAPSHOT") || specVersion.contains("snapshot")) {
            return "-snapshot" + build;
        }

        return build;
    }

    static @Nullable String findImplementationFromManifest() {
        ClassLoader classLoader = Version.class.getClassLoader();

        URL url = classLoader.getResource("org/lwjgl/Version.class");
        if (url != null) {
            String classURL = url.toString();
            try {
                if (classURL.startsWith("jar:")) { // running on standard JDK
                    URL manifest = Version.class.getResource("/" + JarFile.MANIFEST_NAME);

                    String version = readImplementationFromManifest(Objects.requireNonNull(manifest));
                    if (version != null) {
                        return version;
                    }
                } else if (classURL.startsWith("resource:")) { // running on GraalVM native image
                    Enumeration<URL> e = classLoader.getResources(JarFile.MANIFEST_NAME);
                    while (e.hasMoreElements()) {
                        String version = readImplementationFromManifest(e.nextElement());
                        if (version != null) {
                            return version;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private static @Nullable String readImplementationFromManifest(URL url) {
        try (InputStream stream = url.openStream()) {
            Attributes attribs = new Manifest(stream).getMainAttributes();

            // make sure this is the manifest from fjgl.jar
            if (!"fjgl".equals(attribs.getValue(Attributes.Name.IMPLEMENTATION_TITLE))) {
                return null;
            }
            if (!"OblivRuinDev".equals(attribs.getValue(Attributes.Name.IMPLEMENTATION_VENDOR))) {
                return null;
            }

            String specVersion = attribs.getValue(Attributes.Name.SPECIFICATION_VERSION);
            String implVersion = attribs.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            if (specVersion == null || implVersion == null) {
                return null;
            }

            return createImplementation(specVersion, implVersion);
        } catch (Exception ignored) {
            return null;
        }
    }

}