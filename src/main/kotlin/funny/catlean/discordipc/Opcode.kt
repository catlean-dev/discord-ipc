package funny.catlean.discordipc

fun opcode(opcode: Int) = Opcode.entries.toTypedArray()[opcode]

enum class Opcode {
    Handshake,
    Frame,
    Close,
    Ping,
    Pong;
}
