plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.2.0"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "com.kompralo"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Security: Force newer Guava version to fix CVE-2023-2976 and CVE-2020-8908
	implementation(platform("com.google.guava:guava-bom:33.0.0-jre"))
	implementation("com.google.guava:guava")

	// Spring Boot Core
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-mail")

	// Kotlin
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	// JWT
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

	// Database
	runtimeOnly("org.postgresql:postgresql")

// Google OAuth
	implementation("com.google.api-client:google-api-client:2.2.0")
	implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
	implementation("com.google.http-client:google-http-client-gson:1.43.3")

	// Two-Factor Authentication (TOTP)
	implementation("commons-codec:commons-codec:1.16.0")

	// Testing
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
}

tasks.register<JavaExec>("runPasswordGenerator") {
	mainClass.set("com.kompralo.PasswordHashGeneratorKt")
	classpath = sourceSets["main"].runtimeClasspath
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Configuración del JAR ejecutable
tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName.set("app.jar")
	mainClass.set("com.kompralo.AuthBackendApplicationKt")
}

// Deshabilitar JAR plano
tasks.getByName<Jar>("jar") {
	enabled = false
}
