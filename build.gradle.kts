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

	implementation(platform("com.google.guava:guava-bom:33.0.0-jre"))
	implementation("com.google.guava:guava")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("com.google.apis:google-api-services-gmail:v1-rev20251110-2.0.0")
	implementation("com.google.auth:google-auth-library-oauth2-http:1.23.0")
	implementation("org.eclipse.angus:angus-mail:2.0.3")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("io.jsonwebtoken:jjwt-api:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
	runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")
	runtimeOnly("org.postgresql:postgresql")
	implementation("com.google.api-client:google-api-client:2.2.0")
	implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
	implementation("com.google.http-client:google-http-client-gson:1.43.3")
	implementation("commons-codec:commons-codec:1.16.0")
	implementation("com.corundumstudio.socketio:netty-socketio:2.0.9")
	implementation("com.itextpdf:kernel:7.2.5")
	implementation("com.itextpdf:layout:7.2.5")
	implementation("com.google.zxing:core:3.5.2")
	implementation("com.google.zxing:javase:3.5.2")
	implementation("com.google.firebase:firebase-admin:9.2.0")
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


tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName.set("app.jar")
	mainClass.set("com.kompralo.AuthBackendApplicationKt")
}

tasks.getByName<Jar>("jar") {
	enabled = false
}
