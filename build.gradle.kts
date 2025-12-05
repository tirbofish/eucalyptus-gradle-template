/// dropbear-engine template for gradle. its recommended to not touch it unless you
/// know what you're doing

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinxSerialization)
    id("magna-carta") version "1.0-SNAPSHOT"
}

group = "domain.projectExample"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://tirbofish.github.io/dropbear/")
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://tirbofish.github.io/dropbear/")
    }
}

val hostOs = providers.systemProperty("os.name").get()
val isArm64 = providers.systemProperty("os.arch").map { it == "aarch64" }.get()
val isMingwX64 = hostOs.startsWith("Windows")
val isLinux = hostOs == "Linux"
val isMacOs = hostOs == "Mac OS X"

val libName = when {
    isMacOs -> "libeucalyptus_core.dylib"
    isLinux -> "libeucalyptus_core.so"
    isMingwX64 -> "eucalyptus_core.dll"
    else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
}

val libPathProvider = provider {
    val candidates = listOf(
        layout.projectDirectory.file("target/debug/$libName").asFile,
        layout.projectDirectory.file("target/release/$libName").asFile,
        layout.projectDirectory.file("libs/$libName").asFile
    )

    val foundLib = candidates.firstOrNull { it.exists() }

    if (foundLib == null) {
        println(
            "The required library [$libName] does not exist. \n" +
                    "\n" +
                    "Here is how to fix it:\n" +
                    "============================================================================\n" +
                    "You have two options. You can either build it yourself or download a prebuilt one. I would assume that you are just a standard game dev, so you would most likely want a prebuilt one. \n" +
                    "\n" +
                    "a. You can download the eucalyptus_core library from https://github.com/tirbofish/dropbear   in the releases tab. \n" +
                    "Once you have the library, you can put it in the libs folder in the root of this project.\n" +
                    "\n" +
                    "In the case that there is no release, or you just want the cutting edge, you can build it yourself. \n" +
                    "\n" +
                    "b. Build instructions can be found here: https://github.com/tirbofish/dropbear/blob/main/README.md   but here it is anyways: \n" +
                    "\n" +
                    "\t1. Clone the dropbear repository. \n" +
                    "\t2. Run cargo build --release\n" +
                    "\t3. The library should be in the target/debug or target/release folder depending on how you built it (most likely the release). Copy that library into the ${project.rootDir}/libs folder. \n" +
                    "\t4. Profit!\n" +
                    "\t\n" +
                    "If there is still a further issue, please open an issue on the dropbear repository.\n" +
                    "\n" +
                    "Anyhow, glhf ꉂ(˵˃ ᗜ ˂˵)\n" +
                    "============================================================================"
        )
        ""  // Return empty string instead of throwing or returning "ERROR"
    } else {
        foundLib.absolutePath
    }
}

kotlin {
    jvm()

    val nativeTarget = when {
        isMacOs && isArm64 -> macosArm64("nativeLib")
        isMacOs && !isArm64 -> macosX64("nativeLib")
        isLinux && isArm64 -> linuxArm64("nativeLib")
        isLinux && !isArm64 -> linuxX64("nativeLib")
        isMingwX64 -> mingwX64("nativeLib")
        else -> throw GradleException("Host OS is not supported in Kotlin/Native.")
    }

    nativeTarget.apply {
        binaries {
            sharedLib {
                baseName = "projectExample"

                val nativeLibPath = libPathProvider.get()
                if (nativeLibPath.isNotEmpty()) {
                    if (isLinux || isMacOs) {
                        linkerOpts(
                            nativeLibPath,
                            "-Wl,-rpath,\\\$ORIGIN"
                        )
                    } else if (isMingwX64) {
                        linkerOpts(
                            "$nativeLibPath.lib"
                        )
                    }
                } else {
                    throw GradleException("Native library not found. Please check the error message above.")
                }
            }
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                // TODO: change this when there is a proper release
                api("com.dropbear:dropbear:1.0-SNAPSHOT")
            }
        }

        // -----------------------------------------------------------------------------------------------
        //               ENSURE THIS IS KEPT OTHERWISE MAGNA-CARTA WON'T BE ABLE TO RUN
        // -----------------------------------------------------------------------------------------------
        val jvmMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("magna-carta/jvmMain"))
        }

        val nativeLibMain by getting {
            kotlin.srcDir(layout.buildDirectory.dir("magna-carta/nativeLibMain"))
        }
        // -----------------------------------------------------------------------------------------------
    }
}

tasks.register<Jar>("fatJar") {
    archiveClassifier.set("all")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(kotlin.jvm().compilations["main"].output)

    configurations.named("jvmRuntimeClasspath").get().forEach { file ->
        if (file.name.endsWith(".jar")) {
            from(zipTree(file))
        } else {
            from(file)
        }
    }

    manifest {}
}
