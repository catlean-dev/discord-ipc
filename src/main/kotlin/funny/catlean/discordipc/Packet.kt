package funny.catlean.discordipc

import com.google.gson.JsonObject

data class Packet(val opcode: Opcode, val data: JsonObject)
