package funny.catlean.discordipc.connection

import com.google.gson.JsonObject
import funny.catlean.discordipc.data.Opcode
import java.nio.ByteBuffer
import java.util.*

fun Int.rewind() = Integer.reverseBytes(this)

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
}