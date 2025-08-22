package funny.catlean.discordipc.connection

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import funny.catlean.discordipc.Opcode
import funny.catlean.discordipc.Packet
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.util.*

private fun Int.rewind() = Integer.reverseBytes(this)

abstract class Connection {
    fun write(opcode: Opcode, o: JsonObject) {
        o.addProperty("nonce", UUID.randomUUID().toString())

        val d = o.toString().toByteArray(Charsets.UTF_8)

        write(ByteBuffer.allocate(d.size + 8).apply {
            putInt(opcode.ordinal.rewind())
            putInt(d.size.rewind())
            put(d)
            flip()
        })
    }

    protected abstract fun write(buffer: ByteBuffer)

    abstract fun close()

    protected class PacketParser {
        private var opcode: Opcode? = null
        private var length: Int? = null
        private var dataBuf: ByteBuffer? = null
        private val intBuf = ByteBuffer.allocate(4)

        fun read(channel: ReadableByteChannel): Packet? {
            if (opcode == null) {
                if (!readInt(channel)) return null
                opcode = Opcode.entries.getOrNull(intBuf.getInt(0).rewind())
                intBuf.clear()
            }

            if (length == null) {
                if (!readInt(channel)) return null
                length = intBuf.getInt(0).rewind()
                dataBuf = ByteBuffer.allocate(length!!)
                intBuf.clear()
            }

            val buf = dataBuf!!
            channel.read(buf)
            if (buf.hasRemaining()) return null

            buf.flip()
            val json = JsonParser.parseString(Charsets.UTF_8.decode(buf).toString()).asJsonObject
            val packet = Packet(opcode!!, json)

            opcode = null
            length = null
            dataBuf = null
            return packet
        }

        private fun readInt(channel: ReadableByteChannel): Boolean {
            channel.read(intBuf)
            if (intBuf.hasRemaining()) return false
            intBuf.flip()
            return true
        }
    }
}