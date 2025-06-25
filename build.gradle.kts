plugins {
    id("java")

    id("com.gradleup.shadow") version "8.3.7"
}

group = "com.alex"
version = "1.0.0"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // Specifies JDK 21 for compilation
    }
}

dependencies {
    implementation("org.apache.pdfbox:pdfbox:3.0.5")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation ("org.assertj:assertj-core:3.23.0")
}

// Configuration for the ShadowJar plugin
tasks.shadowJar {
    // Sets the base name of the generated JAR file (e.g., pdf-conversion-tool-1.0.0.jar)
    archiveBaseName.set("PDFMono")
    // Removes the default classifier like "-all" or "-shadow" from the JAR filename.
    archiveClassifier.set("")

    // Configures the JAR's manifest to make it executable.
    // This tells the Java Virtual Machine (JVM) which class contains the 'main' method.
    manifest {
        attributes["Main-Class"] = "com.aschwimm.pdfmono.PDFMono" // <--- IMPORTANT: REPLACE with your actual main class
    }
    // You can optionally exclude specific files from the fat JAR if they cause issues
    // or are not needed. For a simple app like this, it's usually not necessary.
    // from(sourceSets.main.resources) {
    //     exclude "application.properties"
    // }
}

// Configuration for running tests
tasks.test {
    useJUnitPlatform() // Ensures JUnit 5 tests are run correctly
}