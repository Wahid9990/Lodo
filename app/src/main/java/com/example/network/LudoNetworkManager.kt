package com.example.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.example.model.PlayerColor
import com.example.model.PlayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

data class NetworkPlayerInfo(
    val color: PlayerColor,
    val name: String,
    val isHost: Boolean,
    val isConnected: Boolean,
    val type: PlayerType = PlayerType.HUMAN
)

data class NetworkRoomState(
    val isHost: Boolean = false,
    val isConnected: Boolean = false,
    val roomCode: String = "",
    val hostIp: String = "",
    val myColor: PlayerColor = PlayerColor.RED,
    val players: List<NetworkPlayerInfo> = emptyList(),
    val statusMessage: String = "Not connected"
)

sealed class NetworkEvent {
    data class RoomUpdated(val roomState: NetworkRoomState) : NetworkEvent()
    data class GameStarted(val gameStateJson: String) : NetworkEvent()
    data class GameSynced(val gameStateJson: String) : NetworkEvent()
    data class ActionRollDice(val color: PlayerColor) : NetworkEvent()
    data class ActionMoveToken(val color: PlayerColor, val tokenId: Int) : NetworkEvent()
    data class Error(val message: String) : NetworkEvent()
}

class LudoNetworkManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var serverJob: Job? = null
    private var clientJob: Job? = null

    private var serverSocket: ServerSocket? = null
    private var clientSocket: Socket? = null

    // For Host: map of color to connected client socket + printwriter
    private val clientSockets = mutableMapOf<PlayerColor, Socket>()
    private val clientWriters = mutableMapOf<PlayerColor, PrintWriter>()

    // For Client: writer to host
    private var hostWriter: PrintWriter? = null

    private val _roomState = MutableStateFlow(NetworkRoomState())
    val roomState: StateFlow<NetworkRoomState> = _roomState.asStateFlow()

    var eventListener: ((NetworkEvent) -> Unit)? = null

    companion object {
        const val PORT = 8888

        fun getLocalIpAddress(context: Context): String {
            try {
                // Try WifiManager first
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val ipInt = wifiManager?.connectionInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    return String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }

                // Fallback to NetworkInterfaces (e.g., Hotspot wlan0/ap0)
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                            val host = addr.hostAddress ?: ""
                            if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                                return host
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return "127.0.0.1"
        }

        fun getGatewayIpAddress(context: Context): String? {
            try {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val dhcpInfo = wifiManager?.dhcpInfo
                if (dhcpInfo != null && dhcpInfo.gateway != 0) {
                    val ipInt = dhcpInfo.gateway
                    return String.format(
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun isNetworkActive(context: Context): Boolean {
            val ip = getLocalIpAddress(context)
            return ip != "127.0.0.1" && ip.isNotBlank()
        }

        fun ipToRoomCode(ip: String): String {
            val parts = ip.split(".")
            if (parts.size == 4) {
                val p3 = parts[2].padStart(2, '0')
                val p4 = parts[3].padStart(3, '0')
                return "$p3$p4"
            }
            return "8888"
        }

        fun roomCodeToIp(code: String, localIp: String): String {
            val trimmed = code.trim()
            if (trimmed.contains(".")) {
                return trimmed // Direct IP entered
            }
            val parts = localIp.split(".")
            if (parts.size == 4 && trimmed.length >= 4) {
                val prefix = "${parts[0]}.${parts[1]}"
                val p3 = trimmed.substring(0, trimmed.length - 3).toIntOrNull() ?: parts[2].toInt()
                val p4 = trimmed.substring(trimmed.length - 3).toIntOrNull() ?: parts[3].toInt()
                return "$prefix.$p3.$p4"
            }
            return localIp
        }
    }

    fun startHost(hostName: String) {
        stopAll()
        val localIp = getLocalIpAddress(context)
        val roomCode = ipToRoomCode(localIp)

        val hostInfo = NetworkPlayerInfo(
            color = PlayerColor.RED,
            name = hostName.ifBlank { "Host (Red)" },
            isHost = true,
            isConnected = true,
            type = PlayerType.HUMAN
        )

        val initialPlayers = listOf(
            hostInfo,
            NetworkPlayerInfo(PlayerColor.GREEN, "Waiting Player 2...", isHost = false, isConnected = false, type = PlayerType.HUMAN),
            NetworkPlayerInfo(PlayerColor.YELLOW, "Waiting Player 3...", isHost = false, isConnected = false, type = PlayerType.HUMAN),
            NetworkPlayerInfo(PlayerColor.BLUE, "Waiting Player 4...", isHost = false, isConnected = false, type = PlayerType.HUMAN)
        )

        val newState = NetworkRoomState(
            isHost = true,
            isConnected = true,
            roomCode = roomCode,
            hostIp = localIp,
            myColor = PlayerColor.RED,
            players = initialPlayers,
            statusMessage = "Room active! Waiting for players to join..."
        )
        _roomState.value = newState
        eventListener?.invoke(NetworkEvent.RoomUpdated(newState))

        serverJob = scope.launch {
            try {
                val ss = ServerSocket()
                ss.reuseAddress = true
                ss.bind(java.net.InetSocketAddress(PORT))
                serverSocket = ss
                while (serverSocket?.isClosed == false) {
                    val client = serverSocket?.accept() ?: break
                    handleIncomingClient(client)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleSlotType(color: PlayerColor) {
        if (!_roomState.value.isHost) return
        val currentPlayers = _roomState.value.players.toMutableList()
        val idx = currentPlayers.indexOfFirst { it.color == color }
        if (idx != -1 && !currentPlayers[idx].isHost && !currentPlayers[idx].isConnected) {
            val currentSlot = currentPlayers[idx]
            val newType = if (currentSlot.type == PlayerType.HUMAN) PlayerType.AI else PlayerType.HUMAN
            val newName = if (newType == PlayerType.AI) "AI Bot" else "Waiting Player ${idx + 1}..."
            currentPlayers[idx] = currentSlot.copy(type = newType, name = newName)

            val updatedState = _roomState.value.copy(players = currentPlayers)
            _roomState.value = updatedState
            eventListener?.invoke(NetworkEvent.RoomUpdated(updatedState))
            broadcastRoomState(updatedState)
        }
    }

    private fun handleIncomingClient(client: Socket) {
        scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(client.getInputStream()))
                val writer = PrintWriter(client.getOutputStream(), true)

                var assignedColor: PlayerColor? = null

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawJson = line ?: continue
                    val json = JSONObject(rawJson)
                    val type = json.optString("type")

                    when (type) {
                        "JOIN_REQUEST" -> {
                            val name = json.optString("playerName", "Player")
                            val prefColorStr = json.optString("preferredColor", "")
                            val prefColor = PlayerColor.entries.find { it.name == prefColorStr }

                            val currentPlayers = _roomState.value.players.toMutableList()
                            
                            // Find an available slot
                            val targetIndex = if (prefColor != null) {
                                val idx = currentPlayers.indexOfFirst { it.color == prefColor && !it.isHost && !it.isConnected }
                                if (idx != -1) idx else currentPlayers.indexOfFirst { !it.isHost && !it.isConnected }
                            } else {
                                currentPlayers.indexOfFirst { !it.isHost && !it.isConnected }
                            }

                            if (targetIndex != -1) {
                                val slot = currentPlayers[targetIndex]
                                assignedColor = slot.color
                                clientSockets[slot.color] = client
                                clientWriters[slot.color] = writer

                                currentPlayers[targetIndex] = slot.copy(
                                    name = name,
                                    isConnected = true,
                                    type = PlayerType.HUMAN
                                )

                                val updatedState = _roomState.value.copy(
                                    players = currentPlayers,
                                    statusMessage = "${name} joined as ${slot.color.displayName}!"
                                )
                                _roomState.value = updatedState
                                eventListener?.invoke(NetworkEvent.RoomUpdated(updatedState))

                                // Broadcast ROOM_STATE to all clients
                                broadcastRoomState(updatedState)
                            } else {
                                // Room full
                                val errJson = JSONObject().apply {
                                    put("type", "ERROR")
                                    put("message", "Room is full!")
                                }
                                writer.println(errJson.toString())
                            }
                        }
                        "ACTION_ROLL" -> {
                            val colorStr = json.optString("color")
                            val color = PlayerColor.entries.find { it.name == colorStr }
                            if (color != null) {
                                eventListener?.invoke(NetworkEvent.ActionRollDice(color))
                            }
                        }
                        "ACTION_MOVE" -> {
                            val colorStr = json.optString("color")
                            val color = PlayerColor.entries.find { it.name == colorStr }
                            val tokenId = json.optInt("tokenId", -1)
                            if (color != null && tokenId != -1) {
                                eventListener?.invoke(NetworkEvent.ActionMoveToken(color, tokenId))
                            }
                        }
                    }
                }

                // Handle disconnect
                if (assignedColor != null) {
                    clientSockets.remove(assignedColor)
                    clientWriters.remove(assignedColor)
                    val currentPlayers = _roomState.value.players.toMutableList()
                    val idx = currentPlayers.indexOfFirst { it.color == assignedColor }
                    if (idx != -1) {
                        currentPlayers[idx] = currentPlayers[idx].copy(
                            name = "AI / Disconnected",
                            isConnected = false,
                            type = PlayerType.AI
                        )
                        val updated = _roomState.value.copy(players = currentPlayers, statusMessage = "${assignedColor.displayName} disconnected.")
                        _roomState.value = updated
                        eventListener?.invoke(NetworkEvent.RoomUpdated(updated))
                        broadcastRoomState(updated)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun broadcastRoomState(state: NetworkRoomState) {
        scope.launch {
            val json = JSONObject().apply {
                put("type", "ROOM_STATE")
                put("roomCode", state.roomCode)
                put("hostIp", state.hostIp)
                put("statusMessage", state.statusMessage)
                val arr = JSONArray()
                state.players.forEach { p ->
                    val obj = JSONObject().apply {
                        put("color", p.color.name)
                        put("name", p.name)
                        put("isHost", p.isHost)
                        put("isConnected", p.isConnected)
                        put("type", p.type.name)
                    }
                    arr.put(obj)
                }
                put("players", arr)
            }
            val str = json.toString()
            clientWriters.values.forEach { writer ->
                try {
                    writer.println(str)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun broadcastStartGame(gameStateJson: String) {
        scope.launch {
            val json = JSONObject().apply {
                put("type", "START_GAME")
                put("gameStateJson", gameStateJson)
            }
            val str = json.toString()
            clientWriters.values.forEach { writer ->
                try {
                    writer.println(str)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun broadcastGameState(gameStateJson: String) {
        scope.launch {
            val json = JSONObject().apply {
                put("type", "SYNC_GAME")
                put("gameStateJson", gameStateJson)
            }
            val str = json.toString()
            clientWriters.values.forEach { writer ->
                try {
                    writer.println(str)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun joinRoom(roomCodeOrIp: String, playerName: String, preferredColor: PlayerColor) {
        stopAll()
        val localIp = getLocalIpAddress(context)
        val targetIp = roomCodeToIp(roomCodeOrIp, localIp)

        _roomState.value = NetworkRoomState(
            isHost = false,
            isConnected = false,
            roomCode = roomCodeOrIp,
            hostIp = targetIp,
            myColor = preferredColor,
            statusMessage = "Connecting to $targetIp..."
        )

        clientJob = scope.launch {
            var activeSocket: Socket? = null
            try {
                // Try target IP first with 3500ms timeout
                val s = Socket()
                s.reuseAddress = true
                var connected = false
                try {
                    s.connect(java.net.InetSocketAddress(targetIp, PORT), 3500)
                    connected = true
                    activeSocket = s
                } catch (e1: Exception) {
                    s.close()
                    // Fallback to Gateway IP if targetIp failed and Gateway IP exists
                    val gatewayIp = getGatewayIpAddress(context)
                    if (!gatewayIp.isNullOrBlank() && gatewayIp != targetIp && gatewayIp != "0.0.0.0" && gatewayIp != "127.0.0.1") {
                        val gwSocket = Socket()
                        gwSocket.reuseAddress = true
                        gwSocket.connect(java.net.InetSocketAddress(gatewayIp, PORT), 3500)
                        connected = true
                        activeSocket = gwSocket
                    } else {
                        throw e1
                    }
                }

                if (!connected || activeSocket == null) {
                    throw Exception("Unable to establish connection")
                }

                clientSocket = activeSocket

                val reader = BufferedReader(InputStreamReader(activeSocket.getInputStream()))
                val writer = PrintWriter(activeSocket.getOutputStream(), true)
                hostWriter = writer

                // Send JOIN_REQUEST
                val joinJson = JSONObject().apply {
                    put("type", "JOIN_REQUEST")
                    put("playerName", playerName.ifBlank { "Player" })
                    put("preferredColor", preferredColor.name)
                }
                writer.println(joinJson.toString())

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val rawJson = line ?: continue
                    val json = JSONObject(rawJson)
                    val type = json.optString("type")

                    when (type) {
                        "ROOM_STATE" -> {
                            val playersArr = json.optJSONArray("players")
                            val playerList = mutableListOf<NetworkPlayerInfo>()
                            var myAssignedColor = preferredColor

                            if (playersArr != null) {
                                for (i in 0 until playersArr.length()) {
                                    val obj = playersArr.getJSONObject(i)
                                    val colorStr = obj.optString("color")
                                    val color = PlayerColor.entries.find { it.name == colorStr } ?: PlayerColor.RED
                                    val name = obj.optString("name")
                                    val isHost = obj.optBoolean("isHost")
                                    val isConnected = obj.optBoolean("isConnected")
                                    val pTypeStr = obj.optString("type")
                                    val pType = PlayerType.entries.find { it.name == pTypeStr } ?: PlayerType.HUMAN

                                    if (name == playerName && isConnected) {
                                        myAssignedColor = color
                                    }

                                    playerList.add(
                                        NetworkPlayerInfo(
                                            color = color,
                                            name = name,
                                            isHost = isHost,
                                            isConnected = isConnected,
                                            type = pType
                                        )
                                    )
                                }
                            }

                            val updated = _roomState.value.copy(
                                isConnected = true,
                                myColor = myAssignedColor,
                                players = playerList,
                                statusMessage = "Connected to room! Waiting for host to start..."
                            )
                            _roomState.value = updated
                            eventListener?.invoke(NetworkEvent.RoomUpdated(updated))
                        }
                        "START_GAME" -> {
                            val stateJson = json.optString("gameStateJson")
                            eventListener?.invoke(NetworkEvent.GameStarted(stateJson))
                        }
                        "SYNC_GAME" -> {
                            val stateJson = json.optString("gameStateJson")
                            eventListener?.invoke(NetworkEvent.GameSynced(stateJson))
                        }
                        "ERROR" -> {
                            val msg = json.optString("message", "Error connecting")
                            _roomState.value = _roomState.value.copy(statusMessage = msg)
                            eventListener?.invoke(NetworkEvent.Error(msg))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                try { activeSocket?.close() } catch (_: Exception) {}
                clientSocket = null
                val err = "Failed to connect to room. Check Wi-Fi / Hotspot IP."
                _roomState.value = _roomState.value.copy(statusMessage = err)
                eventListener?.invoke(NetworkEvent.Error(err))
            }
        }
    }

    fun sendRollDiceAction(color: PlayerColor) {
        if (_roomState.value.isHost) {
            // Local execution handled by VM
        } else {
            scope.launch {
                val json = JSONObject().apply {
                    put("type", "ACTION_ROLL")
                    put("color", color.name)
                }
                hostWriter?.println(json.toString())
            }
        }
    }

    fun sendMoveTokenAction(color: PlayerColor, tokenId: Int) {
        if (_roomState.value.isHost) {
            // Local execution handled by VM
        } else {
            scope.launch {
                val json = JSONObject().apply {
                    put("type", "ACTION_MOVE")
                    put("color", color.name)
                    put("tokenId", tokenId)
                }
                hostWriter?.println(json.toString())
            }
        }
    }

    fun stopAll() {
        serverJob?.cancel()
        clientJob?.cancel()

        try { hostWriter?.close() } catch (_: Exception) {}
        hostWriter = null

        clientWriters.values.forEach { try { it.close() } catch (_: Exception) {} }
        clientWriters.clear()

        clientSockets.values.forEach { try { it.close() } catch (_: Exception) {} }
        clientSockets.clear()

        try { clientSocket?.close() } catch (_: Exception) {}
        clientSocket = null

        try { serverSocket?.close() } catch (_: Exception) {}
        serverSocket = null
    }
}
