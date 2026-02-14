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
        // Listener de conexión
        socketIOServer.addConnectListener(ConnectListener { client ->
            val userId = client.handshakeData.getSingleUrlParam("userId")
            println("Cliente conectado: ${client.sessionId}, userId: $userId")

            // Unir al room del usuario si tiene userId
            userId?.let {
                client.joinRoom("user_$it")
                println("Usuario $it unido a room user_$it")
            }
        })

        // Listener de desconexión
        socketIOServer.addDisconnectListener(DisconnectListener { client ->
            println("Cliente desconectado: ${client.sessionId}")
        })

        // Evento: mensaje general
        socketIOServer.addEventListener("message", Map::class.java, DataListener { client, data, ackRequest ->
            println("Mensaje recibido de ${client.sessionId}: $data")

            // Broadcast a todos
            socketIOServer.broadcastOperations.sendEvent("message", data)

            // Acknowledment si lo solicita
            if (ackRequest.isAckRequested) {
                ackRequest.sendAckData("Mensaje recibido")
            }
        })

        // Evento: notificación privada
        socketIOServer.addEventListener("private_notification", Map::class.java, DataListener { client, data, _ ->
            val targetUserId = data["targetUserId"] as? String
            targetUserId?.let {
                socketIOServer.getRoomOperations("user_$it").sendEvent("notification", data)
                println("Notificación enviada a user_$it")
            }
        })

        // Evento: unirse a room
        socketIOServer.addEventListener("join_room", String::class.java, DataListener { client, roomName, _ ->
            client.joinRoom(roomName)
            println("Cliente ${client.sessionId} unido a room: $roomName")
        })

        // Evento: salir de room
        socketIOServer.addEventListener("leave_room", String::class.java, DataListener { client, roomName, _ ->
            client.leaveRoom(roomName)
            println("Cliente ${client.sessionId} salió de room: $roomName")
        })

        // Iniciar servidor
        try {
            socketIOServer.start()
            println("Socket.IO server iniciado en puerto ${socketIOServer.configuration.port}")
        } catch (e: Exception) {
            println("WARN: No se pudo iniciar Socket.IO en puerto ${socketIOServer.configuration.port}: ${e.message}")
        }
    }

    // Método para enviar notificación a un usuario específico
    fun sendToUser(userId: Long, event: String, data: Any) {
        socketIOServer.getRoomOperations("user_$userId").sendEvent(event, data)
    }

    // Método para broadcast a todos los clientes
    fun broadcast(event: String, data: Any) {
        socketIOServer.broadcastOperations.sendEvent(event, data)
    }

    // Método para enviar a un room específico
    fun sendToRoom(room: String, event: String, data: Any) {
        socketIOServer.getRoomOperations(room).sendEvent(event, data)
    }
}
