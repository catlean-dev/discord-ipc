package funny.catlean.discordipc

import com.google.gson.Gson
import com.google.gson.JsonObject
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.connection.impl.UnixConnection
import funny.catlean.discordipc.connection.impl.WinConnection
import java.io.IOException
import java.lang.management.ManagementFactory
import java.util.Locale
import java.util.function.Consumer

open class DiscordIPC {
    private val unixTempPaths = arrayOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")

    private val gson = Gson()

    private var connection: Connection? = null

    private var receivedDispatch = false
    private var queuedActivity: JsonObject? = null

    val noUser = IPCUser()

    var user = noUser
        private set

    val isConnected: Boolean
        get() = connection != null

    private val pID: Int
        get() {
            val pr = ManagementFactory.getRuntimeMXBean().name
            return pr.substring(0, pr.indexOf('@'.code.toChar())).toInt()
        }

    fun start(appId: Long): Boolean {
        connection = open { onPacket(it) }
        if (connection == null) return false

        val o = JsonObject()
        o.addProperty("v", 1)
        o.addProperty("client_id", appId.toString())
        connection!!.write(Opcode.Handshake, o)

        return true
    }

    fun setActivity(presence: JsonObject) {
        if (connection == null) return
        queuedActivity = presence
        if (receivedDispatch) sendActivity()
    }

    fun stop() {
        if (connection != null) {
            connection!!.close()

            connection = null
            receivedDispatch = false
            queuedActivity = null
            user = noUser
        }
    }

    private fun sendActivity() {
        val args = JsonObject()
        args.addProperty("pid", pID)
        args.add("activity", queuedActivity)

        val o = JsonObject()
        o.addProperty("cmd", "SET_ACTIVITY")
        o.add("args", args)

        connection!!.write(Opcode.Frame, o)
        queuedActivity = null
    }

    private fun onPacket(packet: Packet) {
        val data = packet.data

        if (packet.opcode == Opcode.Close) {
            error("Discord IPC error ${data.get("code").asInt} with message: ${data.get("message").asString}")
            stop()
        } else if (packet.opcode == Opcode.Frame) {
            if (data.has("evt") && data.get("evt").asString == "ERROR") {
                val d = data.getAsJsonObject("data")
                error("Discord IPC error ${d.get("code").asInt} with message: ${d.get("message").asString}")
            } else if (data.has("cmd") && data.get("cmd").getAsString() == "DISPATCH") {
                receivedDispatch = true

                user = gson.fromJson<IPCUser>(data.getAsJsonObject("data").getAsJsonObject("user"), IPCUser::class.java)

                if (queuedActivity != null) sendActivity()
            }
        }
    }

    fun open(callback: Consumer<Packet>): Connection? {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())

        if (os.contains("win")) {
            repeat(10) {
                try {
                    return WinConnection("\\\\?\\pipe\\discord-ipc-$it", callback)
                } catch (_: IOException) { }
            }
        } else {
            var name: String? = null

            for (tempPath in unixTempPaths) {
                name = System.getenv(tempPath)
                if (name != null) break
            }

            if (name == null) name = "/tmp"
            name += "/discord-ipc-"

            repeat(10) {
                try {
                    return UnixConnection(name + it, callback)
                } catch (_: IOException) {
                }
            }
        }

        return null
    }
}
