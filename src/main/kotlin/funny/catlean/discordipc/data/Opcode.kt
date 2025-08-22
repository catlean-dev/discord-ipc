package funny.catlean.discordipc.data

enum class Opcode {
    Handshake,
    Frame,
    Close,
    Ping,
    Pong;
}