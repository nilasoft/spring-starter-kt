import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.springframework.boot") version "2.5.6"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  kotlin("jvm") version "1.5.31"
  kotlin("plugin.spring") version "1.5.31"
  kotlin("plugin.jpa") version "1.5.31"
  kotlin("kapt") version "1.5.31"
}

group = "com.nilasoft"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_11

allOpen {
  annotation("javax.persistence.Entity")
  annotation("javax.persistence.MappedSuperclass")
  annotation("javax.persistence.Embeddable")
}

repositories {
  mavenCentral()
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-aop")
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
  implementation("org.jetbrains.kotlin:kotlin-reflect")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-hibernate5:2.13.0")
  implementation("com.fasterxml.jackson.datatype:jackson-datatype-joda:2.13.0")
  implementation("org.springdoc:springdoc-openapi-ui:1.5.12")
  implementation("org.springdoc:springdoc-openapi-kotlin:1.5.12")
  implementation("org.springdoc:springdoc-openapi-webmvc-core:1.5.12")
  implementation("com.password4j:password4j:1.5.3") { exclude("org.slf4j") }
  implementation("commons-io:commons-io:2.11.0")
  implementation("org.apache.commons:commons-lang3:3.12.0")
  implementation("org.apache.commons:commons-collections4:4.4")
  implementation("org.apache.commons:commons-text:1.9")
  implementation("commons-beanutils:commons-beanutils:1.9.4")
  implementation("commons-codec:commons-codec:1.15")
  implementation("org.apache.tika:tika-core:2.1.0")
  implementation("com.google.guava:guava:31.0.1-jre")
  implementation("io.jsonwebtoken:jjwt-api:0.11.2")
  implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
  implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")
  implementation("joda-time:joda-time:2.10.13")
  implementation("joda-time:joda-time-hibernate:1.4")
  implementation("org.jadira.usertype:usertype.core:7.0.0.CR1")
  implementation("org.zeroturnaround:zt-exec:1.12")
  implementation("com.github.javafaker:javafaker:1.0.2")
  runtimeOnly("org.postgresql:postgresql")
  kapt("org.springframework.boot:spring-boot-configuration-processor")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<KotlinCompile> {
  kotlinOptions {
    freeCompilerArgs = listOf("-Xjsr305=strict")
    jvmTarget = "11"
  }
}

tasks.withType<Test> {
  useJUnitPlatform()
}
