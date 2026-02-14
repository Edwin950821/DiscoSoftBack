package com.kompralo.dto

import com.kompralo.model.CalculationType
import com.kompralo.model.PolicyType
import com.kompralo.model.ShippingType
import java.math.BigDecimal
import java.time.LocalDateTime


data class StoreProfileResponse(
    val businessName: String,
    val businessType: String?,
    val description: String?,
    val logoUrl: String?,
    val phone: String?,
    val website: String?,
    val taxId: String?,
    val address: String?,
    val city: String?,
    val state: String?,
    val postalCode: String?,
    val country: String,
    val currency: String,
    val maintenanceMode: Boolean
)

data class UpdateStoreProfileRequest(
    val businessName: String? = null,
    val businessType: String? = null,
    val description: String? = null,
    val logoUrl: String? = null,
    val phone: String? = null,
    val website: String? = null,
    val taxId: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

data class UpdateGeneralSettingsRequest(
    val currency: String? = null,
    val maintenanceMode: Boolean? = null
)


data class PaymentMethodConfigResponse(
    val method: String,
    val label: String,
    val enabled: Boolean
)

data class UpdatePaymentMethodsRequest(
    val enabledMethods: List<String>,
    val defaultMethod: String? = null
)

data class PaymentSettingsResponse(
    val methods: List<PaymentMethodConfigResponse>,
    val defaultMethod: String
)

data class ShippingZoneResponse(
    val id: Long,
    val name: String,
    val type: ShippingType,
    val countries: String,
    val rate: BigDecimal,
    val calculationType: CalculationType,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateShippingZoneRequest(
    val name: String,
    val type: ShippingType,
    val countries: String = "Colombia",
    val rate: BigDecimal,
    val calculationType: CalculationType = CalculationType.FLAT,
    val active: Boolean = true
)

data class UpdateShippingZoneRequest(
    val name: String? = null,
    val type: ShippingType? = null,
    val countries: String? = null,
    val rate: BigDecimal? = null,
    val calculationType: CalculationType? = null,
    val active: Boolean? = null
)

data class TaxRuleResponse(
    val id: Long,
    val name: String,
    val location: String,
    val taxType: String,
    val rate: BigDecimal,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class TaxSettingsResponse(
    val rules: List<TaxRuleResponse>,
    val taxIncludedInPrice: Boolean,
    val autoTaxUpdate: Boolean
)

data class CreateTaxRuleRequest(
    val name: String,
    val location: String,
    val taxType: String,
    val rate: BigDecimal,
    val active: Boolean = true
)

data class UpdateTaxRuleRequest(
    val name: String? = null,
    val location: String? = null,
    val taxType: String? = null,
    val rate: BigDecimal? = null,
    val active: Boolean? = null
)

data class UpdateTaxGlobalSettingsRequest(
    val taxIncludedInPrice: Boolean? = null,
    val autoTaxUpdate: Boolean? = null
)

data class NotificationPreferencesResponse(
    val notifyNewOrder: Boolean,
    val notifyOrderStatusChange: Boolean,
    val notifyLowStock: Boolean,
    val notifyNewCustomer: Boolean,
    val emailNotifications: Boolean,
    val pushNotifications: Boolean
)

data class UpdateNotificationPreferencesRequest(
    val notifyNewOrder: Boolean? = null,
    val notifyOrderStatusChange: Boolean? = null,
    val notifyLowStock: Boolean? = null,
    val notifyNewCustomer: Boolean? = null,
    val emailNotifications: Boolean? = null,
    val pushNotifications: Boolean? = null
)


data class AppearanceSettingsResponse(
    val primaryColor: String,
    val bannerUrl: String?,
    val facebookUrl: String?,
    val instagramUrl: String?,
    val twitterUrl: String?
)

data class UpdateAppearanceSettingsRequest(
    val primaryColor: String? = null,
    val bannerUrl: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val twitterUrl: String? = null
)


data class StorePolicyResponse(
    val id: Long,
    val policyType: PolicyType,
    val title: String,
    val content: String,
    val active: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateStorePolicyRequest(
    val policyType: PolicyType,
    val title: String,
    val content: String,
    val active: Boolean = true
)

data class UpdateStorePolicyRequest(
    val title: String? = null,
    val content: String? = null,
    val active: Boolean? = null
)
