
plugins {
    kotlin("jvm")
    id("jps-compatible")
}

jvmTarget = "1.6"

dependencies {
    compile(project(":kotlin-script-runtime"))
    compile(project(":kotlin-stdlib"))
    compile(project(":kotlin-scripting-common"))
    compile(project(":kotlin-scripting-jvm"))
    compile(project(":kotlin-script-util"))
    compileOnly(project(":compiler:cli"))
    compileOnly(intellijCoreDep())
    runtime(projectRuntimeJar(":kotlin-compiler"))
    testCompile(commonDep("junit"))
    testCompile(project(":compiler:daemon-common")) // TODO(JPS): fix import
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

standardPublicJars()

publish()

