package funny.catlean.discordipc.connection.impl

import funny.catlean.discordipc.RichPresence.handlePacket
import funny.catlean.discordipc.connection.Connection
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread

class UnixConnection(name: String) : Connection() {
    private val selector = Selector.open()
    private val socket = SocketChannel.open(UnixDomainSocketAddress.of(name))
    private val parser = PacketParser()

    init {
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)
        thread(name = "Discord IPC - Socket thread", start = true) { run() }
    }

    private fun run() {
        runCatching {
            while (selector.select() > 0) {
                selector.selectedKeys().removeIf {
                    if (it.isReadable) {
                        parser.read(socket)?.let {
                            handlePacket(it)
                            return@removeIf true
                        }
                        return@removeIf false
                    }
                    false
                }
            }
        }
    }

    override fun write(buffer: ByteBuffer) {
        runCatching { socket.write(buffer) }
    }

    override fun close() {
        runCatching {
            socket.close()
            selector.close()
        }
    }
}