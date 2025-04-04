plugins {
    `java-library`
    id("jacoco")
    id("com.diffplug.spotless") version "6.25.0"
}
repositories {
    mavenCentral()
}
dependencies {
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:5.8.2")
    testRuntimeOnly("org.junit.platform:junit-platform-runner:1.8.2")
    testImplementation("org.mockito:mockito-core:4.3.1")
    testImplementation("jakarta.inject:jakarta.inject-tck:2.0.1")
}
tasks.test {
    useJUnitPlatform()
}
tasks.withType<Test>() {
    useJUnitPlatform()
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

spotless {
    java {
        palantirJavaFormat()
        removeUnusedImports()
        formatAnnotations()
    }

}

tasks.build {
    dependsOn(tasks.spotlessApply)
}
tasks.register("prepareKotlinBuildScriptModel") {}
