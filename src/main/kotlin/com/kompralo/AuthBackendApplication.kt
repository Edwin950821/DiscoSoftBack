package com.kompralo

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Clase principal de la aplicación Spring Boot
 * Escanea automáticamente todos los componentes en el package com.kompralo
 */
@SpringBootApplication
class AuthBackendApplication

fun main(args: Array<String>) {
	runApplication<AuthBackendApplication>(*args)
}