package com.kompralo

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

fun main() {
    val encoder = BCryptPasswordEncoder()

    println("=== GENERADOR DE PASSWORDS HASHEADAS ===\n")

    // Admin
    val adminPass = "Admin123!"
    val adminHash = encoder.encode(adminPass)
    println("ADMIN:")
    println("Password: $adminPass")
    println("Hash: $adminHash\n")

    // User
    val userPass = "User123!"
    val userHash = encoder.encode(userPass)
    println("USER:")
    println("Password: $userPass")
    println("Hash: $userHash\n")

    // Seller
    val sellerPass = "Seller123!"
    val sellerHash = encoder.encode(sellerPass)
    println("SELLER:")
    println("Password: $sellerPass")
    println("Hash: $sellerHash")
}