package funny.catlean.discordipc.data

enum class Opcode {
    HANDSHAKE,
    FRAME,
    CLOSE,
    PING,
    PONG
}