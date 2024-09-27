plugins {
    id("java")
}

group = "janigo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("mysql:mysql-connector-java:8.0.33")
    implementation("dev.langchain4j:langchain4j:0.35.0")
    implementation("dev.langchain4j:langchain4j-vertex-ai-gemini:0.35.0")
    implementation("dev.langchain4j:langchain4j-mistral-ai:0.35.0")
    implementation("dev.langchain4j:langchain4j-open-ai:0.35.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}