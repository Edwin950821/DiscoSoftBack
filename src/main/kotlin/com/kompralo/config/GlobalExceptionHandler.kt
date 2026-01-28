package com.kompralo.config

import com.kompralo.dto.ErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Maneja excepciones globalmente en toda la aplicación
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Maneja errores de validación
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(message = e.message ?: "Error de validación"))
    }

    /**
     * Maneja errores de seguridad/autorización
     */
    @ExceptionHandler(SecurityException::class)
    fun handleSecurity(e: SecurityException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ErrorResponse(message = e.message ?: "No autorizado"))
    }

    /**
     * Maneja cualquier error no capturado
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse(message = "Error interno del servidor"))
    }
}