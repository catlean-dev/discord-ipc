package funny.catlean.discordipc

import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.connection.impl.UnixConnection
import funny.catlean.discordipc.connection.impl.WinConnection
import funny.catlean.discordipc.data.*
import funny.catlean.discordipc.drafts.DraftService
import funny.catlean.discordipc.serialization.json
import kotlinx.serialization.json.encodeToJsonElement
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.Locale
import kotlin.concurrent.thread

open class DiscordIPC {
    private val unixTempPaths = arrayOf("XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP")
    private var connection: Connection? = null

    private var ready = false
    private var queuedActivity: Activity? = null

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
            it.write(
                Opcode.HANDSHAKE,
                json.encodeToJsonElement(HandshakePayload(v = 1, clientId = appId.toString()))
            )

            startThread()
        }
    }

    protected fun setActivity(presence: Activity) {
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
        connection?.let {
            queuedActivity?.let { activity ->
                it.write(
                    Opcode.FRAME, json.encodeToJsonElement(
                        FramePayload(
                            cmd = Command.SET_ACTIVITY,
                            args = FrameArgs(
                                pid = pID,
                                activity = activity
                            )
                        )
                    )
                )
            }
        }

        queuedActivity = null
    }

    fun handlePacket(packet: Packet) = when (packet.opcode) {
        Opcode.FRAME -> when {
            packet.data.evt == PacketEvt.ERROR -> {
                println("Discord IPC error ${packet.data.data.code} with message: ${packet.data.data.message}")
            }

            packet.data.cmd == Command.DISPATCH -> {
                ready = true
                user = packet.data.data.user!!
                queuedActivity?.let { sendActivity() }
            }

            else -> {}
        }

        Opcode.CLOSE -> {
            println("Discord IPC error ${packet.data.data.code} with message: ${packet.data.data.message}")
            stop()
        }

        else -> {}
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
                    return@open UnixConnection("${name ?: "/tmp"}/discord-ipc-$it")
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
