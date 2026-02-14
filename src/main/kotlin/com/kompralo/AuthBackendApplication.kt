package com.kompralo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
@EntityScan(basePackages = ["com.kompralo.model"])
@EnableJpaRepositories(basePackages = ["com.kompralo.repository"])
class AuthBackendApplication

fun main(args: Array<String>) {
	runApplication<AuthBackendApplication>(*args)
}
