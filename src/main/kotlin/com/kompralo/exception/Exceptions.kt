package com.kompralo.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Excepción cuando no se encuentra un recurso
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException(message: String) : RuntimeException(message)

/**
 * Excepción cuando hay un conflicto (ej: email duplicado)
 */
@ResponseStatus(HttpStatus.CONFLICT)
class ResourceAlreadyExistsException(message: String) : RuntimeException(message)

/**
 * Excepción para errores de validación
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
class ValidationException(message: String) : RuntimeException(message)
