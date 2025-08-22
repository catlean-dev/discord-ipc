package funny.catlean.discordipc.data

import com.google.gson.JsonObject
import funny.catlean.discordipc.data.Opcode

data class Packet(val opcode: Opcode, val data: JsonObject)