package com.kompralo.controller

import com.kompralo.dto.*
import com.kompralo.services.SettingsService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/settings")
@CrossOrigin(origins = ["http://localhost:5173"], allowCredentials = "true")
class SettingsController(
    private val settingsService: SettingsService
) {
    private val log = LoggerFactory.getLogger(SettingsController::class.java)

    @GetMapping("/store")
    fun getStoreProfile(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getStoreProfile(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener perfil")))
        }
    }

    @PutMapping("/store/profile")
    fun updateStoreProfile(
        @RequestBody request: UpdateStoreProfileRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            log.info("updateStoreProfile called by: ${authentication.name}, request: $request")
            ResponseEntity.ok(settingsService.updateStoreProfile(authentication.name, request))
        } catch (e: Exception) {
            log.error("Error en updateStoreProfile para ${authentication.name}", e)
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar perfil")))
        }
    }

    @PutMapping("/store/general")
    fun updateGeneralSettings(
        @RequestBody request: UpdateGeneralSettingsRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateGeneralSettings(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar configuracion")))
        }
    }

    @GetMapping("/payments")
    fun getPaymentMethods(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getPaymentMethods(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener metodos de pago")))
        }
    }

    @PutMapping("/payments")
    fun updatePaymentMethods(
        @RequestBody request: UpdatePaymentMethodsRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updatePaymentMethods(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar metodos de pago")))
        }
    }

    @GetMapping("/shipping")
    fun getShippingZones(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getShippingZones(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener zonas de envio")))
        }
    }

    @PostMapping("/shipping")
    fun createShippingZone(
        @RequestBody request: CreateShippingZoneRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(settingsService.createShippingZone(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear zona de envio")))
        }
    }

    @PutMapping("/shipping/{id}")
    fun updateShippingZone(
        @PathVariable id: Long,
        @RequestBody request: UpdateShippingZoneRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateShippingZone(authentication.name, id, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar zona de envio")))
        }
    }

    @DeleteMapping("/shipping/{id}")
    fun deleteShippingZone(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            settingsService.deleteShippingZone(authentication.name, id)
            ResponseEntity.noContent().build<Void>()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al eliminar zona de envio")))
        }
    }

    @GetMapping("/taxes")
    fun getTaxSettings(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getTaxSettings(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener configuracion fiscal")))
        }
    }

    @PostMapping("/taxes")
    fun createTaxRule(
        @RequestBody request: CreateTaxRuleRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(settingsService.createTaxRule(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear regla fiscal")))
        }
    }

    @PutMapping("/taxes/{id}")
    fun updateTaxRule(
        @PathVariable id: Long,
        @RequestBody request: UpdateTaxRuleRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateTaxRule(authentication.name, id, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar regla fiscal")))
        }
    }

    @DeleteMapping("/taxes/{id}")
    fun deleteTaxRule(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            settingsService.deleteTaxRule(authentication.name, id)
            ResponseEntity.noContent().build<Void>()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al eliminar regla fiscal")))
        }
    }

    @PutMapping("/taxes/global")
    fun updateTaxGlobalSettings(
        @RequestBody request: UpdateTaxGlobalSettingsRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateTaxGlobalSettings(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar configuracion fiscal")))
        }
    }

    @GetMapping("/notifications")
    fun getNotificationPreferences(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getNotificationPreferences(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener preferencias")))
        }
    }

    @PutMapping("/notifications")
    fun updateNotificationPreferences(
        @RequestBody request: UpdateNotificationPreferencesRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateNotificationPreferences(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar preferencias")))
        }
    }

    @GetMapping("/appearance")
    fun getAppearanceSettings(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getAppearanceSettings(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener apariencia")))
        }
    }

    @PutMapping("/appearance")
    fun updateAppearanceSettings(
        @RequestBody request: UpdateAppearanceSettingsRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updateAppearanceSettings(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar apariencia")))
        }
    }

    @GetMapping("/policies")
    fun getPolicies(authentication: Authentication): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.getPolicies(authentication.name))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(mapOf("message" to (e.message ?: "Error al obtener politicas")))
        }
    }

    @PostMapping("/policies")
    fun createPolicy(
        @RequestBody request: CreateStorePolicyRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.status(HttpStatus.CREATED)
                .body(settingsService.createPolicy(authentication.name, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al crear politica")))
        }
    }

    @PutMapping("/policies/{id}")
    fun updatePolicy(
        @PathVariable id: Long,
        @RequestBody request: UpdateStorePolicyRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            ResponseEntity.ok(settingsService.updatePolicy(authentication.name, id, request))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al actualizar politica")))
        }
    }

    @DeleteMapping("/policies/{id}")
    fun deletePolicy(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<*> {
        return try {
            settingsService.deletePolicy(authentication.name, id)
            ResponseEntity.noContent().build<Void>()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(mapOf("message" to (e.message ?: "Error al eliminar politica")))
        }
    }
}
