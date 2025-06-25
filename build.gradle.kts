plugins {
    id("java")
}

group = "com.alex"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
    implementation("org.apache.pdfbox:pdfbox:3.0.5")
    testImplementation ("org.assertj:assertj-core:3.23.0")

}

tasks.test {
    useJUnitPlatform()
}