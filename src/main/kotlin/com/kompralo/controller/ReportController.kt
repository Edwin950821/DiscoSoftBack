package com.kompralo.controller

import com.kompralo.dto.ReportProblemRequest
import com.kompralo.dto.ReportProblemResponse
import com.kompralo.repository.UserRepository
import com.kompralo.services.EmailService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class ReportController(
    private val emailService: EmailService,
    private val userRepository: UserRepository
) {

    @PostMapping("/api/report/problem", "/api/help/tickets")
    fun reportProblem(
        @RequestBody request: ReportProblemRequest,
        authentication: Authentication
    ): ResponseEntity<ReportProblemResponse> {
        val email = authentication.name
        val user = userRepository.findByEmail(email)
            .orElse(null) ?: return ResponseEntity.badRequest()
            .body(ReportProblemResponse(false, "Usuario no encontrado"))

        if (request.tipo.isBlank() || request.seccion.isBlank() || request.descripcion.length < 20) {
            return ResponseEntity.badRequest()
                .body(ReportProblemResponse(false, "Todos los campos son obligatorios y la descripcion debe tener al menos 20 caracteres"))
        }

        return try {
            val sent = emailService.sendProblemReport(
                userEmail = email,
                userName = user.name,
                tipo = request.tipo,
                seccion = request.seccion,
                descripcion = request.descripcion
            )

            if (sent) {
                ResponseEntity.ok(ReportProblemResponse(true, "Reporte enviado correctamente"))
            } else {
                ResponseEntity.internalServerError()
                    .body(ReportProblemResponse(false, "Error al enviar el reporte. Intenta de nuevo."))
            }
        } catch (_: Exception) {
            ResponseEntity.internalServerError()
                .body(ReportProblemResponse(false, "Error al enviar el reporte. Intenta de nuevo."))
        }
    }
}
