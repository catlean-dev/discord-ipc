package funny.catlean.discordipc.connection.impl

import com.google.gson.JsonParser
import funny.catlean.discordipc.Opcode
import funny.catlean.discordipc.Packet
import funny.catlean.discordipc.RichPresence.onPacket
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.opcode
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import kotlin.concurrent.thread

class UnixConnection(name: String) : Connection() {
    private val selector = Selector.open()
    private val socket = SocketChannel.open(UnixDomainSocketAddress.of(name))

    init {
        socket.configureBlocking(false)
        socket.register(selector, SelectionKey.OP_READ)

        thread(name = "Discord IPC - Read thread", start = true) { run() }
    }

    private fun run() {
        var state = State.Opcode

        val intB = ByteBuffer.allocate(4)
        var dataB: ByteBuffer? = null
        var opcode: Opcode? = null

        runCatching {
            while (true) {
                selector.select()

                when (state) {
                    State.Opcode -> {
                        socket.read(intB)
                        if (intB.hasRemaining()) break

                        opcode = opcode(Integer.reverseBytes(intB.getInt(0)))
                        state = State.Length

                        intB.rewind()
                    }

                    State.Length -> {
                        socket.read(intB)
                        if (intB.hasRemaining()) break

                        dataB = ByteBuffer.allocate(Integer.reverseBytes(intB.getInt(0)))
                        state = State.Data

                        intB.rewind()
                    }

                    State.Data -> {
                        socket.read(dataB)
                        if (dataB!!.hasRemaining()) break

                        val data = Charset.defaultCharset().decode(dataB.rewind()).toString()
                        onPacket(Packet(opcode!!, JsonParser.parseString(data).asJsonObject))

                        dataB = null
                        state = State.Opcode
                    }
                }
            }
        }
    }

    override fun write(buffer: ByteBuffer) {
        runCatching {
            socket.write(buffer)
        }
    }

    override fun close() {
        runCatching {
            selector.close()
            socket.close()
        }
    }

    private enum class State {
        Opcode,
        Length,
        Data
    }
}