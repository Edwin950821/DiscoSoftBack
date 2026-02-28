package com.kompralo.services

import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.ConnectListener
import com.corundumstudio.socketio.listener.DataListener
import com.corundumstudio.socketio.listener.DisconnectListener
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Service

@Service
class SocketIOService(
    private val socketIOServer: SocketIOServer
) {

    @PostConstruct
    fun startServer() {
        socketIOServer.addConnectListener(ConnectListener { client ->
            val userId = client.handshakeData.getSingleUrlParam("userId")
            println("Cliente conectado: ${client.sessionId}, userId: $userId")

            userId?.let {
                client.joinRoom("user_$it")
                println("Usuario $it unido a room user_$it")
            }
        })

        socketIOServer.addDisconnectListener(DisconnectListener { client ->
            println("Cliente desconectado: ${client.sessionId}")
        })

        socketIOServer.addEventListener("message", Map::class.java, DataListener { client, data, ackRequest ->
            println("Mensaje recibido de ${client.sessionId}: $data")

            socketIOServer.broadcastOperations.sendEvent("message", data)

            if (ackRequest.isAckRequested) {
                ackRequest.sendAckData("Mensaje recibido")
            }
        })

        socketIOServer.addEventListener("private_notification", Map::class.java, DataListener { client, data, _ ->
            val targetUserId = data["targetUserId"] as? String
            targetUserId?.let {
                socketIOServer.getRoomOperations("user_$it").sendEvent("notification", data)
                println("Notificación enviada a user_$it")
            }
        })

        socketIOServer.addEventListener("join_room", String::class.java, DataListener { client, roomName, _ ->
            client.joinRoom(roomName)
            println("Cliente ${client.sessionId} unido a room: $roomName")
        })

        socketIOServer.addEventListener("leave_room", String::class.java, DataListener { client, roomName, _ ->
            client.leaveRoom(roomName)
            println("Cliente ${client.sessionId} salió de room: $roomName")
        })

        try {
            socketIOServer.start()
            println("Socket.IO server iniciado en puerto ${socketIOServer.configuration.port}")
        } catch (e: Exception) {
            println("WARN: No se pudo iniciar Socket.IO en puerto ${socketIOServer.configuration.port}: ${e.message}")
        }
    }

    fun sendToUser(userId: Long, event: String, data: Any) {
        socketIOServer.getRoomOperations("user_$userId").sendEvent(event, data)
    }

    fun broadcast(event: String, data: Any) {
        socketIOServer.broadcastOperations.sendEvent(event, data)
    }

    fun sendToRoom(room: String, event: String, data: Any) {
        socketIOServer.getRoomOperations(room).sendEvent(event, data)
    }
}
