package com.example

import kotlinx.serialization.SerialName
import com.corundumstudio.socketio.AckRequest
import com.corundumstudio.socketio.Configuration
import com.corundumstudio.socketio.SocketIOClient
import com.corundumstudio.socketio.SocketIOServer
import com.corundumstudio.socketio.listener.DataListener
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.Serializable

@kotlinx.serialization.Serializable
data class Player(val p1: Boolean, val p2: Boolean) : Serializable

@kotlinx.serialization.Serializable
data class PlayerSelected(@SerialName("id") val id: String, @SerialName("isSelected") val isSelected: Boolean) :
    Serializable

@kotlinx.serialization.Serializable
data class PlayerInfo(val playerId: String, val deviceId: String) : Serializable

@kotlinx.serialization.Serializable
data class CarPosition(val playerId: Int, val position: Int) : Serializable

const val id_p1 = 1
const val id_p2 = 2

object Util {
    fun connect() {
        val config = Configuration()
        config.port = 9092


        val server = SocketIOServer(config)
        server.addEventListener(EventTypes.ON_TAP.eventName, String::class.java, object : DataListener<String> {
            override fun onData(client: SocketIOClient?, data: String?, ackSender: AckRequest?) {
                logEvent(EventTypes.ON_TAP.eventName, data)
                server.allClients?.forEach { cl ->
                    if (data == id_p1.toString()) {
                        cl.sendEvent(
                            ClientWatchingEvents.ON_PLAYER_TAP_PROCESSED.eventName,
                            Player(p1 = true, p2 = false)
                        )
                    } else {
                        cl.sendEvent(
                            ClientWatchingEvents.ON_PLAYER_TAP_PROCESSED.eventName,
                            Player(p1 = false, p2 = true)
                        )
                    }
                }
            }
        })

        server.addEventListener(
            EventTypes.ON_SHOW_ACTIVE_PLAYERS_COUNT.eventName,
            String::class.java
        ) { _, data, _ ->
            logEvent(EventTypes.ON_SHOW_ACTIVE_PLAYERS_COUNT.eventName, data)
            server.allClients?.forEach { cl ->
                cl.sendEvent(
                    ClientWatchingEvents.ON_PLAYER_TAP_PROCESSED.eventName, server.allClients?.size ?: 0
                )
            }
        }

        server.addEventListener(
            EventTypes.PLAYER_SELECTED.eventName,
            String::class.java
        ) { client, data, _ ->
            try {
                logEvent(EventTypes.PLAYER_SELECTED.eventName, data)
                val playerInfo = Json.decodeFromString<PlayerInfo>(data)
                players[playerInfo.deviceId] = playerInfo.playerId
                server.allClients?.forEach { cl ->
                    cl.sendEvent(
                        ClientWatchingEvents.PLAYER_SELECTED.eventName,
                        PlayerSelected(id = playerInfo.playerId, isSelected = true)
                    )
                }
                if (players.values.size == MAX_PLAYERS_COUNT) {
                    players.clear()
                    server.allClients?.forEach { cl ->
                        cl.sendEvent(ClientWatchingEvents.PLAYERS_READY.eventName, true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        server.addEventListener(
            EventTypes.RESET_CURRENT_PLAYER.eventName,
            String::class.java
        ) { client, deviceId, _ ->
            logEvent(EventTypes.RESET_CURRENT_PLAYER.eventName, deviceId)
            val playerId = players[deviceId]
            if (playerId != null) {
                players.remove(deviceId)
                server.allClients?.forEach { cl ->
                    cl.sendEvent(
                        ClientWatchingEvents.PLAYER_SELECTED.eventName,
                        PlayerSelected(id = playerId, false)
                    )
                }
            }
        }

        server.addEventListener(
            EventTypes.ON_CAR_POSITION_CHANGED.eventName,
            String::class.java
        ) { client, data, _ ->
            logEvent(EventTypes.ON_CAR_POSITION_CHANGED.eventName, data)
            val carPosition: CarPosition = Json.decodeFromString(data)
            server.allClients?.forEach { cl ->
                cl.sendEvent(
                    ClientWatchingEvents.ON_CAR_POSITION_PROCESSED.eventName,
                    carPosition
                )
            }
        }

        server.addConnectListener {
            println("connected ----- ${it}")
        }
        server.start()
    }

    fun logEvent(eventName: String, data: Any?) {
        println("EVENT_ $eventName data = $data")
    }

    private var players = HashMap<String, String>()
    private const val MAX_PLAYERS_COUNT = 2

    enum class EventTypes(val eventName: String) {
        ON_TAP("onTap"),
        PLAYER_SELECTED("PLAYER_SELECTED"),
        RESET_CURRENT_PLAYER("RESET_CURRENT_PLAYER"),
        ON_CAR_POSITION_CHANGED("ON_CAR_POSITION_CHANGED"),
        ON_SHOW_ACTIVE_PLAYERS_COUNT("ON_SHOW_ACTIVE_PLAYERS_COUNT")
    }

    enum class ClientWatchingEvents(val eventName: String) {
        ON_PLAYER_TAP_PROCESSED("onProcessed"),
        PLAYERS_READY("PLAYERS_READY"),
        PLAYER_SELECTED("PLAYER_SELECTED"),
        ON_CAR_POSITION_PROCESSED("ON_CAR_POSITION_PROCESSED"),
        PLAYER_RESET_PROCESSED("PLAYER_RESET_PROCESSED"),
        ACTIVE_PLAYER_COUNT_RECEIVED("ACTIVE_PLAYER_COUNT")
    }
}