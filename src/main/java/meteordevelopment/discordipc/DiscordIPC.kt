package meteordevelopment.discordipc

import com.google.gson.Gson
import com.google.gson.JsonObject
import meteordevelopment.discordipc.connection.Connection
import meteordevelopment.discordipc.connection.impl.UnixConnection
import meteordevelopment.discordipc.connection.impl.WinConnection
import java.io.IOException
import java.lang.management.ManagementFactory
import java.util.Locale
import java.util.function.BiConsumer
import java.util.function.Consumer

open class DiscordIPC {
    private val UNIX_TEMP_PATHS = arrayOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")

    private val GSON = Gson()

    private var c: Connection? = null

    private var receivedDispatch = false
    private var queuedActivity: JsonObject? = null

    var user: IPCUser? = null
        private set

    val isConnected: Boolean
        get() = c != null

    private val pID: Int
        get() {
            val pr = ManagementFactory.getRuntimeMXBean().name
            return pr.substring(0, pr.indexOf('@'.code.toChar())).toInt()
        }

    fun start(appId: Long): Boolean {
        c = open { onPacket(it) }
        if (c == null) return false

        val o = JsonObject()
        o.addProperty("v", 1)
        o.addProperty("client_id", appId.toString())
        c!!.write(Opcode.Handshake, o)

        return true
    }

    fun setActivity(presence: RichPresence) {
        if (c == null) return

        queuedActivity = presence.toJson()
        if (receivedDispatch) sendActivity()
    }

    fun stop() {
        if (c != null) {
            c!!.close()

            c = null
            receivedDispatch = false
            queuedActivity = null
            user = null
        }
    }

    private fun sendActivity() {
        val args = JsonObject()
        args.addProperty("pid", pID)
        args.add("activity", queuedActivity)

        val o = JsonObject()
        o.addProperty("cmd", "SET_ACTIVITY")
        o.add("args", args)

        c!!.write(Opcode.Frame, o)
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

                user = GSON.fromJson<IPCUser>(data.getAsJsonObject("data").getAsJsonObject("user"), IPCUser::class.java)

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

            for (tempPath in UNIX_TEMP_PATHS) {
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
