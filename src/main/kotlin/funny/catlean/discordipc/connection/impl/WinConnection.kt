package funny.catlean.discordipc.connection.impl

import com.google.gson.JsonParser
import funny.catlean.discordipc.RichPresence
import funny.catlean.discordipc.RichPresence.handlePacket
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.data.Opcode
import funny.catlean.discordipc.data.Packet
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import java.lang.Thread.sleep
import java.nio.charset.Charset

class WinConnection(name: String) : Connection() {
    private val raf = RandomAccessFile(name, "rw")

    init {
        thread(name = "Discord IPC - Pipe thread", start = true) { run() }
    }

    override fun write(buffer: ByteBuffer) {
        runCatching {
            raf.write(buffer.array())
        }.onFailure {
            RichPresence.stop()
        }
    }

    private fun run() {
        val intB = ByteBuffer.allocate(4)

        runCatching {
            while (true) {
                readFully(intB)
                val opcode = Opcode.entries[(Integer.reverseBytes(intB.getInt(0)))]

                readFully(intB)
                val length = Integer.reverseBytes(intB.getInt(0))

                val dataB = ByteBuffer.allocate(length)
                readFully(dataB)
                val data = Charset.defaultCharset().decode(dataB.rewind()).toString()

                handlePacket(Packet(opcode, JsonParser.parseString(data).asJsonObject))
            }
        }
    }

    private fun readFully(buffer: ByteBuffer) {
        buffer.rewind()

        while (raf.length() < buffer.remaining()) {
            Thread.onSpinWait()
            sleep(100)
        }

        while (buffer.hasRemaining()) raf.getChannel().read(buffer)
    }

    override fun close() {
        runCatching {
            raf.close()
        }
    }
}