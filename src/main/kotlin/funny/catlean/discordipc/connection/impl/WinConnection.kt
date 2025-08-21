package funny.catlean.discordipc.connection.impl

import com.google.gson.JsonParser
import funny.catlean.discordipc.Packet
import funny.catlean.discordipc.RichPresence.onPacket
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.opcode
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.function.Consumer
import kotlin.concurrent.thread

class WinConnection(name: String) : Connection() {
    private val raf = RandomAccessFile(name, "rw")

    init {
        thread(name = "Discord IPC - Read thread", start = true) { run() }
    }

    override fun write(buffer: ByteBuffer) {
        runCatching {
            raf.write(buffer.array())
        }
    }

    private fun run() {
        val intB = ByteBuffer.allocate(4)

        runCatching {
            while (true) {
                readFully(intB)
                val opcode = opcode(Integer.reverseBytes(intB.getInt(0)))

                readFully(intB)
                val length = Integer.reverseBytes(intB.getInt(0))

                val dataB = ByteBuffer.allocate(length)
                readFully(dataB)
                val data = Charset.defaultCharset().decode(dataB.rewind()).toString()

                onPacket(Packet(opcode, JsonParser.parseString(data).asJsonObject))
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