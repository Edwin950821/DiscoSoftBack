package com.kompralo.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "El username es requerido")
    @field:Email(message = "El email no es valido")
    val username: String,
    @field:NotBlank(message = "La contrasena es requerida")
    @field:Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    val password: String,
    @field:NotBlank(message = "El nombre de la empresa es requerido")
    @field:Size(max = 255, message = "El nombre de la empresa no puede exceder 255 caracteres")
    val company_name: String,
    @field:Email(message = "El email de la empresa no es valido")
    val company_email: String? = null,
    @field:Size(max = 20, message = "El telefono no puede exceder 20 caracteres")
    val company_phone: String? = null,
    @field:Size(max = 20, message = "El NIT no puede exceder 20 caracteres")
    val company_nit: String? = null,
    @field:Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    val owner_name: String? = null,
    @field:Size(max = 100, message = "El apellido no puede exceder 100 caracteres")
    val owner_lastname: String? = null,
    @field:Email(message = "El email del propietario no es valido")
    val owner_email: String? = null,
    val plan: String? = "FREE_TRIAL"
)
