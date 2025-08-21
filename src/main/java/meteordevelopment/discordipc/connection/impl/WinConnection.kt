package meteordevelopment.discordipc.connection.impl

import com.google.gson.JsonParser
import meteordevelopment.discordipc.Packet
import meteordevelopment.discordipc.connection.Connection
import meteordevelopment.discordipc.opcode
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.function.Consumer

class WinConnection internal constructor(name: String, private val callback: Consumer<Packet>) : Connection() {
    private val raf = RandomAccessFile(name, "rw")

    init {
        Thread { run() }
            .also { it.name = "Discord IPC - Read thread" }
            .start()
    }

    override fun write(buffer: ByteBuffer) {
        try {
            raf.write(buffer.array())
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun run() {
        val intB = ByteBuffer.allocate(4)

        try {
            while (true) {
                // Opcode
                readFully(intB)
                val opcode = opcode(Integer.reverseBytes(intB.getInt(0)))

                // Length
                readFully(intB)
                val length = Integer.reverseBytes(intB.getInt(0))

                // Data
                val dataB = ByteBuffer.allocate(length)
                readFully(dataB)
                val data = Charset.defaultCharset().decode(dataB.rewind()).toString()

                // Call callback
                callback.accept(Packet(opcode, JsonParser.parseString(data).getAsJsonObject()))
            }
        } catch (ignored: Exception) {
        }
    }

    @Throws(IOException::class)
    private fun readFully(buffer: ByteBuffer) {
        buffer.rewind()

        while (raf.length() < buffer.remaining()) {
            Thread.onSpinWait()
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        while (buffer.hasRemaining()) raf.getChannel().read(buffer)
    }

    override fun close() {
        try {
            raf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}