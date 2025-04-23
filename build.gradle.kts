plugins {
    id("java")
}

group = "com.alex"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    testImplementation ("org.assertj:assertj-core:3.23.0")

}

tasks.test {
    useJUnitPlatform()
}