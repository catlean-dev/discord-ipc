package meteordevelopment.discordipc.connection

import com.google.gson.JsonObject
import meteordevelopment.discordipc.Opcode
import java.nio.ByteBuffer
import java.util.*

abstract class Connection {
    fun write(opcode: Opcode, o: JsonObject) {
        o.addProperty("nonce", UUID.randomUUID().toString())

        val d = o.toString().toByteArray()
        val packet = ByteBuffer.allocate(d.size + 8)
        packet.putInt(Integer.reverseBytes(opcode.ordinal))
        packet.putInt(Integer.reverseBytes(d.size))
        packet.put(d)

        packet.rewind()
        write(packet)
    }

    protected abstract fun write(buffer: ByteBuffer)

    abstract fun close()
}
