package funny.catlean.discordipc.connection.impl

import com.google.gson.JsonParser
import funny.catlean.discordipc.Opcode
import funny.catlean.discordipc.Packet
import funny.catlean.discordipc.connection.Connection
import funny.catlean.discordipc.opcode
import java.io.IOException
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.SocketChannel
import java.nio.charset.Charset
import java.util.function.Consumer

class UnixConnection(name: String, private val callback: Consumer<Packet>) : Connection() {
    private val s = Selector.open()
    private val sc = SocketChannel.open(UnixDomainSocketAddress.of(name))

    init {
        sc.configureBlocking(false)
        sc.register(s, SelectionKey.OP_READ)

        Thread { run() }
            .also { it.name = "Discord IPC - Read thread" }
            .start()
    }

    private fun run() {
        var state = State.Opcode

        val intB = ByteBuffer.allocate(4)
        var dataB: ByteBuffer? = null

        var opcode: Opcode? = null

        try {
            while (true) {
                s.select()

                when (state) {
                    State.Opcode -> {
                        sc.read(intB)
                        if (intB.hasRemaining()) break

                        opcode = opcode(Integer.reverseBytes(intB.getInt(0)))
                        state = State.Length

                        intB.rewind()
                    }

                    State.Length -> {
                        sc.read(intB)
                        if (intB.hasRemaining()) break

                        dataB = ByteBuffer.allocate(Integer.reverseBytes(intB.getInt(0)))
                        state = State.Data

                        intB.rewind()
                    }

                    State.Data -> {
                        sc.read(dataB)
                        if (dataB!!.hasRemaining()) break

                        val data = Charset.defaultCharset().decode(dataB.rewind()).toString()
                        callback.accept(Packet(opcode!!, JsonParser.parseString(data).getAsJsonObject()))

                        dataB = null
                        state = State.Opcode
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    override fun write(buffer: ByteBuffer) {
        try {
            sc.write(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun close() {
        try {
            s.close()
            sc.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private enum class State {
        Opcode,
        Length,
        Data
    }
}