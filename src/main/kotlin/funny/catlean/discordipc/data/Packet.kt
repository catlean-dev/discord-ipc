package funny.catlean.discordipc.data

data class Packet(val opcode: Opcode, val data: PacketPayload)