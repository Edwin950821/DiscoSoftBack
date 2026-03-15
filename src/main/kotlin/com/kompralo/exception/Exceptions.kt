package com.kompralo.exception

sealed class DomainException(message: String) : RuntimeException(message)

class EntityNotFoundException(entity: String, id: Any) :
    DomainException("$entity no encontrado: $id")

class UnauthorizedActionException(message: String = "No autorizado") :
    DomainException(message)

class InsufficientStockException(productName: String, requested: Int, available: Int) :
    DomainException("Stock insuficiente para '$productName'. Disponible: $available, Solicitado: $requested")

class PaymentFailedException(reason: String) :
    DomainException("Error de pago: $reason")

class BusinessRuleViolationException(message: String) :
    DomainException(message)

class ResourceAlreadyExistsException(message: String) :
    DomainException(message)

class ResourceNotFoundException(message: String) :
    DomainException(message)

class ValidationException(message: String) :
    DomainException(message)
