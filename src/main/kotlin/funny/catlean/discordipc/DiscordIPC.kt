package funny.catlean.discordipc

import com.google.gson.Gson
import com.google.gson.JsonObject
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.connection.impl.UnixConnection
import funny.catlean.discordipc.connection.impl.WinConnection
import funny.catlean.discordipc.data.IPCUser
import funny.catlean.discordipc.data.Opcode
import funny.catlean.discordipc.data.Packet
import funny.catlean.discordipc.drafts.DraftService
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.Locale
import kotlin.concurrent.thread

open class DiscordIPC {
    private val unixTempPaths = arrayOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")

    private val gson = Gson()

    private var connection: Connection? = null

    private var ready = false
    private var queuedActivity: JsonObject? = null

    val initTime = Instant.now().epochSecond

    val noUser = IPCUser()

    var user = noUser
        private set

    val isConnected: Boolean
        get() = connection != null

    private val pID: Int
        get() = ManagementFactory.getRuntimeMXBean().name.substringBefore('@'.code.toChar()).toInt()

    protected fun run() {
        if (ready) {
            sendActivity()
            DraftService.handleRoll()
        }
    }

    protected fun start(appId: Long) {
        connection?.let { stop() }

        connection = open()

        connection?.let {
            it.write(Opcode.Handshake, JsonObject().apply {
                addProperty("v", 1)
                addProperty("client_id", appId.toString())
            })

            startThread()
        }
    }

    protected fun setActivity(presence: JsonObject) {
        queuedActivity = presence
    }

    fun stop() = connection?.let {
        it.close()

        connection = null
        ready = false
        queuedActivity = null
        user = noUser
    }

    private fun sendActivity() {
        connection?.let { c ->
            queuedActivity?.let { a ->
                c.write(Opcode.Frame, JsonObject().apply {
                    addProperty("cmd", "SET_ACTIVITY")
                    add("args", JsonObject().apply {
                        addProperty("pid", pID)
                        add("activity", a)
                    })
                })
            }
        }

        queuedActivity = null
    }

    fun handlePacket(packet: Packet) {
        val data = packet.data

        when (packet.opcode) {
            Opcode.Frame -> {
                when {
                    data.has("evt") && data["evt"].asString == "ERROR" -> {
                        data["data"].asJsonObject.let {
                            println("Discord IPC error ${it["code"].asInt} with message: ${it["message"].asString}")
                        }
                    }

                    data.has("cmd") && data["cmd"].asString == "DISPATCH" -> {
                        ready = true
                        user = gson.fromJson(data["data"].asJsonObject["user"], IPCUser::class.java)
                        queuedActivity?.let { sendActivity() }
                    }
                }
            }

            Opcode.Close -> {
                println("Discord IPC error ${data["code"].asInt} with message: ${data["message"].asString}")
                stop()
            }

            else -> {}
        }
    }

    private fun open(): Connection? {
        val os = System.getProperty("os.name").lowercase(Locale.getDefault())

        if (os.contains("win")) {
            repeat(10) {
                runCatching {
                    return@open WinConnection("\\\\?\\pipe\\discord-ipc-$it")
                }
            }
        } else {
            val name = unixTempPaths.firstNotNullOfOrNull { System.getenv(it) }

            repeat(10) {
                runCatching {
                    return@open UnixConnection("${name?:"/tmp"}/discord-ipc-$it")
                }
            }
        }

        return null
    }

    private fun startThread() {
        thread(name = "Discord IPC - Update thread", start = true, isDaemon = true) {
            while (isConnected) {
                runCatching { run() }
                Thread.sleep(5000)
            }
        }
    }
}
