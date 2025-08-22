package funny.catlean.discordipc.connection.impl

import com.google.gson.JsonParser
import funny.catlean.discordipc.RichPresence.handlePacket
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.connection.rewind
import funny.catlean.discordipc.data.Opcode
import funny.catlean.discordipc.data.Packet
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import kotlin.concurrent.thread

class UnixConnection(name: String) : Connection() {
    private val selector = Selector.open()
    private val socket = SocketChannel.open(UnixDomainSocketAddress.of(name))
    private var opcode: Opcode? = null
    private var length: Int? = null
    private var dataBuf: ByteBuffer? = null
    private val intBuf = ByteBuffer.allocate(4)

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
                        socket.read()?.let { p ->
                            handlePacket(p)
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

    fun ReadableByteChannel.read(): Packet? {
        if (opcode == null) {
            if (!readInt()) return null
            opcode = Opcode.entries.getOrNull(intBuf.getInt(0).rewind())
            intBuf.clear()
        }

        if (length == null) {
            if (!readInt()) return null
            length = intBuf.getInt(0).rewind()
            dataBuf = ByteBuffer.allocate(length!!)
            intBuf.clear()
        }

        read(dataBuf!!)
        if (dataBuf!!.hasRemaining()) return null

        dataBuf!!.flip()
        val json = JsonParser.parseString(Charsets.UTF_8.decode(dataBuf!!).toString()).asJsonObject
        val packet = Packet(opcode!!, json)

        opcode = null
        length = null
        dataBuf = null
        return packet
    }

    fun ReadableByteChannel.readInt(): Boolean {
        read(intBuf)
        if (intBuf.hasRemaining()) return false
        intBuf.flip()
        return true
    }
}