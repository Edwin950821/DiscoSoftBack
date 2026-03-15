package com.kompralo.services

import com.kompralo.exception.*
import com.kompralo.dto.*
import com.kompralo.model.*
import com.kompralo.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
@Transactional
class SettingsService(
    private val storeSettingsRepository: StoreSettingsRepository,
    private val shippingZoneRepository: ShippingZoneRepository,
    private val taxRuleRepository: TaxRuleRepository,
    private val storePolicyRepository: StorePolicyRepository,
    private val sellerProfileRepository: SellerProfileRepository,
    private val userRepository: UserRepository
) {

    companion object {
        val PAYMENT_METHOD_LABELS = mapOf(
            "CASH_ON_DELIVERY" to "Pago contra entrega",
            "CREDIT_CARD" to "Tarjeta de credito",
            "DEBIT_CARD" to "Tarjeta de debito",
            "TRANSFER" to "Transferencia bancaria",
            "PSE" to "PSE",
            "PAYPAL" to "PayPal",
            "WOMPI" to "Wompi"
        )
        val ALL_PAYMENT_METHODS = PAYMENT_METHOD_LABELS.keys.toList()
    }

    private fun findSeller(email: String): User {
        return userRepository.findByEmail(email)
            .orElseThrow { EntityNotFoundException("Vendedor", email) }
    }

    private fun getOrCreateSettings(seller: User): StoreSettings {
        return storeSettingsRepository.findBySeller(seller).orElseGet {
            storeSettingsRepository.save(StoreSettings(seller = seller))
        }
    }

    fun getStoreProfile(email: String): StoreProfileResponse {
        val seller = findSeller(email)
        val profile = sellerProfileRepository.findByUserId(seller.id!!).orElse(null)
        val settings = getOrCreateSettings(seller)

        return StoreProfileResponse(
            businessName = profile?.businessName ?: seller.name,
            businessType = profile?.businessType,
            description = profile?.description,
            logoUrl = profile?.logoUrl,
            phone = profile?.phone,
            website = profile?.website,
            taxId = profile?.taxId,
            address = profile?.address,
            city = profile?.city,
            state = profile?.state,
            postalCode = profile?.postalCode,
            country = profile?.country ?: "Colombia",
            currency = settings.currency,
            maintenanceMode = settings.maintenanceMode
        )
    }

    fun updateStoreProfile(email: String, request: UpdateStoreProfileRequest): StoreProfileResponse {
        val seller = findSeller(email)
        val profile = sellerProfileRepository.findByUserId(seller.id!!).orElseGet {
            SellerProfile(
                user = seller,
                businessName = seller.name,
                country = "Colombia",
                totalProducts = 0,
                totalReviews = 0,
                averageRating = BigDecimal.ZERO,
                totalSales = BigDecimal.ZERO
            )
        }

        request.businessName?.let {
            profile.businessName = it
            seller.name = it
            userRepository.save(seller)
        }
        request.businessType?.let { profile.businessType = it }
        request.description?.let { profile.description = it }
        request.logoUrl?.let { profile.logoUrl = it }
        request.phone?.let { profile.phone = it }
        request.website?.let { profile.website = it }
        request.taxId?.let { profile.taxId = it }
        request.address?.let { profile.address = it }
        request.city?.let { profile.city = it }
        request.state?.let { profile.state = it }
        request.postalCode?.let { profile.postalCode = it }
        request.country?.let { profile.country = it }

        sellerProfileRepository.save(profile)
        return getStoreProfile(email)
    }

    fun updateGeneralSettings(email: String, request: UpdateGeneralSettingsRequest): StoreProfileResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        request.currency?.let { settings.currency = it }
        request.maintenanceMode?.let { settings.maintenanceMode = it }

        storeSettingsRepository.save(settings)
        return getStoreProfile(email)
    }

    fun getPaymentMethods(email: String): PaymentSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)
        val enabledMethods = settings.getEnabledMethodsList()

        val methods = ALL_PAYMENT_METHODS.map { method ->
            PaymentMethodConfigResponse(
                method = method,
                label = PAYMENT_METHOD_LABELS[method] ?: method,
                enabled = enabledMethods.contains(method)
            )
        }

        return PaymentSettingsResponse(
            methods = methods,
            defaultMethod = settings.defaultPaymentMethod
        )
    }

    fun updatePaymentMethods(email: String, request: UpdatePaymentMethodsRequest): PaymentSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        settings.setEnabledMethodsList(request.enabledMethods.filter { it in ALL_PAYMENT_METHODS })
        request.defaultMethod?.let {
            if (it in request.enabledMethods) settings.defaultPaymentMethod = it
        }

        storeSettingsRepository.save(settings)
        return getPaymentMethods(email)
    }

    @Transactional(readOnly = true)
    fun getShippingZones(email: String): List<ShippingZoneResponse> {
        val seller = findSeller(email)
        return shippingZoneRepository.findBySellerOrderByCreatedAtDesc(seller).map { it.toResponse() }
    }

    fun createShippingZone(email: String, request: CreateShippingZoneRequest): ShippingZoneResponse {
        val seller = findSeller(email)
        val zone = ShippingZone(
            seller = seller,
            name = request.name,
            type = request.type,
            countries = request.countries,
            rate = request.rate,
            calculationType = request.calculationType,
            active = request.active
        )
        return shippingZoneRepository.save(zone).toResponse()
    }

    fun updateShippingZone(email: String, id: Long, request: UpdateShippingZoneRequest): ShippingZoneResponse {
        val seller = findSeller(email)
        val zone = shippingZoneRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Zona de envio", id) }
        if (zone.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")

        request.name?.let { zone.name = it }
        request.type?.let { zone.type = it }
        request.countries?.let { zone.countries = it }
        request.rate?.let { zone.rate = it }
        request.calculationType?.let { zone.calculationType = it }
        request.active?.let { zone.active = it }

        return shippingZoneRepository.save(zone).toResponse()
    }

    fun deleteShippingZone(email: String, id: Long) {
        val seller = findSeller(email)
        val zone = shippingZoneRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Zona de envio", id) }
        if (zone.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")
        shippingZoneRepository.delete(zone)
    }

    fun getTaxSettings(email: String): TaxSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)
        val rules = taxRuleRepository.findBySellerOrderByCreatedAtDesc(seller).map { it.toResponse() }

        return TaxSettingsResponse(
            rules = rules,
            taxIncludedInPrice = settings.taxIncludedInPrice,
            autoTaxUpdate = settings.autoTaxUpdate
        )
    }

    fun createTaxRule(email: String, request: CreateTaxRuleRequest): TaxRuleResponse {
        val seller = findSeller(email)
        val rule = TaxRule(
            seller = seller,
            name = request.name,
            location = request.location,
            taxType = request.taxType,
            rate = request.rate,
            active = request.active
        )
        return taxRuleRepository.save(rule).toResponse()
    }

    fun updateTaxRule(email: String, id: Long, request: UpdateTaxRuleRequest): TaxRuleResponse {
        val seller = findSeller(email)
        val rule = taxRuleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Regla fiscal", id) }
        if (rule.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")

        request.name?.let { rule.name = it }
        request.location?.let { rule.location = it }
        request.taxType?.let { rule.taxType = it }
        request.rate?.let { rule.rate = it }
        request.active?.let { rule.active = it }

        return taxRuleRepository.save(rule).toResponse()
    }

    fun deleteTaxRule(email: String, id: Long) {
        val seller = findSeller(email)
        val rule = taxRuleRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Regla fiscal", id) }
        if (rule.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")
        taxRuleRepository.delete(rule)
    }

    fun updateTaxGlobalSettings(email: String, request: UpdateTaxGlobalSettingsRequest): TaxSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        request.taxIncludedInPrice?.let { settings.taxIncludedInPrice = it }
        request.autoTaxUpdate?.let { settings.autoTaxUpdate = it }

        storeSettingsRepository.save(settings)
        return getTaxSettings(email)
    }

    fun getNotificationPreferences(email: String): NotificationPreferencesResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        return NotificationPreferencesResponse(
            notifyNewOrder = settings.notifyNewOrder,
            notifyOrderStatusChange = settings.notifyOrderStatusChange,
            notifyLowStock = settings.notifyLowStock,
            notifyNewCustomer = settings.notifyNewCustomer,
            emailNotifications = settings.emailNotifications,
            pushNotifications = settings.pushNotifications
        )
    }

    fun updateNotificationPreferences(email: String, request: UpdateNotificationPreferencesRequest): NotificationPreferencesResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        request.notifyNewOrder?.let { settings.notifyNewOrder = it }
        request.notifyOrderStatusChange?.let { settings.notifyOrderStatusChange = it }
        request.notifyLowStock?.let { settings.notifyLowStock = it }
        request.notifyNewCustomer?.let { settings.notifyNewCustomer = it }
        request.emailNotifications?.let { settings.emailNotifications = it }
        request.pushNotifications?.let { settings.pushNotifications = it }

        storeSettingsRepository.save(settings)
        return getNotificationPreferences(email)
    }

    fun getAppearanceSettings(email: String): AppearanceSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        return AppearanceSettingsResponse(
            primaryColor = settings.primaryColor,
            bannerUrl = settings.bannerUrl,
            facebookUrl = settings.facebookUrl,
            instagramUrl = settings.instagramUrl,
            twitterUrl = settings.twitterUrl
        )
    }

    fun updateAppearanceSettings(email: String, request: UpdateAppearanceSettingsRequest): AppearanceSettingsResponse {
        val seller = findSeller(email)
        val settings = getOrCreateSettings(seller)

        request.primaryColor?.let { settings.primaryColor = it }
        request.bannerUrl?.let { settings.bannerUrl = it }
        request.facebookUrl?.let { settings.facebookUrl = it }
        request.instagramUrl?.let { settings.instagramUrl = it }
        request.twitterUrl?.let { settings.twitterUrl = it }

        storeSettingsRepository.save(settings)
        return getAppearanceSettings(email)
    }

    @Transactional(readOnly = true)
    fun getPolicies(email: String): List<StorePolicyResponse> {
        val seller = findSeller(email)
        return storePolicyRepository.findBySellerOrderByCreatedAtDesc(seller).map { it.toResponse() }
    }

    fun createPolicy(email: String, request: CreateStorePolicyRequest): StorePolicyResponse {
        val seller = findSeller(email)
        val policy = StorePolicy(
            seller = seller,
            policyType = request.policyType,
            title = request.title,
            content = request.content,
            active = request.active
        )
        return storePolicyRepository.save(policy).toResponse()
    }

    fun updatePolicy(email: String, id: Long, request: UpdateStorePolicyRequest): StorePolicyResponse {
        val seller = findSeller(email)
        val policy = storePolicyRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Politica", id) }
        if (policy.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")

        request.title?.let { policy.title = it }
        request.content?.let { policy.content = it }
        request.active?.let { policy.active = it }

        return storePolicyRepository.save(policy).toResponse()
    }

    fun deletePolicy(email: String, id: Long) {
        val seller = findSeller(email)
        val policy = storePolicyRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Politica", id) }
        if (policy.seller.id != seller.id) throw UnauthorizedActionException("No autorizado")
        storePolicyRepository.delete(policy)
    }

    private fun ShippingZone.toResponse() = ShippingZoneResponse(
        id = id!!,
        name = name,
        type = type,
        countries = countries,
        rate = rate,
        calculationType = calculationType,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun TaxRule.toResponse() = TaxRuleResponse(
        id = id!!,
        name = name,
        location = location,
        taxType = taxType,
        rate = rate,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun StorePolicy.toResponse() = StorePolicyResponse(
        id = id!!,
        policyType = policyType,
        title = title,
        content = content,
        active = active,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
