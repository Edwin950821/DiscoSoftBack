package com.kompralo.config

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.gmail.Gmail
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.UserCredentials
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GmailApiConfig {

    @Value("\${gmail.api.client-id}")
    private lateinit var clientId: String

    @Value("\${gmail.api.client-secret}")
    private lateinit var clientSecret: String

    @Value("\${gmail.api.refresh-token}")
    private lateinit var refreshToken: String

    @Bean
    fun gmailService(): Gmail {
        val credentials = UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .build()

        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Gmail.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("Kompralo Marketplace")
            .build()
    }
}
