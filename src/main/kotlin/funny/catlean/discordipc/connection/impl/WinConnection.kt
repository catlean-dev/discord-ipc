package funny.catlean.discordipc.connection.impl

import funny.catlean.discordipc.RichPresence.handlePacket
import funny.catlean.discordipc.connection.Connection
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class WinConnection(name: String) : Connection() {
    private val raf = RandomAccessFile(name, "rw")
    private val channel = raf.channel
    private val parser = PacketParser()

    init {
        thread(name = "Discord IPC - Pipe thread", start = true) { run() }
    }

    private fun run() {
        runCatching {
            while (true)
                parser.read(channel)?.let { handlePacket(it) }?: continue
        }
    }

    override fun write(buffer: ByteBuffer) {
        runCatching { channel.write(buffer) }
    }

    override fun close() {
        runCatching { channel.close() }
        runCatching { raf.close() }
    }
}