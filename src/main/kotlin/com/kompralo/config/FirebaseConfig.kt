package com.kompralo.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials-file}")
    private val credentialsFile: Resource
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @PostConstruct
    fun init() {
        if (FirebaseApp.getApps().isEmpty()) {
            try {
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentialsFile.inputStream))
                    .build()
                FirebaseApp.initializeApp(options)
                logger.info("Firebase Admin SDK initialized successfully")
            } catch (e: Exception) {
                logger.warn("Firebase Admin SDK not initialized: ${e.message}. Push notifications will be disabled.")
            }
        }
    }
}
