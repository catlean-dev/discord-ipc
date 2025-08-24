package funny.catlean.discordipc.connection

import funny.catlean.discordipc.data.Opcode
import funny.catlean.discordipc.serialization.json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.nio.ByteBuffer
import java.util.*

fun Int.rewind() = Integer.reverseBytes(this)

abstract class Connection {
    fun write(opcode: Opcode, element: JsonElement) {
        val mutableJson = element.jsonObject.toMutableMap()
        mutableJson["nonce"] = JsonPrimitive(UUID.randomUUID().toString())

        json.encodeToString(JsonObject(mutableJson)).toByteArray(Charsets.UTF_8).also {
            write(ByteBuffer.allocate(it.size + 8).apply {
                putInt(opcode.ordinal.rewind())
                putInt(it.size.rewind())
                put(it)
                flip()
            })
        }
    }

    protected abstract fun write(buffer: ByteBuffer)

    abstract fun close()
}