package com.kompralo.config

import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.annotation.SpringAnnotationScanner
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import jakarta.annotation.PreDestroy

@Component
class SocketIOConfig {

    @Value("\${socketio.host:0.0.0.0}")
    private lateinit var host: String

    @Value("\${socketio.port:3001}")
    private var port: Int = 3001

    private var server: SocketIOServer? = null

    @Bean
    fun socketIOServer(): SocketIOServer {
        val config = Configuration().apply {
            hostname = host
            port = this@SocketIOConfig.port

            origin = "http://localhost:5173,http://localhost:5174,http://localhost:3000"

            isAllowCustomRequests = true
            upgradeTimeout = 10000
            pingTimeout = 60000
            pingInterval = 25000
        }

        server = SocketIOServer(config)
        return server!!
    }

    @Bean
    fun springAnnotationScanner(socketServer: SocketIOServer): SpringAnnotationScanner {
        return SpringAnnotationScanner(socketServer)
    }

    @PreDestroy
    fun stopSocketIOServer() {
        server?.stop()
    }
}
