package com.kompralo.services

import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import com.kompralo.repository.UserRepository
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SocketIOService(
    private val socketIOServer: SocketIOServer,
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) {

    private val log = LoggerFactory.getLogger(SocketIOService::class.java)

    @Value("\${socketio.enabled:true}")
    private var enabled: Boolean = true

    @PostConstruct
    fun startServer() {
        if (!enabled) {
            log.info("Socket.IO DESACTIVADO por configuracion (socketio.enabled=false)")
            return
        }

        socketIOServer.addConnectListener(ConnectListener { client ->
            val token = extractTokenFromHandshake(client)

            if (token.isNullOrBlank()) {
                log.warn("Conexion Socket.IO rechazada: sin token")
                client.disconnect()
                return@ConnectListener
            }

            try {
                if (!jwtService.validateToken(token)) {
                    log.warn("Conexion Socket.IO rechazada: token invalido")
                    client.disconnect()
                    return@ConnectListener
                }

                val email = jwtService.extractUsername(token)
                val role = jwtService.extractRole(token)
                client.set("email", email)
                client.set("role", role)

                val userId = client.handshakeData.getSingleUrlParam("userId")
                userId?.let {
                    client.joinRoom("user_$it")
                }

                val negocioId = try {
                    userRepository.findByEmail(email)
                        .or { userRepository.findByUsername(email) }
                        .orElse(null)?.negocioId?.toString()
                } catch (e: Exception) {
                    log.warn("Could not resolve negocioId for {}: {}", email, e.message)
                    null
                }
                client.set("negocioId", negocioId)

                val meseroId = client.handshakeData.getSingleUrlParam("meseroId")

                when (role) {
                    "ADMIN" -> {
                        client.joinRoom("disco_admin")
                        negocioId?.let { client.joinRoom("disco_admin_$it") }
                        log.info("Admin conectado a disco_admin{}: {}", negocioId?.let { "_$it" } ?: "", email)
                    }
                    "OWNER" -> {
                        client.joinRoom("disco_admin")
                        negocioId?.let { client.joinRoom("disco_admin_$it") }
                        log.info("Dueño conectado a disco_admin{}: {}", negocioId?.let { "_$it" } ?: "", email)
                    }
                    "MESERO" -> {
                        meseroId?.let {
                            client.joinRoom("disco_mesero_$it")
                            negocioId?.let { nid -> client.joinRoom("disco_mesero_${nid}_$it") }
                            client.set("meseroId", it)
                        }
                        client.joinRoom("disco_meseros")
                        negocioId?.let { client.joinRoom("disco_meseros_$it") }
                        log.info("Mesero conectado: {} (meseroId: {}, negocioId: {})", email, meseroId, negocioId)
                    }
                }

                log.debug("Socket.IO conectado: {} ({}) role={}", client.sessionId, email, role)
            } catch (e: Exception) {
                log.warn("Conexion Socket.IO rechazada: {}", e.message)
                client.disconnect()
            }
        })

        socketIOServer.addDisconnectListener(DisconnectListener { client ->
            log.debug("Socket.IO desconectado: {}", client.sessionId)
        })

        socketIOServer.addEventListener("message", Map::class.java, DataListener { client, data, ackRequest ->
            val email = client.get<String>("email")
            if (email == null) {
                client.disconnect()
                return@DataListener
            }

            socketIOServer.broadcastOperations.sendEvent("message", data)

            if (ackRequest.isAckRequested) {
                ackRequest.sendAckData("Mensaje recibido")
            }
        })

        socketIOServer.addEventListener("private_notification", Map::class.java, DataListener { client, data, _ ->
            val email = client.get<String>("email")
            if (email == null) {
                client.disconnect()
                return@DataListener
            }

            val targetUserId = data["targetUserId"] as? String
            targetUserId?.let {
                socketIOServer.getRoomOperations("user_$it").sendEvent("notification", data)
            }
        })

        socketIOServer.addEventListener("join_room", String::class.java, DataListener { client, roomName, _ ->
            val email = client.get<String>("email")
            if (email == null) {
                client.disconnect()
                return@DataListener
            }
            client.joinRoom(roomName)
        })

        socketIOServer.addEventListener("leave_room", String::class.java, DataListener { client, roomName, _ ->
            client.leaveRoom(roomName)
        })

        try {
            socketIOServer.start()
            log.info("Socket.IO server iniciado en puerto {}", socketIOServer.configuration.port)
        } catch (e: Exception) {
            log.warn("No se pudo iniciar Socket.IO en puerto {}: {}", socketIOServer.configuration.port, e.message)
        }
    }

    fun sendToUser(userId: Long, event: String, data: Any) {
        if (!enabled) return
        socketIOServer.getRoomOperations("user_$userId").sendEvent(event, data)
    }

    fun broadcast(event: String, data: Any) {
        if (!enabled) return
        socketIOServer.broadcastOperations.sendEvent(event, data)
    }

    fun sendToRoom(room: String, event: String, data: Any) {
        if (!enabled) return
        socketIOServer.getRoomOperations(room).sendEvent(event, data)
    }

    fun sendToAdmin(event: String, data: Any, negocioId: String? = null) {
        if (!enabled) return
        if (negocioId != null) {
            socketIOServer.getRoomOperations("disco_admin_$negocioId").sendEvent(event, data)
        } else {
            socketIOServer.getRoomOperations("disco_admin").sendEvent(event, data)
        }
    }

    fun sendToMesero(meseroId: String, event: String, data: Any, negocioId: String? = null) {
        if (!enabled) return
        if (negocioId != null) {
            socketIOServer.getRoomOperations("disco_mesero_${negocioId}_$meseroId").sendEvent(event, data)
        } else {
            socketIOServer.getRoomOperations("disco_mesero_$meseroId").sendEvent(event, data)
        }
    }

    fun sendToAllMeseros(event: String, data: Any, negocioId: String? = null) {
        if (!enabled) return
        if (negocioId != null) {
            socketIOServer.getRoomOperations("disco_meseros_$negocioId").sendEvent(event, data)
        } else {
            socketIOServer.getRoomOperations("disco_meseros").sendEvent(event, data)
        }
    }

    private fun extractTokenFromHandshake(client: SocketIOClient): String? {
        val queryToken = client.handshakeData.getSingleUrlParam("token")
        if (!queryToken.isNullOrBlank()) return queryToken

        val cookieHeader = client.handshakeData.httpHeaders.get("Cookie") ?: return null
        val match = Regex("authToken=([^;]+)").find(cookieHeader)
        return match?.groupValues?.get(1)
    }
}
